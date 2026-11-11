// This file was automatically generated from value-classes.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleValueClasses03

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

import io.github.esafak.kotlintsgen.core.TsBrand

@TsBrand
@Serializable
@JvmInline
value class UserId(val value: String)

@TsBrand
@Serializable
@JvmInline
value class UserGroupId(val value: String)

@Serializable
data class Membership(val user: UserId, val group: UserGroupId)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Membership.serializer()))
}
