// This file was automatically generated from polymorphism-sealed.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.examplePolymorphicSealedClass01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("kind")
sealed class Project {
  abstract val name: String
}

@Serializable
@SerialName("OProj")
class OwnedProject(override val name: String, val owner: String) : Project()

@Serializable
class DeprecatedProject(override val name: String, val reason: String) : Project()

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Project.serializer()))
}
