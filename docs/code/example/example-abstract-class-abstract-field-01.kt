// This file was automatically generated from polymorphism-open.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleAbstractClassAbstractField01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
abstract class AbstractSimpleTypes {
  abstract val aString: String
  abstract var anInt: Int
  abstract val aDouble: Double
  abstract val bool: Boolean
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(AbstractSimpleTypes.serializer()))
}
