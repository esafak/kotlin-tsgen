// This file was automatically generated from lists.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleListPrimitive01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
data class MyLists(
  val strings: List<String>,
  val ints: Set<Int>,
  val longs: Collection<Long>,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(MyLists.serializer()))
}
