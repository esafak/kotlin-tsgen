// This file was automatically generated from default-values.md by Knit tool. Do not edit.
@file:Suppress("JSUnusedLocalSymbols")
package io.github.esafak.kotlintsgen.example.test

import io.github.esafak.kotlintsgen.util.*
import io.kotest.core.spec.style.*
import io.kotest.matchers.*
import kotlinx.knit.test.*

class DefaultValuesTest : FunSpec({

  tags(Knit)
  context("ExampleDefaultValuesSingleField01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleDefaultValuesSingleField01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface Colour {
          |  rgb?: Int;
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

  context("ExampleDefaultValuesSingleField02") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleDefaultValuesSingleField02.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface Colour {
          |  rgb: Int | null;
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

  context("ExampleDefaultValuesPrimitiveFields01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleDefaultValuesPrimitiveFields01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface ContactDetails {
          |  name: string;
          |  email: string | null;
          |  active?: boolean;
          |  phoneNumber?: string | null;
          |}
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExampleDefaultValuesPrimitiveFields02") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleDefaultValuesPrimitiveFields02.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface ContactDetails {
          |  name: string;
          |  email: string | null;
          |  active: boolean;
          |  phoneNumber: string | null;
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
