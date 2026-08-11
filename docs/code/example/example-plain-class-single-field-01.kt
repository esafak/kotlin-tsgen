// This file was automatically generated from basic-classes.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.examplePlainClassSingleField01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
class Color(val rgb: Int)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Color.serializer()))
}
