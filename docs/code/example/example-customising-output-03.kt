// This file was automatically generated from customising-output.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleCustomisingOutput03

import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

import kotlinx.serialization.builtins.serializer
import io.github.esafak.kotlintsgen.core.*

@Serializable
data class ItemHolder(
  val item: Item,
)

@Serializable
data class Item(
  val count: UInt? = 0u,
  val score: Int? = 0,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()

  tsGenerator.descriptorOverrides +=
    UInt.serializer().descriptor to TsDeclaration.TsTypeAlias(
      id = TsElementId("kotlin.UInt"),
      typeRef = TsTypeRef.Declaration(id = TsElementId("uint"), parent = null, nullable = false)
    )

  tsGenerator.descriptorOverrides += Int.serializer().descriptor to TsLiteral.Custom("customInt")

  println(tsGenerator.generate(ItemHolder.serializer()))
}
