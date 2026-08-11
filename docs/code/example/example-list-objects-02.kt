// This file was automatically generated from lists.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleListObjects02

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
data class Colour(
  val rgb: String
)

@Serializable
data class MyLists(
  val listOfMaps: List<Map<String, Int>>,
  val listOfColourMaps: List<Map<String, Colour>>,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(MyLists.serializer()))
}
