package io.github.esafak.kotlintsgen

import io.github.esafak.kotlintsgen.core.SerializerDescriptorsExtractor
import io.github.esafak.kotlintsgen.core.TsDeclaration
import io.github.esafak.kotlintsgen.core.TsElement
import io.github.esafak.kotlintsgen.core.TsElementConverter
import io.github.esafak.kotlintsgen.core.TsElementId
import io.github.esafak.kotlintsgen.core.TsElementIdConverter
import io.github.esafak.kotlintsgen.core.TsLiteral
import io.github.esafak.kotlintsgen.core.TsMapTypeConverter
import io.github.esafak.kotlintsgen.core.TsSourceCodeGenerator
import io.github.esafak.kotlintsgen.core.TsTypeRef
import io.github.esafak.kotlintsgen.core.TsTypeRefConverter
import io.github.esafak.kotlintsgen.core.TsIdentifierValidator
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.descriptors.getContextualDescriptor
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.overwriteWith


private val builtInPrimitiveSerialNames = setOf(
  "kotlin.Boolean",
  "kotlin.Byte",
  "kotlin.Char",
  "kotlin.Double",
  "kotlin.Float",
  "kotlin.Int",
  "kotlin.Long",
  "kotlin.Short",
  "kotlin.String",
)


/**
 * Generate TypeScript from [`@Serializable`][Serializable] Kotlin.
 *
 * The output can be controlled by the settings in [config],
 * or by setting hardcoded values in [serializerDescriptorOverrides] or [descriptorOverrides],
 * or changed by overriding any converter.
 *
 * @param[config] General settings that affect how KotlinTsGen works
 * @param[sourceCodeGenerator] Convert [TsElement]s to TypeScript source code
 */
