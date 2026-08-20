package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

class NumericAliasesTest :
  FunSpec({
    test("signed numeric fields use reachable aliases") {
      KotlinTsGenerator().generate(NumericHolder.serializer()) shouldBe
        """
      |export interface NumericHolder {
      |  byte: Byte;
      |  short: Short;
      |  int: Int;
      |  long: Long;
      |  float: Float;
      |  double: Double;
      |}
      |
      |export type Byte = number;
      |
      |export type Short = number;
      |
      |export type Int = number;
      |
      |export type Long = number;
      |
      |export type Float = number;
      |
      |export type Double = number;
        """.trimMargin()
    }

    test("numeric aliases work in collections and nullable fields") {
      val ts = KotlinTsGenerator().generate(CollectionHolder.serializer())
      ts shouldContain "values: Int[];"
      ts shouldContain "nullable: Double | null;"
    }

    test("nullable built-in primitives render as lowercase primitives") {
      KotlinTsGenerator().generate(BuiltInPrimitiveHolder.serializer()) shouldBe
        """
      |export interface BuiltInPrimitiveHolder {
      |  description: string | null;
      |  flag: boolean | null;
      |  character: string | null;
      |  plain: string;
      |}
        """.trimMargin()
    }

    test("nullable built-in primitives remain lowercase when encountered first") {
      val ts = KotlinTsGenerator().generate(NullableFirstPrimitiveHolder.serializer())

      ts shouldContain "nullable: string | null;"
      ts shouldContain "plain: string;"
      ts shouldNotContain "String"
      ts shouldNotContain "type Boolean"
      ts shouldNotContain "type Char"
    }

    test("shared generators do not retain built-in aliases between calls") {
      val generator = KotlinTsGenerator()
      val first = generator.generate(NullableNestedHolder.serializer())
      val expectedFirst = KotlinTsGenerator().generate(NullableNestedHolder.serializer())

      val actual = generator.generate(MixedPrimitiveHolder.serializer())
      val expected = KotlinTsGenerator().generate(MixedPrimitiveHolder.serializer())

      first shouldBe expectedFirst
      actual shouldBe expected
      actual shouldContain "value: string | null;"
      actual shouldNotContain "String"
    }

    test("custom string aliases use literal string map keys") {
      val ts = KotlinTsGenerator().generate(CustomStringKeyHolder.serializer())

      ts shouldContain "{ [key: string]: string }"
      ts shouldContain "export type StringId = string;"
    }

    test("plain string map keys use literal string index signatures") {
      KotlinTsGenerator().generate(StringMapHolder.serializer()) shouldContain
        "{ [key: string]: string }"
    }

    test("Map<Int, String> uses a numeric index signature and preserves value types") {
      val ts = KotlinTsGenerator().generate(MapHolder.serializer())
      ts shouldContain "{ [key: number]: string }"
      ts shouldContain "map:"
    }

    test("root numeric serializers emit an alias") {
      KotlinTsGenerator().generate(Int.serializer()) shouldBe "export type Int = number;"
    }

    test("custom primitive serializers use aliases in nested fields") {
      val ts = KotlinTsGenerator().generate(CustomHolder.serializer())
      ts shouldContain "value: CustomInt;"
      ts shouldContain "export type CustomInt = number;"
    }

    test("reachable declaration name collisions fail generation") {
      shouldThrow<InvalidTsIdentifierException> {
        KotlinTsGenerator().generate(CollisionHolder.serializer())
      }
    }

    test("primitive aliases with the same rendered name fail generation") {
      shouldThrow<InvalidTsIdentifierException> {
        KotlinTsGenerator().generate(PrimitiveAliasCollisionHolder.serializer())
      }
    }
  })

@Serializable
private data class NumericHolder(
  val byte: Byte,
  val short: Short,
  val int: Int,
  val long: Long,
  val float: Float,
  val double: Double,
)

@Serializable
private data class CollectionHolder(
  val values: List<Int>,
  val nullable: Double?,
)

@Serializable
private data class MapHolder(
  val map: Map<Int, String>,
)

@Serializable
private data class BuiltInPrimitiveHolder(
  val description: String?,
  val flag: Boolean?,
  val character: Char?,
  val plain: String,
)

@Serializable
private data class NullableFirstPrimitiveHolder(
  val nullable: String?,
  val plain: String,
)

@Serializable
private data class NullableNestedHolder(
  val value: String?,
)

@Serializable
private data class MixedPrimitiveHolder(
  val plain: String,
  val nested: NullableNestedHolder,
)

@Serializable
private data class CustomHolder(
  @Serializable(with = CustomIntSerializer::class)
  val value: CustomInt,
)

@Serializable(with = CustomIntSerializer::class)
@JvmInline
private value class CustomInt(
  val value: Int,
)

private object CustomIntSerializer : KSerializer<CustomInt> {
  override val descriptor = PrimitiveSerialDescriptor("CustomInt", PrimitiveKind.INT)

  override fun serialize(
    encoder: Encoder,
    value: CustomInt,
  ) = encoder.encodeInt(value.value)

  override fun deserialize(decoder: Decoder) = CustomInt(decoder.decodeInt())
}

@Serializable
@SerialName("Int")
private data class CollisionHolder(
  val numeric: Int,
  val value: CollisionType,
)

@Serializable
private data class CollisionType(
  val value: String,
)

@Serializable
private data class PrimitiveAliasCollisionHolder(
  val int: Int,
  @Serializable(with = StringIntSerializer::class) val custom: StringInt,
)

@Serializable(with = StringIntSerializer::class)
@JvmInline
private value class StringInt(
  val value: String,
)

private object StringIntSerializer : KSerializer<StringInt> {
  override val descriptor = PrimitiveSerialDescriptor("Int", PrimitiveKind.STRING)

  override fun serialize(
    encoder: Encoder,
    value: StringInt,
  ) = encoder.encodeString(value.value)

  override fun deserialize(decoder: Decoder) = StringInt(decoder.decodeString())
}

@Serializable
private data class CustomStringKeyHolder(
  val values: Map<StringId, String>,
)

@Serializable
private data class StringMapHolder(
  val values: Map<String, String>,
)

@Serializable(with = StringIdSerializer::class)
@JvmInline
private value class StringId(
  val value: String,
)

private object StringIdSerializer : KSerializer<StringId> {
  override val descriptor = PrimitiveSerialDescriptor("StringId", PrimitiveKind.STRING)

  override fun serialize(
    encoder: Encoder,
    value: StringId,
  ) = encoder.encodeString(value.value)

  override fun deserialize(decoder: Decoder) = StringId(decoder.decodeString())
}
