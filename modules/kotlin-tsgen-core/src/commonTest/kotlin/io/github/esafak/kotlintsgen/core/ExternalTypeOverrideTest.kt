package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsConfig
import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer


class ExternalTypeOverrideTest : FunSpec({
  test("external type overrides are referenced but not emitted") {
    val generator = KotlinTsGenerator()
    generator.descriptorOverrides[Double.serializer().descriptor] = external("double")

    generator.generate(ExternalTypeHolder.serializer()) shouldBe """
      |export interface ExternalTypeHolder {
      |  value: double;
      |  nullable: double | null;
      |}
    """.trimMargin()
  }

  test("external type references preserve namespaces") {
    val generator = KotlinTsGenerator(
      config = KotlinTsConfig(
        namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
      ),
    )
    generator.descriptorOverrides[Double.serializer().descriptor] = external("library.double")

    generator.generate(ExternalTypeHolder.serializer()) shouldContain "value: library.double;"
  }

  test("external type names remain exact with namespaces disabled") {
    val generator = KotlinTsGenerator()
    generator.descriptorOverrides[Double.serializer().descriptor] = external("library.double")

    generator.generate(ExternalTypeHolder.serializer()) shouldContain "value: library.double;"
  }

  test("external type overrides work as map keys") {
    val generator = KotlinTsGenerator()
    generator.descriptorOverrides[Double.serializer().descriptor] = external("double")

    generator.generate(ExternalTypeMapHolder.serializer()) shouldContain
      "values: Map<double, string>;"
  }
})


@Serializable
private data class ExternalTypeHolder(
  val value: Double,
  val nullable: Double?,
)


@Serializable
private data class ExternalTypeMapHolder(
  val values: Map<Double, String>,
)
