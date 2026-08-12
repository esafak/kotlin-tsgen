package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsConfig
import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.assertions.throwables.shouldThrow
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

  test("descriptor namespaces qualify cross-namespace references") {
    val config = KotlinTsConfig(
      namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
    )

    val ts = KotlinTsGenerator(config).generate(CrossNamespaceParent.serializer())

    ts shouldContain "child: org.two.Child;"
    ts shouldContain "export namespace one"
    ts shouldContain "export namespace two"
  }

  test("descriptor namespace keeps a reachable namespaced Int distinct from the builtin alias") {
    val config = KotlinTsConfig(
      namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
    )

    val ts = KotlinTsGenerator(config).generate(NamespacedInt.serializer())
    ts shouldContain "export type Int = number;"
    ts shouldContain "export namespace org"
    ts shouldContain "export namespace example"
    ts shouldContain "export interface Int"
  }

  test("disabled namespace rejects the same rendered-name collision") {
    shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator().generate(NamespacedInt.serializer())
    }
  }

  test("flat namespaces reject colliding declarations") {
    val exception = shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator().generate(CollidingHolder.serializer())
    }

    exception.identifier shouldBe "Kind"
    exception.message shouldContain "A.Kind"
    exception.message shouldContain "B.Kind"
    exception.message shouldContain "DescriptorNamePrefix"
  }

  test("descriptor namespaces resolve colliding declarations") {
    val config = KotlinTsConfig(
      namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
    )

    val ts = KotlinTsGenerator(config).generate(CollidingHolder.serializer())

    ts shouldContain "export namespace A"
    ts shouldContain "export namespace B"
    ts shouldContain "export enum Kind"
  }

  test("static namespaces reject colliding declarations") {
    val config = KotlinTsConfig(
      namespaceConfig = KotlinTsConfig.NamespaceConfig.Static("models"),
    )

    shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator(config).generate(CollidingHolder.serializer())
    }
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

  @Serializable
  @SerialName("org.one.Parent")
  private class CrossNamespaceParent(val child: CrossNamespaceChild)

  @Serializable
  @SerialName("org.two.Child")
  private class CrossNamespaceChild(val value: String)

  @Serializable
  @SerialName("org.example.Int")
  private class NamespacedInt(val value: Int)

  @Serializable
  private class CollidingHolder(
    val first: A,
    val second: B,
  )

  @Serializable
  @SerialName("A")
  private class A(val kind: AKind)

  @Serializable
  @SerialName("B")
  private class B(val kind: BKind)

  @Serializable
  @SerialName("A.Kind")
  private enum class AKind { ONE }

  @Serializable
  @SerialName("B.Kind")
  private enum class BKind { TWO }

  private class GroupingSource(
    private val group: String?,
  ) : TsSourceCodeGenerator by TsSourceCodeGenerator.Default(KotlinTsConfig()) {
    override fun groupElementsBy(element: TsElement): String? = group
  }
}
