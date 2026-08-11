// This file was automatically generated from value-classes.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleValueClasses03

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

import io.github.esafak.kotlintsgen.KotlinTsConfig.TypeAliasTypingConfig.BrandTyping
import kotlinx.serialization.builtins.serializer

fun main() {

  val tsConfig = KotlinTsConfig(typeAliasTyping = BrandTyping)

  val tsGenerator = KotlinTsGenerator(config = tsConfig)
  println(
    tsGenerator.generate(
      ULong.serializer(),
    )
  )
}
