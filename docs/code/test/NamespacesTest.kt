// This file was automatically generated from namespaces.md by Knit tool. Do not edit.
@file:Suppress("JSUnusedLocalSymbols")
package io.github.esafak.kotlintsgen.example.test

import io.github.esafak.kotlintsgen.util.*
import io.kotest.core.spec.style.*
import io.kotest.matchers.*
import kotlinx.knit.test.*

class NamespacesTest : FunSpec({

  tags(Knit)
  context("ExampleNamespacesDisabled01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleNamespacesDisabled01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface Widget {
          |  value: string;
          |}
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExampleNamespacesCrossReference01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleNamespacesCrossReference01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export namespace org {
          |  export namespace one {
          |    export interface Parent {
          |      child: org.two.Child;
          |    }
          |  }
          |
          |  export namespace two {
          |    export interface Child {
          |      value: string;
          |    }
          |  }
          |}
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExampleNamespacesStatic01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleNamespacesStatic01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export namespace models {
          |  export interface Widget {
          |    value: string;
          |  }
          |}
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExampleNamespacesDescriptorPrefix01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleNamespacesDescriptorPrefix01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export namespace org {
          |  export namespace example {
          |    export interface Widget {
          |      value: string;
          |    }
          |  }
          |}
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }
})
