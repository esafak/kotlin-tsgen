// This file was automatically generated from maps.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleMapPrimitive05

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
data class Config(
  val nullableVals: Map<String, String?>,
  val nullableKeys: Map<String?, String>,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Config.serializer()))
}
