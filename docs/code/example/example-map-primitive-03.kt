// This file was automatically generated from maps.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleMapPrimitive03

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
class MapsWithLists(
  val mapOfLists: Map<String, List<String>>
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(MapsWithLists.serializer()))
}
