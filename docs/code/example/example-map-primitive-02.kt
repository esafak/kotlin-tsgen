// This file was automatically generated from maps.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleMapPrimitive02

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
class Application(
  val settings: Map<SettingKeys, String>
)

@Serializable
enum class SettingKeys {
  SCREEN_SIZE,
  MAX_MEMORY,
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Application.serializer()))
}
