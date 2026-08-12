// This file was automatically generated from namespaces.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleNamespacesDisabled01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
class Widget(val value: String)

fun main() {
  println(KotlinTsGenerator().generate(Widget.serializer()))
}
