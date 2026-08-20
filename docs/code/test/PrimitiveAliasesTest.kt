package io.github.esafak.kotlintsgen.example.test

import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.github.esafak.kotlintsgen.util.shouldTypeScriptCompile
import io.github.esafak.kotlintsgen.util.tsCompile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

class PrimitiveAliasesTest :
  FunSpec({
    test("generated files compile with consumer use-sites").config(tags = tsCompile) {
      val generator = KotlinTsGenerator()
      val generated = generator.generate(Generated.serializer())
      val nested = generator.generate(NullableNested.serializer())
      val mixed = generator.generate(Mixed.serializer())

      generated shouldContain "createdAt: Instant;"

      (
        generated +
          """

      |declare const value: Generated;
      |const description: string | null = value.description;
      |const createdAt: string = value.createdAt;
          """.trimMargin()
      ).shouldTypeScriptCompile("primitive-aliases-generated")

      (
        nested +
          """

      |declare const value: NullableNested;
      |const description: string | null = value.description;
          """.trimMargin()
      ).shouldTypeScriptCompile("primitive-aliases-nested")

      (
        mixed +
          """

      |declare const value: Mixed;
      |const description: string | null = value.nested.description;
          """.trimMargin()
      ).shouldTypeScriptCompile("primitive-aliases-mixed")
    }
  })

@Serializable
private data class Generated(
  val description: String?,
  val createdAt: Instant,
  val values: Map<String, String>,
)

@Serializable
private data class NullableNested(
  val description: String?,
)

@Serializable
private data class Mixed(
  val plain: String,
  val nested: NullableNested,
)

@Serializable(with = InstantSerializer::class)
@JvmInline
private value class Instant(
  val value: String,
)

private object InstantSerializer : KSerializer<Instant> {
  override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

  override fun serialize(
    encoder: Encoder,
    value: Instant,
  ) = encoder.encodeString(value.value)

  override fun deserialize(decoder: Decoder) = Instant(decoder.decodeString())
}
