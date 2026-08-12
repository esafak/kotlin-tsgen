// This file was automatically generated from customising-output.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleCustomisingOutput03

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

  tsGenerator.descriptorOverrides +=
    Double.serializer().descriptor to TsLiteral.Custom("customDouble")

  println(tsGenerator.generate(Item.serializer()))
}
