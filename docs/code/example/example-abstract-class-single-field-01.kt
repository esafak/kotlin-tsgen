// This file was automatically generated from polymorphism-open.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleAbstractClassSingleField01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
abstract class Color(val rgb: Int)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Color.serializer()))
}
