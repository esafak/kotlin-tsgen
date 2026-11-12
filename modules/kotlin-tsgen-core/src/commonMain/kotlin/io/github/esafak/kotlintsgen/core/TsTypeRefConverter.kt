package io.github.esafak.kotlintsgen.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.getContextualDescriptor
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

fun interface TsTypeRefConverter {

  operator fun invoke(descriptor: SerialDescriptor): TsTypeRef

  @OptIn(ExperimentalSerializationApi::class)
  open class Default(
    val elementIdConverter: TsElementIdConverter = TsElementIdConverter.Default,
    val mapTypeConverter: TsMapTypeConverter = TsMapTypeConverter.Default,
    val serializersModule: SerializersModule = EmptySerializersModule(),
    val primitiveTypeRefOverride: ((SerialDescriptor) -> TsTypeRef?)? = null,
    val indexSignatureKeyTypeRefOverride: ((SerialDescriptor) -> TsTypeRef.Literal?)? = null,
  ) : TsTypeRefConverter {

    /**
     * Resolver for nested list elements, map values (and non-index-signature map keys) and
     * contextual targets. Defaults to null (meaning `this`). Wrappers that intercept [invoke]
     * for mapping overrides (e.g. branded aliases) supply their override-aware converter here
     * so nested positions see the same overrides as top-level descriptors. Index-signature
     * keys stay unwrapped to `string`/`number` and never use this resolver.
     *
     * Kept as a mutable property (rather than a constructor parameter) so existing
     * five-parameter constructor calls — including trailing-lambda calls binding to
     * [indexSignatureKeyTypeRefOverride] — keep compiling and linking.
     */
    var nestedTypeRefConverterProvider: (() -> TsTypeRefConverter)? = null

    /** Override the type reference used for non-index-signature map keys. */
    open fun mapKeyTypeRefOverride(descriptor: SerialDescriptor): TsTypeRef? = null

    private fun nestedConverter(): TsTypeRefConverter =
      nestedTypeRefConverterProvider?.invoke() ?: this

    override operator fun invoke(descriptor: SerialDescriptor): TsTypeRef {
      primitiveTypeRefOverride?.invoke(descriptor)?.let { return it }
      return when (val descriptorKind = descriptor.kind) {
        is PrimitiveKind -> primitiveTypeRef(descriptor, descriptorKind)

        StructureKind.LIST -> when {
          descriptor.elementDescriptors.count() > 1 -> declarationTypeRef(descriptor)
          else -> listTypeRef(descriptor)
        }

        StructureKind.MAP -> mapTypeRef(descriptor)

        SerialKind.CONTEXTUAL -> resolveContextual(descriptor)

        PolymorphicKind.SEALED,
        PolymorphicKind.OPEN,
        SerialKind.ENUM,
        StructureKind.CLASS,
        StructureKind.OBJECT,
        -> declarationTypeRef(descriptor)
      }
    }

    private fun resolveContextual(descriptor: SerialDescriptor): TsTypeRef {
      val resolved = runCatching {
        serializersModule.getContextualDescriptor(descriptor)
      }.getOrNull() ?: return declarationTypeRef(descriptor)

      return withNullable(nestedConverter()(resolved), descriptor.isNullable)
    }

    private fun withNullable(typeRef: TsTypeRef, nullable: Boolean): TsTypeRef = when (typeRef) {
      is TsTypeRef.Literal -> typeRef.copy(nullable = nullable)
      is TsTypeRef.Declaration -> typeRef.copy(nullable = nullable)
    }

    fun primitiveTypeRef(descriptor: SerialDescriptor, kind: PrimitiveKind): TsTypeRef.Literal {
      val tsPrimitive = when (kind) {
        PrimitiveKind.BOOLEAN -> TsLiteral.Primitive.TsBoolean

        PrimitiveKind.BYTE,
        PrimitiveKind.SHORT,
        PrimitiveKind.INT,
        PrimitiveKind.LONG,
        PrimitiveKind.FLOAT,
        PrimitiveKind.DOUBLE,
        -> TsLiteral.Primitive.TsNumber

        PrimitiveKind.CHAR,
        PrimitiveKind.STRING,
        -> TsLiteral.Primitive.TsString
      }
      return TsTypeRef.Literal(tsPrimitive, descriptor.isNullable)
    }

    fun mapTypeRef(descriptor: SerialDescriptor): TsTypeRef.Literal {
      val (keyDescriptor, valueDescriptor) = descriptor.elementDescriptors.toList()
      val type = mapTypeConverter(keyDescriptor, valueDescriptor)
      val keyTypeRef = if (type == TsLiteral.TsMap.Type.INDEX_SIGNATURE) {
        indexSignatureKeyTypeRef(keyDescriptor)
      } else {
        mapKeyTypeRefOverride(keyDescriptor) ?: nestedConverter()(keyDescriptor)
      }
      val valueTypeRef = nestedConverter()(valueDescriptor)
      val map = TsLiteral.TsMap(keyTypeRef, valueTypeRef, type)
      return TsTypeRef.Literal(map, descriptor.isNullable)
    }

    /**
     * Index-signature parameters must be literal `string`/`number`-like types in TypeScript.
     * Type aliases are valid for mapped-object and `Map` keys, but TypeScript rejects aliases in
     * `[key: T]` positions. Inline value classes are therefore unwrapped before rendering.
     */
    private fun indexSignatureKeyTypeRef(descriptor: SerialDescriptor): TsTypeRef.Literal {
      indexSignatureKeyTypeRefOverride?.invoke(descriptor)?.let { return it.copy(nullable = false) }
      val resolved = when {
        descriptor.isInline && descriptor.elementsCount > 0 ->
          indexSignatureKeyTypeRef(descriptor.elementDescriptors.first())

        descriptor.kind == SerialKind.CONTEXTUAL -> {
          val contextual = runCatching {
            serializersModule.getContextualDescriptor(descriptor)
          }.getOrNull()
          if (contextual != null) {
            indexSignatureKeyTypeRef(contextual)
          } else {
            error("Expected primitive descriptor, got ${descriptor.kind}")
          }
        }

        else -> primitiveTypeRef(descriptor, descriptor.kind as PrimitiveKind)
      }
      return resolved.copy(nullable = false)
    }

    fun listTypeRef(descriptor: SerialDescriptor): TsTypeRef.Literal {
      val elementDescriptor = descriptor.elementDescriptors.first()
      val elementTypeRef = nestedConverter()(elementDescriptor)
      val listRef = TsLiteral.TsList(elementTypeRef)
      return TsTypeRef.Literal(listRef, descriptor.isNullable)
    }

    fun declarationTypeRef(descriptor: SerialDescriptor): TsTypeRef.Declaration {
      val id = elementIdConverter(descriptor)
      return TsTypeRef.Declaration(id, null, descriptor.isNullable)
    }
  }
}
