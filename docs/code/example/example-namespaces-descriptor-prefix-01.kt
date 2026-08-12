// This file was automatically generated from namespaces.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleNamespacesDescriptorPrefix01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
@SerialName("org.example.Widget")
class Widget(val value: String)

fun main() {
  val config = KotlinTsConfig(
    namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
  )
  println(KotlinTsGenerator(config).generate(Widget.serializer()))
}
