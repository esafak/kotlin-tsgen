// This file was automatically generated from basic-classes.md by Knit tool. Do not edit.
@file:Suppress("JSUnusedLocalSymbols")
package io.github.esafak.kotlintsgen.example.test

import io.github.esafak.kotlintsgen.util.*
import io.kotest.core.spec.style.*
import io.kotest.matchers.*
import kotlinx.knit.test.*

class BasicClassesTest : FunSpec({

  tags(Knit)
  context("ExamplePlainClassSingleField01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.examplePlainClassSingleField01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface Color {
          |  rgb: Int;
          |}
          |
          |export type Int = number;
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExamplePlainClassPrimitiveFields01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.examplePlainClassPrimitiveFields01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface SimpleTypes {
          |  aString: string;
          |  anInt: Int;
          |  aDouble: Double;
          |  bool: boolean;
          |  privateMember: string;
          |}
          |
          |export type Int = number;
          |
          |export type Double = number;
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExamplePlainDataClass01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.examplePlainDataClass01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface SomeDataClass {
          |  aString: string;
          |  anInt: Int;
          |  aDouble: Double;
          |  bool: boolean;
          |  privateMember: string;
          |}
          |
          |export type Int = number;
          |
          |export type Double = number;
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }
})
