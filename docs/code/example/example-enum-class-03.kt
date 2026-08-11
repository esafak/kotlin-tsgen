// This file was automatically generated from enums.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleEnumClass03

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
enum class WireType {
  @SerialName("discussion")
  DISCUSSION,
  @SerialName("chat_room")
  CHAT_ROOM,
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(WireType.serializer()))
}
