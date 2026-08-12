// This file was automatically generated from customising-output.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleCustomisingOutput02

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

import kotlinx.serialization.builtins.serializer
import io.github.esafak.kotlintsgen.core.*

@Serializable
data class Position(
  val x: Double,
  val y: Double?,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()

  tsGenerator.mapTypes {
    Double.serializer() mapsTo external("double")
  }

  println(tsGenerator.generate(Position.serializer()))
}
