// This file was automatically generated from namespaces.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleNamespacesStatic01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
class Widget(val value: String)

fun main() {
  val config = KotlinTsConfig(
    namespaceConfig = KotlinTsConfig.NamespaceConfig.Static("models"),
  )
  println(KotlinTsGenerator(config).generate(Widget.serializer()))
}
