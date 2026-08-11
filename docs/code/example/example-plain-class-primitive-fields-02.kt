// This file was automatically generated from basic-classes.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.examplePlainClassPrimitiveFields02

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

import kotlinx.serialization.Transient

@Serializable
class SimpleTypes(
  @Transient
  val aString: String = "default-value"
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(SimpleTypes.serializer()))
}
