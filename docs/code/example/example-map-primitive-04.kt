// This file was automatically generated from maps.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleMapPrimitive04

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
@JvmInline
value class Data(val content: String)

@Serializable
class MyDataClass(
  val mapOfLists: Map<String, Data>
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(MyDataClass.serializer()))
}
