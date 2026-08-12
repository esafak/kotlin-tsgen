package io.github.esafak.kotlintsgen.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.getPolymorphicDescriptors
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.json.JsonClassDiscriminator


fun interface TsElementConverter {

  operator fun invoke(
    descriptor: SerialDescriptor,
  ): Set<TsElement>


  @OptIn(ExperimentalSerializationApi::class)
  open class Default(
    val elementIdConverter: TsElementIdConverter,
    val mapTypeConverter: TsMapTypeConverter,
    val typeRefConverter: TsTypeRefConverter,
    val serializersModule: SerializersModule = EmptySerializersModule(),
  ) : TsElementConverter {

    override operator fun invoke(
      descriptor: SerialDescriptor,
    ): Set<TsElement> {
      return when (descriptor.kind) {
        SerialKind.ENUM        -> setOf(convertEnum(descriptor))

        PrimitiveKind.BOOLEAN  -> setOf(TsLiteral.Primitive.TsBoolean)

        PrimitiveKind.CHAR,
        PrimitiveKind.STRING   -> setOf(TsLiteral.Primitive.TsString)

        PrimitiveKind.BYTE,
        PrimitiveKind.SHORT,
        PrimitiveKind.INT,
        PrimitiveKind.LONG,
        PrimitiveKind.FLOAT,
        PrimitiveKind.DOUBLE   -> setOf(TsLiteral.Primitive.TsNumber)

        StructureKind.LIST     -> setOf(
          when {
            descriptor.elementDescriptors.count() > 1 -> convertTuple(descriptor)
            else                                      -> convertList(descriptor)
          }
        )

        StructureKind.MAP      -> setOf(convertMap(descriptor))

        StructureKind.CLASS,
        StructureKind.OBJECT   -> setOf(
          when {
            descriptor.isInline -> convertTypeAlias(descriptor)
            else                -> convertInterface(descriptor)
          }
        )

        PolymorphicKind.SEALED -> convertDiscriminatedInterface(descriptor)

        SerialKind.CONTEXTUAL -> setOf(createTypeAliasAny(descriptor))

        PolymorphicKind.OPEN   -> convertOpenPolymorphic(descriptor)
      }
    }


    open fun convertOpenPolymorphic(
      descriptor: SerialDescriptor,
    ): Set<TsDeclaration> {
      val subclasses = serializersModule.getPolymorphicDescriptors(descriptor)
      if (subclasses.isEmpty()) return setOf(createTypeAliasAny(descriptor))

      val typeRefs = subclasses
        .map { TsTypeRef.Declaration(elementIdConverter(it), null, false) }
        .toSet()

      return setOf(TsDeclaration.TsTypeUnion(elementIdConverter(descriptor), typeRefs))
    }


    /**
     * Handle sealed-polymorphic descriptors.
     *
     * Generate...
     *
     * 1. a namespace that contains
     *   a. a 'type' enum, for each subclass
     *   b. the subclasses, as [TsDeclaration.TsInterface], with an additional 'type' field
     * 2. a type union of all subclasses
     */
    open fun convertDiscriminatedInterface(
      descriptor: SerialDescriptor,
    ): Set<TsDeclaration> {
      // namespace details
      val namespaceId = elementIdConverter(descriptor)
      val namespaceRef = TsTypeRef.Declaration(namespaceId, null, false)

      // The discriminator is configured on the sealed parent. A discriminator annotation on a
      // subclass cannot be represented here because this generator emits one shared property for
      // the whole hierarchy. kotlinx.serialization also requires subclass values to match.
      val discriminatorName = descriptor.annotations
        .filterIsInstance<JsonClassDiscriminator>()
        .firstOrNull()
        ?.discriminator
        ?: "type"

      // `type` is the historical default and is a valid unquoted property key even though it is
      // a contextual TypeScript keyword. Other names must at least be identifier-shaped here;
      // the derived enum name below receives the complete validator, including reserved names.
      if (!TsIdentifierValidator.isValidIdentifierSyntax(discriminatorName)) {
        throw InvalidTsIdentifierException(
          discriminatorName,
          "discriminator property of '${descriptor.serialName}'",
          "it must match ^[A-Za-z_$][A-Za-z0-9_$]*$",
        )
      }

      val subclassesDescriptorToInterface: Map<SerialDescriptor, TsDeclaration.TsInterface> =
        descriptor.elementDescriptors
          .firstOrNull { it.kind == SerialKind.CONTEXTUAL }
          ?.elementDescriptors
          ?.associateWith { this(it) }
          ?.mapValues { (_, v) ->
            v.filterIsInstance<TsDeclaration.TsInterface>()
              .map {
                it.copy(id = TsElementId("${descriptor.serialName}.${it.id.name}"))
              }.single()
          } ?: emptyMap()

      // verify a discriminated interface can be created
      if (subclassesDescriptorToInterface.isEmpty()) {
        // fallback: a type alias to 'any', same as for open-polymorphism
        return setOf(createTypeAliasAny(descriptor))
      } else {
        // discriminator enum
        val discriminatorEnum = run {
          val discriminatorEnumName = discriminatorName.replaceFirstChar { it.uppercaseChar() }
          TsIdentifierValidator.assertValidIdentifier(
            discriminatorEnumName,
            "discriminator enum for '${descriptor.serialName}'",
          )
          val id = TsElementId("${namespaceId.namespace}.$discriminatorEnumName")

          val members = subclassesDescriptorToInterface.entries.map { (subclassDescriptor, tsInterface) ->
            val enumMemberName = tsInterface.id.name
            TsIdentifierValidator.assertValidIdentifier(
              enumMemberName,
              "discriminator enum member of '${subclassDescriptor.serialName}'",
            )
            val enumMemberValue = TsTypeRef.Literal(
              TsLiteral.Custom(subclassDescriptor.serialName),
              false
            )
            TsProperty(enumMemberName, enumMemberValue, false)
          }.toSet()

          TsDeclaration.TsEnum(id, members)
        }

        val discriminatorEnumRef = TsTypeRef.Declaration(discriminatorEnum.id, namespaceRef, false)

        // add discriminator property to subclasses
        val subInterfacesWithTypeProp = subclassesDescriptorToInterface.map { (_, subclass) ->

          val subclassId = TsElementId(namespaceId.toString() + "." + subclass.id.name)

          val literalTypeRef = TsTypeRef.Declaration(
            TsElementId("${discriminatorEnum.id.name}.${subclassId.name}"),
            discriminatorEnumRef,
            false,
          )

          val literalTypeProperty = TsProperty(discriminatorName, literalTypeRef, false)

          subclass.copy(properties = setOf(literalTypeProperty) + subclass.properties)
        }

        // create type union and namespace
        val subInterfaceTypeUnion = run {
          val subInterfaceRefs =
            subclassesDescriptorToInterface.entries.map { (_, subclass) ->
              val subclassId = TsElementId(namespaceId.toString() + "." + subclass.id.name)
              TsTypeRef.Declaration(subclassId, namespaceRef, false)
            }.toSet()

          TsDeclaration.TsTypeUnion(
            namespaceId,
            subInterfaceRefs
          )
        }

        val namespace = TsDeclaration.TsNamespace(
          namespaceId,
          buildSet {
            add(discriminatorEnum)
            addAll(subInterfacesWithTypeProp)
          }
        )

        return setOf(subInterfaceTypeUnion, namespace)
      }
    }


    open fun convertTypeAlias(
      structDescriptor: SerialDescriptor,
    ): TsDeclaration {
      val resultId = elementIdConverter(structDescriptor)
      val fieldDescriptor = structDescriptor.elementDescriptors.first()
      val fieldTypeRef = typeRefConverter(fieldDescriptor)
      return TsDeclaration.TsTypeAlias(resultId, fieldTypeRef)
    }


    open fun convertInterface(
      descriptor: SerialDescriptor,
    ): TsDeclaration {
      val resultId = elementIdConverter(descriptor)

      val properties = convertProperties(descriptor)

      return TsDeclaration.TsInterface(resultId, properties)
    }


    open fun convertTuple(
      descriptor: SerialDescriptor,
    ): TsDeclaration.TsTuple {
      val resultId = elementIdConverter(descriptor)

      val properties = convertProperties(descriptor)

      return TsDeclaration.TsTuple(resultId, properties)
    }


    open fun convertProperties(
      descriptor: SerialDescriptor,
    ): Set<TsProperty> {
      return descriptor.elementDescriptors.mapIndexed { index, fieldDescriptor ->
        val name = descriptor.getElementName(index)
        val fieldTypeRef = typeRefConverter(fieldDescriptor)
        val optional = descriptor.isElementOptional(index)
        TsProperty(name, fieldTypeRef, optional)
      }.toSet()
    }


    open fun convertEnum(
      enumDescriptor: SerialDescriptor,
    ): TsDeclaration.TsEnum {
      val resultId = elementIdConverter(enumDescriptor)
      val members = convertProperties(enumDescriptor)
      members.forEach { member ->
        TsIdentifierValidator.assertValidIdentifier(
          member.name,
          "enum member of '${enumDescriptor.serialName}'",
        )
      }
      return TsDeclaration.TsEnum(resultId, members)
    }


    open fun convertList(
      listDescriptor: SerialDescriptor,
    ): TsLiteral.TsList {
      val elementDescriptor = listDescriptor.elementDescriptors.first()
      val elementTypeRef = typeRefConverter(elementDescriptor)
      return TsLiteral.TsList(elementTypeRef)
    }


    open fun convertMap(
      mapDescriptor: SerialDescriptor,
    ): TsLiteral.TsMap {

      val (keyDescriptor, valueDescriptor) = mapDescriptor.elementDescriptors.toList()

      val keyTypeRef = typeRefConverter(keyDescriptor)
      val valueTypeRef = typeRefConverter(valueDescriptor)

      val type = mapTypeConverter(keyDescriptor, valueDescriptor)

      return TsLiteral.TsMap(keyTypeRef, valueTypeRef, type)
    }


    open fun createTypeAliasAny(
      descriptor: SerialDescriptor,
    ): TsDeclaration.TsTypeAlias {
      val resultId = elementIdConverter(descriptor)
      val fieldTypeRef = TsTypeRef.Literal(TsLiteral.Primitive.TsAny, false)
      return TsDeclaration.TsTypeAlias(resultId, fieldTypeRef)
    }
  }
}
