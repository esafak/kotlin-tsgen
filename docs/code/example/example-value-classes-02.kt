// This file was automatically generated from value-classes.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleValueClasses02

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

import kotlinx.serialization.builtins.serializer

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(
    tsGenerator.generate(
      UByte.serializer(),
      UShort.serializer(),
      UInt.serializer(),
      ULong.serializer(),
    )
  )
}
