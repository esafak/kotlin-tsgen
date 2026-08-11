// This file was automatically generated from tuples.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleTuple01

import io.github.esafak.kotlintsgen.*
import io.github.esafak.kotlintsgen.core.experiments.TupleSerializer
import kotlinx.serialization.*

@Serializable(with = SimpleTypes.SimpleTypesSerializer::class)
data class SimpleTypes(
  val aString: String,
  var anInt: Int,
  val aDouble: Double?,
  val bool: Boolean,
  private val privateMember: String,
) {
  // Create `SimpleTypesSerializer` inside `SimpleTypes`, so it
  // has access to the private property `privateMember`.
  object SimpleTypesSerializer : TupleSerializer<SimpleTypes>(
    "SimpleTypes",
    {
      // Provide all tuple elements, in order, using the 'elements' helper method.
      element(SimpleTypes::aString)
      element(SimpleTypes::anInt)
      element(SimpleTypes::aDouble)
      element(SimpleTypes::bool)
      element(SimpleTypes::privateMember)
    }
  ) {
    override fun tupleConstructor(elements: Iterator<*>): SimpleTypes {
      // When deserializing, the elements will be available as a list, in the order defined above
      return SimpleTypes(
        elements.next() as String,
        elements.next() as Int,
        elements.next() as Double,
        elements.next() as Boolean,
        elements.next() as String,
      )
    }
  }
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(SimpleTypes.serializer()))
}
