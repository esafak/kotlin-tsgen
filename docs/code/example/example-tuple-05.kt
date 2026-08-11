// This file was automatically generated from tuples.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleTuple05

import io.github.esafak.kotlintsgen.*
import io.github.esafak.kotlintsgen.core.experiments.TupleSerializer
import kotlinx.serialization.*

import io.github.esafak.kotlintsgen.example.exampleTuple04.Coordinates

@Serializable
class GameLocations(
  val homeLocation: Coordinates,
  val allLocations: List<Coordinates>,
  val namedLocations: Map<String, Coordinates>,
  val locationsInfo: Map<Coordinates, String>,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(GameLocations.serializer()))
}
