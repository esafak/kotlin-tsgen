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
import io.github.esafak.kotlintsgen.core.InvalidTsIdentifierException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
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

  private var generatedPrimitiveAliases: Map<String, TsElementId> = emptyMap()


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
      primitiveTypeRefOverride = { descriptor ->
        generatedPrimitiveAliases[descriptor.serialName.removeSuffix("?")]
          ?.let { TsTypeRef.Declaration(it, null, descriptor.isNullable) }
      },
      indexSignatureKeyTypeRefOverride = { descriptor ->
        (findOverride(descriptor) as? TsLiteral)?.let { TsTypeRef.Literal(it, false) }
      },
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


  private fun primitiveAlias(
    descriptor: SerialDescriptor,
  ): TsDeclaration.TsTypeAlias? {
    val primitiveKind = descriptor.kind as? PrimitiveKind ?: return null
    if (descriptor.serialName in builtInPrimitiveSerialNames && primitiveKind !in signedPrimitiveKinds) {
      return null
    }
    if (findOverride(descriptor) != null) return null

    val id = generatedPrimitiveAliases[descriptor.serialName.removeSuffix("?")] ?: return null

    return TsDeclaration.TsTypeAlias(
      id = id,
      typeRef = converterPrimitiveLiteral(descriptor),
    )
  }

  private val signedPrimitiveKinds = setOf(
    PrimitiveKind.BYTE,
    PrimitiveKind.SHORT,
    PrimitiveKind.INT,
    PrimitiveKind.LONG,
    PrimitiveKind.FLOAT,
    PrimitiveKind.DOUBLE,
  )

  private fun converterPrimitiveLiteral(descriptor: SerialDescriptor): TsTypeRef {
    val literal = when (descriptor.kind) {
      PrimitiveKind.BOOLEAN -> TsLiteral.Primitive.TsBoolean
      PrimitiveKind.CHAR,
      PrimitiveKind.STRING -> TsLiteral.Primitive.TsString
      PrimitiveKind.BYTE,
      PrimitiveKind.SHORT,
      PrimitiveKind.INT,
      PrimitiveKind.LONG,
      PrimitiveKind.FLOAT,
      PrimitiveKind.DOUBLE -> TsLiteral.Primitive.TsNumber
      else -> error("Expected primitive descriptor, got ${descriptor.kind}")
    }
    return TsTypeRef.Literal(literal, false)
  }


  open fun generate(vararg serializers: KSerializer<*>): String {
    val rootSerializers = serializers.toSet()
    val descriptors = rootSerializers
      .flatMap { serializer -> descriptorsExtractor(serializer) }
      .toSet()

    val primitiveAliasDescriptors = descriptors
      .filter { descriptor ->
        val kind = descriptor.kind as? PrimitiveKind ?: return@filter false
        kind !in setOf(PrimitiveKind.BOOLEAN, PrimitiveKind.CHAR, PrimitiveKind.STRING) ||
          descriptor.serialName !in builtInPrimitiveSerialNames
      }
      .filter { findOverride(it) == null }
      .distinctBy { it.serialName.removeSuffix("?") to it.kind }
    val aliasEntries = primitiveAliasDescriptors.map { descriptor ->
        val serialName = descriptor.serialName.removeSuffix("?")
        val id = if (serialName in builtInPrimitiveSerialNames) {
          TsElementId(serialName.substringAfterLast('.'))
        } else {
          elementIdConverter(descriptor)
        }
      serialName to id
    }
    // Compare rendered locations rather than descriptor IDs: namespace configuration determines
    // whether two declarations with the same short name actually collide in TypeScript.
    aliasEntries.groupingBy { (_, id) ->
      sourceCodeGenerator.groupElementsBy(TsDeclaration.TsTypeAlias(id, TsTypeRef.Literal(TsLiteral.Primitive.TsString, false))) to id.name
    }.eachCount().filterValues { it > 1 }.keys.firstOrNull()?.let { (_, name) ->
      throw InvalidTsIdentifierException(name, "generated numeric type aliases", "multiple reachable primitive aliases render with the same name")
    }
    generatedPrimitiveAliases = aliasEntries.distinctBy { it.first }.toMap()

    val elements = descriptors

      // 1. get all SerialDescriptors from a KSerializer
      // 2. convert each SerialDescriptor to some TsElements
      .flatMap { descriptor -> elementConverter(descriptor) }
      .toSet()

    val aliases = descriptors.mapNotNull(::primitiveAlias)
    val aliasKeys = aliases.groupingBy { alias ->
      sourceCodeGenerator.groupElementsBy(alias) to alias.id.name
    }.eachCount()
    aliasKeys.filterValues { it > 1 }.keys.firstOrNull()?.let { (_, name) ->
      throw InvalidTsIdentifierException(name, "generated numeric type aliases", "multiple reachable primitive aliases render with the same name")
    }
    val declarations = elements.filterIsInstance<TsDeclaration>()
    val declarationKeys = declarations.associateBy { declaration ->
      sourceCodeGenerator.groupElementsBy(declaration) to declaration.id.name
    }
    aliases
      .filter { alias -> (sourceCodeGenerator.groupElementsBy(alias) to alias.id.name) in declarationKeys }
      .firstOrNull()
      ?.let { alias ->
        val conflict = declarationKeys[sourceCodeGenerator.groupElementsBy(alias) to alias.id.name]!!
        throw InvalidTsIdentifierException(
          conflict.id.name,
          "generated numeric type alias",
          "it conflicts with reachable declaration '${conflict.id}'",
        )
      }

    return elements
      .plus(aliases)

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
