package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsConfig
import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


class NamespaceRenderingTest : FunSpec({

  test("disabled namespaces preserve flat output") {
    KotlinTsGenerator().generate(PlainType.serializer()) shouldBe
      """
        |export interface PlainType {
        |  value: string;
        |}
      """.trimMargin()
  }

  test("static namespaces wrap plain declarations") {
    val config = KotlinTsConfig(
      namespaceConfig = KotlinTsConfig.NamespaceConfig.Static("models"),
    )

    KotlinTsGenerator(config).generate(PlainType.serializer()) shouldBe
      """
        |export namespace models {
        |  export interface PlainType {
        |    value: string;
        |  }
        |}
      """.trimMargin()
  }

  test("static namespaces wrap a sealed hierarchy once") {
    val config = KotlinTsConfig(
      namespaceConfig = KotlinTsConfig.NamespaceConfig.Static("models"),
    )
    val ts = KotlinTsGenerator(config).generate(SealedParent.serializer())

    ts.split("export namespace models").size - 1 shouldBe 1
    ts shouldContain "export type SealedParent ="
    ts shouldContain "export interface SealedChild"
  }

  test("descriptor prefixes become nested namespaces") {
    val config = KotlinTsConfig(
      namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
    )

    KotlinTsGenerator(config).generate(PrefixedType.serializer()) shouldBe
      """
        |export namespace org {
        |  export namespace example {
        |    export interface PrefixedType {
        |      value: string;
        |    }
        |  }
        |}
      """.trimMargin()
  }

  test("dotless descriptor names remain flat") {
    val config = KotlinTsConfig(
      namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
    )

    KotlinTsGenerator(config).generate(DotlessType.serializer()) shouldBe
      """
        |export interface DotlessType {
        |  value: string;
        |}
      """.trimMargin()
  }

  test("null and blank grouping keys remain flat") {
    val nullGrouped = KotlinTsGenerator(
      sourceCodeGenerator = GroupingSource(null),
    ).generate(PlainType.serializer())
    val blankGrouped = KotlinTsGenerator(
      sourceCodeGenerator = GroupingSource(" "),
    ).generate(PlainType.serializer())

    nullGrouped shouldBe blankGrouped
    nullGrouped shouldContain "export interface PlainType"
    nullGrouped shouldNotContain "export namespace"
  }

  test("namespace segments are validated") {
    val config = KotlinTsConfig(
      namespaceConfig = KotlinTsConfig.NamespaceConfig.Static("bad-segment"),
    )

    val exception = io.kotest.assertions.throwables.shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator(config).generate(PlainType.serializer())
    }

    exception.identifier shouldBe "bad-segment"
    exception.context shouldContain "namespace segment"
  }

}) {
  @Serializable
  private class PlainType(val value: String)

  @Serializable
  @SerialName("DotlessType")
  private class DotlessType(val value: String)

  @Serializable
  sealed class SealedParent

  @Serializable
  private class SealedChild : SealedParent()

  @Serializable
  @SerialName("org.example.PrefixedType")
  private class PrefixedType(val value: String)

  private class GroupingSource(
    private val group: String?,
  ) : TsSourceCodeGenerator by TsSourceCodeGenerator.Default(KotlinTsConfig()) {
    override fun groupElementsBy(element: TsElement): String? = group
  }
}
