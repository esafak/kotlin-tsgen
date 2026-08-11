// This file was automatically generated from default-values.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleDefaultValuesSingleField01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
class Colour(val rgb: Int = 12345)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Colour.serializer()))
}
