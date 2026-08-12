// This file was automatically generated from lists.md by Knit tool. Do not edit.
@file:Suppress("JSUnusedLocalSymbols")
package io.github.esafak.kotlintsgen.example.test

import io.github.esafak.kotlintsgen.util.*
import io.kotest.core.spec.style.*
import io.kotest.matchers.*
import kotlinx.knit.test.*

class ListsTests : FunSpec({

  tags(Knit)
  context("ExampleListPrimitive01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleListPrimitive01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface MyLists {
          |  strings: string[];
          |  ints: Int[];
          |  longs: Long[];
          |}
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

  context("ExampleListObjects01") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleListObjects01.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface MyLists {
          |  colours: Colour[];
          |  colourGroups: Colour[][];
          |  colourGroupGroups: Colour[][][];
          |}
          |
          |export interface Colour {
          |  rgb: string;
          |}
        """.trimMargin()
        .normalize()
      )
    }

    test("expect actual compiles").config(tags = tsCompile) {
      actual.shouldTypeScriptCompile(caseName)
    }
  }

  context("ExampleListObjects02") {
    val caseName = testCase.name.name

    val actual = captureOutput(caseName) {
      io.github.esafak.kotlintsgen.example.exampleListObjects02.main()
    }.normalizeJoin()

    test("expect actual matches TypeScript") {
      actual.shouldBe(
        // language=TypeScript
        """
          |export interface MyLists {
          |  listOfMaps: { [key: string]: Int }[];
          |  listOfColourMaps: { [key: string]: Colour }[];
          |}
          |
          |export interface Colour {
          |  rgb: string;
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
})
