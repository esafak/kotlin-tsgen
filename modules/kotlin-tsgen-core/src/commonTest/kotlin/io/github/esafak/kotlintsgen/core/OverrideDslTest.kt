package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

class OverrideDslTest :
  FunSpec({
    test("mapTypes configures serializer and descriptor overrides") {
      val generator = KotlinTsGenerator()
      generator.mapTypes {
        Double.serializer() mapsTo external("double")
        Int.serializer() mapsTo custom("integer")
        UInt.serializer() mapsTo typeAlias("UInt", ref("uint"))
      }

      generator.generate(OverrideDslHolder.serializer()) shouldBe
        """
      |export interface OverrideDslHolder {
      |  external: double;
      |  inline: integer;
      |  alias: UInt;
      |}
      |
      |export type UInt = uint;
        """.trimMargin()
    }

    test("mapTypes calls accumulate") {
      val generator = KotlinTsGenerator()
      generator.mapTypes { Double.serializer() mapsTo external("double") }
      generator.mapTypes { Int.serializer() mapsTo custom("integer") }

      generator.generate(AccumulatedOverrideHolder.serializer()) shouldBe
        """
      |export interface AccumulatedOverrideHolder {
      |  external: double;
      |  inline: integer;
      |}
        """.trimMargin()
    }

    test("builders compose with the existing descriptorOverrides map") {
      val generator = KotlinTsGenerator()
      generator.descriptorOverrides +=
        Double.serializer().descriptor to typeAlias("Double", external("double"))

      generator.generate(ExternalTypeHolderForBuilders.serializer()) shouldBe
        """
      |export interface ExternalTypeHolderForBuilders {
      |  value: Double;
      |}
      |
      |export type Double = double;
        """.trimMargin()
    }

    test("descriptor overrides are refreshed between generate calls") {
      val generator = KotlinTsGenerator()
      generator.mapTypes { String.serializer() mapsTo custom("LegacyString") }

      generator.generate(OverrideCacheHolder.serializer()) shouldContain "value: LegacyString;"

      generator.descriptorOverrides.clear()
      generator.generate(OverrideCacheHolder.serializer()) shouldBe
        """
      |export interface OverrideCacheHolder {
      |  value: string;
      |}
        """.trimMargin()
    }

    test("serializer descriptor overrides are refreshed between generate calls") {
      val generator = KotlinTsGenerator()
      val serializer = OverrideCacheHolder.serializer()
      generator.serializerDescriptorOverrides[serializer] =
        setOf(DescriptorReplacement.serializer().descriptor)

      generator.generate(serializer) shouldContain "export interface DescriptorReplacement {"

      generator.serializerDescriptorOverrides.clear()
      generator.generate(serializer) shouldContain "export interface OverrideCacheHolder {"
    }

    test("custom overrides are rejected for index signature keys") {
      val generator = KotlinTsGenerator()
      generator.mapTypes { String.serializer() mapsTo custom("Locale") }

      shouldThrow<InvalidTsIdentifierException> {
        generator.generate(CustomIndexKeyHolder.serializer())
      }
    }
  })

@Serializable
private data class OverrideDslHolder(
  val external: Double,
  val inline: Int,
  val alias: UInt,
)

@Serializable
private data class AccumulatedOverrideHolder(
  val external: Double,
  val inline: Int,
)

@Serializable
private data class ExternalTypeHolderForBuilders(
  val value: Double,
)

@Serializable
private data class OverrideCacheHolder(
  val value: String,
)

@Serializable
private data class DescriptorReplacement(
  val replacement: Int,
)

@Serializable
private data class CustomIndexKeyHolder(
  val values: Map<String, String>,
)
