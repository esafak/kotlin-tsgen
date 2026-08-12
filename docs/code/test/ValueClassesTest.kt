// This file was automatically generated from value-classes.md by Knit tool. Do not edit.
@file:Suppress("JSUnusedLocalSymbols")
package io.github.esafak.kotlintsgen.example.test

import io.github.esafak.kotlintsgen.util.*
import io.kotest.core.spec.style.*
import io.kotest.matchers.*
import kotlinx.knit.test.*

class ValueClassesTest : FunSpec({

  tags(Knit)
  context("ExampleValueClasses01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleValueClasses01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export type AuthToken = string;
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExampleValueClasses02") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleValueClasses02.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export type UByte = Byte;
          |
          |export type UShort = Short;
          |
          |export type UInt = Int;
          |
          |export type ULong = Long;
          |
          |export type Byte = number;
          |
          |export type Short = number;
          |
          |export type Int = number;
          |
          |export type Long = number;
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExampleValueClasses03") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleValueClasses03.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export type ULong = Long & { __ULong__: void };
          |
          |export type Long = number & { __Long__: void };
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExampleValueClasses04") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleValueClasses04.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export type UserCount = UInt;
          |
          |export type UInt = Int;
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
})
