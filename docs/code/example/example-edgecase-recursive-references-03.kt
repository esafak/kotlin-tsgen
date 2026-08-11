// This file was automatically generated from edgecases.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleEdgecaseRecursiveReferences03

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
class A(
  val map: Map<String, B>
)

@Serializable
class B(
  val map: Map<String, A>
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(A.serializer(), B.serializer()))
}