@OptIn(ExperimentalSerializationApi::class)
open class KotlinTsGenerator(
  open val config: KotlinTsConfig = KotlinTsConfig(),
  open val sourceCodeGenerator: TsSourceCodeGenerator = TsSourceCodeGenerator.Default(config),
  open val serializersModule: SerializersModule = EmptySerializersModule(),
) {

  constructor(
    config: KotlinTsConfig,
    sourceCodeGenerator: TsSourceCodeGenerator,
  ) : this(config, sourceCodeGenerator, EmptySerializersModule())

  private val effectiveSerializersModule = config.serializersModule overwriteWith serializersModule


  val serializerDescriptorOverrides: MutableMap<KSerializer<*>, Set<SerialDescriptor>> =
    mutableMapOf()

  // Explicit descriptor overrides intentionally bypass generated-identifier validation because
  // their TsElement ids are supplied directly by the caller.
  val descriptorOverrides: MutableMap<SerialDescriptor, TsElement> = mutableMapOf()

  private fun findOverride(descriptor: SerialDescriptor): TsElement? {
    return descriptorOverrides.entries.run {
      firstOrNull { it.key == descriptor } ?: firstOrNull { it.key.nullable == descriptor.nullable }
    }?.value
  }

  open fun findMapTypeOverride(descriptor: SerialDescriptor): TsLiteral.TsMap.Type? {
    return when (findOverride(descriptor)) {
      null                         -> null

      is TsDeclaration.TsEnum      -> TsLiteral.TsMap.Type.MAPPED_OBJECT

      is TsLiteral.Custom,
      TsLiteral.Primitive.TsNumber,
      TsLiteral.Primitive.TsString -> TsLiteral.TsMap.Type.INDEX_SIGNATURE

      else                         -> TsLiteral.TsMap.Type.MAP
    }
  }


  open val descriptorsExtractor = object : SerializerDescriptorsExtractor {
    val extractor: SerializerDescriptorsExtractor =
      SerializerDescriptorsExtractor.default(effectiveSerializersModule)
    val cache: MutableMap<KSerializer<*>, Set<SerialDescriptor>> = mutableMapOf()

    override fun invoke(serializer: KSerializer<*>): Set<SerialDescriptor> =
      cache.getOrPut(serializer) {
        serializerDescriptorOverrides[serializer] ?: extractor(serializer)
      }
  }


  val elementIdConverter: TsElementIdConverter = object : TsElementIdConverter {
    private val converter: TsElementIdConverter = TsElementIdConverter.Default
    private val cache: MutableMap<SerialDescriptor, TsElementId> = mutableMapOf()

    override fun invoke(descriptor: SerialDescriptor): TsElementId =
      cache.getOrPut(descriptor) {
        when (val override = findOverride(descriptor)) {
          is TsDeclaration -> override.id
          else             -> converter(descriptor)
        }
      }
  }


  val mapTypeConverter: TsMapTypeConverter = object : TsMapTypeConverter {
    private val converter = TsMapTypeConverter.Default
    private val cache: MutableMap<Pair<SerialDescriptor, SerialDescriptor>, TsLiteral.TsMap.Type> =
      mutableMapOf()

    override fun invoke(
      keyDescriptor: SerialDescriptor,
      valDescriptor: SerialDescriptor,
    ): TsLiteral.TsMap.Type =
      cache.getOrPut(keyDescriptor to valDescriptor) {
        findMapTypeOverride(keyDescriptor) ?: converter(keyDescriptor, valDescriptor)
      }
  }


  val typeRefConverter: TsTypeRefConverter = object : TsTypeRefConverter {
    private val converter = TsTypeRefConverter.Default(
      elementIdConverter,
      mapTypeConverter,
      effectiveSerializersModule,
    )
    val cache: MutableMap<SerialDescriptor, TsTypeRef> = mutableMapOf()

    override fun invoke(descriptor: SerialDescriptor): TsTypeRef =
      cache.getOrPut(descriptor) {
        when (val override = findOverride(descriptor)) {
          null             -> converter(descriptor)
          is TsLiteral     -> TsTypeRef.Literal(override, descriptor.isNullable)
          is TsDeclaration -> TsTypeRef.Declaration(override.id, null, descriptor.isNullable)
        }
      }
  }


  val elementConverter: TsElementConverter = object : TsElementConverter {
    private val converter = TsElementConverter.Default(
      elementIdConverter,
      mapTypeConverter,
      typeRefConverter,
      effectiveSerializersModule,
    )
    val cache: MutableMap<SerialDescriptor, Set<TsElement>> = mutableMapOf()

    override fun invoke(descriptor: SerialDescriptor): Set<TsElement> =
      cache.getOrPut(descriptor) {
        when (val override = findOverride(descriptor)) {
          null -> converter(descriptor)
          else -> setOf(override)
        }
      }
  }


  private fun rootPrimitiveAlias(
    descriptor: SerialDescriptor,
  ): TsDeclaration.TsTypeAlias? {
    if (descriptor.kind !is PrimitiveKind) return null
    if (descriptor.serialName in builtInPrimitiveSerialNames) return null
    if (findOverride(descriptor) != null) return null

    return TsDeclaration.TsTypeAlias(
      id = elementIdConverter(descriptor),
      typeRef = typeRefConverter(descriptor),
    )
  }


  private fun rootDescriptors(serializer: KSerializer<*>): Set<SerialDescriptor> {
    serializerDescriptorOverrides[serializer]?.let { return it }

    val descriptor = serializer.descriptor
    if (descriptor.kind != SerialKind.CONTEXTUAL) return setOf(descriptor)

    return runCatching {
      effectiveSerializersModule.getContextualDescriptor(descriptor)
    }.getOrNull()?.let(::setOf) ?: setOf(descriptor)
  }


  open fun generate(vararg serializers: KSerializer<*>): String {
    val rootSerializers = serializers.toSet()
    val rootPrimitiveAliases = rootSerializers
      .flatMap(::rootDescriptors)
      .mapNotNull(::rootPrimitiveAlias)
      .distinctBy { it.id }

    return rootSerializers

      // 1. get all SerialDescriptors from a KSerializer
      .flatMap { serializer -> descriptorsExtractor(serializer) }
      .toSet()

      // 2. convert each SerialDescriptor to some TsElements
      .flatMap { descriptor -> elementConverter(descriptor) }
      .toSet()
      .plus(rootPrimitiveAliases)

      // 3. group by namespaces
      .groupBy { element -> sourceCodeGenerator.groupElementsBy(element) }

      // 4. convert to source code and render configured namespace groups
      .let(::renderGroups)
  }


  private class NamespaceGroupNode {
    val declarations: MutableList<String> = mutableListOf()
    val children: LinkedHashMap<String, NamespaceGroupNode> = linkedMapOf()
  }


  private fun renderGroups(groups: Map<String?, List<TsElement>>): String {
    val flatGroups = mutableListOf<String>()
    val root = NamespaceGroupNode()

    groups.forEach { (group, elements) ->
      val declarations = elements
        .filterIsInstance<TsDeclaration>()
        .map { element -> sourceCodeGenerator.generateDeclarationInNamespace(element, group) }
        .filter(String::isNotBlank)

      if (group.isNullOrBlank()) {
        flatGroups += declarations.joinToString(config.declarationSeparator)
      } else {
        val segments = group.split('.')
        segments.forEach { segment ->
          TsIdentifierValidator.assertValidIdentifier(segment, "namespace segment")
        }

        var node = root
        segments.forEach { segment ->
          node = node.children.getOrPut(segment) { NamespaceGroupNode() }
        }
        node.declarations += declarations
      }
    }

    fun renderNode(node: NamespaceGroupNode): String {
      val directDeclarations = node.declarations.joinToString(config.declarationSeparator)
      val nestedNamespaces = node.children.entries.map { (name, child) ->
        sourceCodeGenerator.wrapInNamespace(name, renderNode(child))
      }
      return (listOf(directDeclarations) + nestedNamespaces)
        .filter(String::isNotBlank)
        .joinToString(config.declarationSeparator)
    }

    return (flatGroups + renderNode(root))
      .filter(String::isNotBlank)
      .joinToString(config.declarationSeparator)
  }

}
