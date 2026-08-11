// This file was automatically generated from default-values.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleDefaultValuesPrimitiveFields02

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
data class ContactDetails(
  @Required
  val name: String,
  @Required
  val email: String?,
  @Required
  val active: Boolean = true,
  @Required
  val phoneNumber: String? = null,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(ContactDetails.serializer()))
}
