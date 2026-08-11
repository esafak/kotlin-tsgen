// This file was automatically generated from polymorphism-sealed.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.examplePolymorphicStaticTypes01

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
open class Project(val name: String)

class OwnedProject(name: String, val owner: String) : Project(name)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Project.serializer()))
}
