// This file was automatically generated from namespaces.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleNamespacesCrossReference01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
@SerialName("org.one.Parent")
class Parent(val child: Child)

@Serializable
@SerialName("org.two.Child")
class Child(val value: String)

fun main() {
  val config = KotlinTsConfig(
    namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
  )
  println(KotlinTsGenerator(config).generate(Parent.serializer()))
}
