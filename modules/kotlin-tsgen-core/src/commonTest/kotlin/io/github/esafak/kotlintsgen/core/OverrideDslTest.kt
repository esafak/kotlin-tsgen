package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer


class OverrideDslTest : FunSpec({
  test("mapTypes configures serializer and descriptor overrides") {
    val generator = KotlinTsGenerator()
    generator.mapTypes {
      Double.serializer() mapsTo external("double")
      Int.serializer() mapsTo custom("integer")
      UInt.serializer() mapsTo typeAlias("UInt", ref("uint"))
    }

    generator.generate(OverrideDslHolder.serializer()) shouldBe """
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

    generator.generate(AccumulatedOverrideHolder.serializer()) shouldBe """
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

    generator.generate(ExternalTypeHolderForBuilders.serializer()) shouldBe """
      |export interface ExternalTypeHolderForBuilders {
      |  value: Double;
      |}
      |
      |export type Double = double;
    """.trimMargin()
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
