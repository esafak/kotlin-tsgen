// This file was automatically generated from polymorphism-sealed.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.examplePolymorphicStaticTypes02

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

import kotlinx.serialization.modules.*

@Serializable
abstract class Project {
  abstract val name: String
}

@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

val module = SerializersModule {
  polymorphic(Project::class) {
    subclass(OwnedProject::class)
  }
}

fun main() {
  val config = KotlinTsConfig(serializersModule = module)

  val tsGenerator = KotlinTsGenerator(config)

  println(tsGenerator.generate(Project.serializer()))
}
