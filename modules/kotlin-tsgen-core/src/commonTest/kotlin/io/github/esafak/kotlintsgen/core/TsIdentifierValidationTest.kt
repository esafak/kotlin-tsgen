package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.github.esafak.kotlintsgen.KotlinTsConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@OptIn(ExperimentalSerializationApi::class)
class TsIdentifierValidationTest : FunSpec({

  test("valid serial names remain valid identifiers") {
    KotlinTsGenerator().generate(ValidType.serializer()) shouldBe
      """
        |export interface ValidType {
        |  value: string;
        |}
      """.trimMargin()
  }

  test("invalid type serial names fail with context") {
    val exception = shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator().generate(InvalidTypeName.serializer())
    }

    exception.identifier shouldBe "invalid-type"
    exception.context shouldContain "type name"
    exception.message shouldContain "invalid-type"
  }

  test("leading digits fail as type names") {
    val exception = shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator().generate(LeadingDigitType.serializer())
    }

    exception.identifier shouldBe "1stType"
  }

  test("reserved words fail as type names") {
    val exception = shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator().generate(ReservedType.serializer())
    }

    exception.identifier shouldBe "class"
  }

  test("generated primitive type names fail as type names") {
    val exception = shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator().generate(PrimitiveNameType.serializer())
    }

    exception.identifier shouldBe "string"
  }

  test("serial names are validated for namespace segments") {
    val exception = shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator().generate(InvalidNamespaceType.serializer())
    }

    exception.identifier shouldBe "bad-segment"
    exception.context shouldContain "namespace segment"
  }

  test("serial names on enum members are validated") {
    val elementIdConverter = TsElementIdConverter { TsElementId("InvalidEnum") }
    val typeRefConverter = TsTypeRefConverter.Default(elementIdConverter)
    val converter = TsElementConverter.Default(
      elementIdConverter = elementIdConverter,
      mapTypeConverter = TsMapTypeConverter.Default,
      typeRefConverter = typeRefConverter,
    )

    val exception = shouldThrow<InvalidTsIdentifierException> {
      converter(InvalidEnum.serializer().descriptor)
    }

    exception.identifier shouldBe "invalid-member"
    exception.context shouldContain "enum member"
  }

  test("inline sealed discriminator enum members are validated") {
    val elementIdConverter = TsElementIdConverter { descriptor ->
      if (descriptor.serialName.endsWith("InvalidDiscriminatorSubclass")) {
        TsElementId("string")
      } else {
        TsElementIdConverter.Default(descriptor)
      }
    }
    val typeRefConverter = TsTypeRefConverter.Default(elementIdConverter)
    val converter = TsElementConverter.Default(
      elementIdConverter = elementIdConverter,
      mapTypeConverter = TsMapTypeConverter.Default,
      typeRefConverter = typeRefConverter,
    )

    val exception = shouldThrow<InvalidTsIdentifierException> {
      converter(DiscriminatorExample.Parent.serializer().descriptor)
    }

    exception.identifier shouldBe "string"
    exception.context shouldContain "discriminator enum member"
  }

  test("invalid serialized property names remain outside identifier validation") {
    val ts = KotlinTsGenerator().generate(InvalidPropertyName.serializer())

    ts shouldContain "\"invalid-property\": string;"
  }

  test("optional tuple elements place the marker on the label") {
    val tuple = TsDeclaration.TsTuple(
      id = TsElementId("OptionalTuple"),
      elements = setOf(
        TsProperty(
          name = "value",
          typeRef = TsTypeRef.Literal(TsLiteral.Primitive.TsString, nullable = false),
          optional = true,
        ),
      ),
    )

    val ts = TsSourceCodeGenerator.Default(KotlinTsConfig()).generateDeclaration(tuple)

    ts shouldContain "value?: string,"
    ts shouldNotContain "value: string?,"
  }

  test("malformed generic serial names fail rather than emit an invalid identifier") {
    val exception = shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator().generate(MalformedGenericSerializer)
    }

    exception.message.orEmpty() shouldContain "Map<kotlin"
  }

}) {
  @Serializable
  private class ValidType(val value: String)

  @Serializable
  @SerialName("invalid-type")
  private class InvalidTypeName(val value: String)

  @Serializable
  @SerialName("1stType")
  private class LeadingDigitType(val value: String)

  @Serializable
  @SerialName("class")
  private class ReservedType(val value: String)

  @Serializable
  @SerialName("string")
  private class PrimitiveNameType(val value: String)

  @Serializable
  @SerialName("bad-segment.Type")
  private class InvalidNamespaceType(val value: String)

  @Serializable
  private enum class InvalidEnum {
    @SerialName("invalid-member")
    VALUE,
  }

  private object DiscriminatorExample {
    @Serializable
    sealed class Parent

    @Serializable
    class InvalidDiscriminatorSubclass : Parent()
  }

  @Serializable
  private class InvalidPropertyName(
    @SerialName("invalid-property")
    val value: String,
  )

  private object MalformedGenericSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor(
      "pkg.Map<kotlin.String, kotlin.Int>",
      PrimitiveKind.STRING,
    )

    override fun serialize(encoder: Encoder, value: String) {
      encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String = decoder.decodeString()
  }

}
