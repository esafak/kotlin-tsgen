package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsConfig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain


class TsElementIdTest : FunSpec({

  test("root IDs have no namespace segments") {
    val id = TsElementId("User")

    id.name shouldBe "User"
    id.namespace shouldBe "User"
    id.namespaceSegments.shouldBeEmpty()
    id.toString() shouldBe "User"
  }

  test("one-level namespaces preserve public and structured views") {
    val id = TsElementId("models.User")

    id.name shouldBe "User"
    id.namespace shouldBe "models"
    id.namespaceSegments shouldBe listOf("models")
    id.toString() shouldBe "models.User"
  }

  test("multi-level namespaces preserve segment order") {
    val id = TsElementId("org.example.models.User")

    id.name shouldBe "User"
    id.namespace shouldBe "org.example.models"
    id.namespaceSegments shouldBe listOf("org", "example", "models")
    id.toString() shouldBe "org.example.models.User"
  }

  test("invalid namespace segments remain visible to validation") {
    val id = TsElementId("bad-segment.User")

    id.namespaceSegments shouldBe listOf("bad-segment")
    id.name shouldBe "User"
  }

  test("empty namespace segments remain visible") {
    TsElementId(".User").namespaceSegments shouldBe listOf("")
  }

  test("nested references retain their qualified namespace") {
    val generator = TsSourceCodeGenerator.Default(
      KotlinTsConfig(
        namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
      ),
    )
    val parent = TsDeclaration.TsInterface(
      TsElementId("org.one.Parent"),
      setOf(
        TsProperty(
          "child",
          TsTypeRef.Declaration(TsElementId("org.two.Child"), null, false),
          false,
        ),
      ),
    )

    generator.generateDeclarationInNamespace(parent, "org.one") shouldContain
      "child: org.two.Child;"
  }
})
