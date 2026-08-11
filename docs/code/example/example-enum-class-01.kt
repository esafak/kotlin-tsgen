// This file was automatically generated from enums.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleEnumClass01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
enum class SomeType {
  Alpha,
  Beta,
  Gamma
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(SomeType.serializer()))
}
