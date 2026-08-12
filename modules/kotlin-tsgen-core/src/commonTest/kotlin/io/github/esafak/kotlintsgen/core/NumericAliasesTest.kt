package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlin.jvm.JvmInline
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class NumericAliasesTest : FunSpec({
  test("signed numeric fields use reachable aliases") {
    KotlinTsGenerator().generate(NumericHolder.serializer()) shouldBe """
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
private data class CustomHolder(
  @Serializable(with = CustomIntSerializer::class)
  val value: CustomInt,
)

@Serializable(with = CustomIntSerializer::class)
@JvmInline
private value class CustomInt(val value: Int)

private object CustomIntSerializer : KSerializer<CustomInt> {
  override val descriptor = PrimitiveSerialDescriptor("CustomInt", PrimitiveKind.INT)
  override fun serialize(encoder: Encoder, value: CustomInt) = encoder.encodeInt(value.value)
  override fun deserialize(decoder: Decoder) = CustomInt(decoder.decodeInt())
}

@Serializable
@SerialName("Int")
private data class CollisionHolder(
  val numeric: Int,
  val value: CollisionType,
)

@Serializable
private data class CollisionType(val value: String)
