// This file was automatically generated from customising-output.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleCustomisingOutput01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

import kotlinx.serialization.builtins.serializer
import io.github.esafak.kotlintsgen.core.*

@Serializable
data class Item(
  val price: Double,
  val count: Int,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()

  tsGenerator.mapTypes {
    Double.serializer() mapsTo typeAlias("Double", ref("double"))
  }

  println(tsGenerator.generate(Item.serializer()))
}
