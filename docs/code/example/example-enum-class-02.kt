// This file was automatically generated from enums.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleEnumClass02

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
enum class SomeType2(val coolName: String) {
  Alpha("alpha") {
    val extra: Long = 123L
  },
  Beta("be_beta"),
  Gamma("gamma 3 3 3")
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(SomeType2.serializer()))
}
