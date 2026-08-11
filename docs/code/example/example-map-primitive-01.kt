// This file was automatically generated from maps.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleMapPrimitive01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
data class Config(
  val properties: Map<String, String>
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Config.serializer()))
}
