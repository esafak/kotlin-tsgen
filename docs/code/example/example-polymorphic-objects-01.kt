// This file was automatically generated from polymorphism-sealed.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.examplePolymorphicObjects01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
sealed class Response

@Serializable
object EmptyResponse : Response()

@Serializable
class TextResponse(val text: String) : Response()

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(
    tsGenerator.generate(Response.serializer())
  )
}
