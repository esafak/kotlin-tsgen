// This file was automatically generated from value-classes.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleValueClasses04

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
@JvmInline
value class UserCount(private val count: UInt)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(UserCount.serializer()))
}
