// This file was automatically generated from tuples.md by Knit tool. Do not edit.
@file:Suppress("PackageDirectoryMismatch", "unused")
package io.github.esafak.kotlintsgen.example.exampleTuple03

import io.github.esafak.kotlintsgen.*
import io.github.esafak.kotlintsgen.core.experiments.TupleSerializer
import kotlinx.serialization.*

@Serializable(with = OptionalFields.Serializer::class)
data class OptionalFields(
  val requiredString: String,
  val nullableString: String?,
  val optionalString: String = "",
  val nullableOptionalString: String? = "",
) {
  object Serializer : TupleSerializer<OptionalFields>(
    "OptionalFields",
    {
      element(OptionalFields::requiredString)
      element(OptionalFields::nullableString)
      element(OptionalFields::optionalString, isOptional = true)
      element(OptionalFields::nullableOptionalString, isOptional = true)
    }
  ) {
    override fun tupleConstructor(elements: Iterator<*>): OptionalFields {
      val iter = elements.iterator()
      return OptionalFields(
        iter.next() as String,
        iter.next() as String?,
        iter.next() as String,
        iter.next() as String?,
      )
    }
  }
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(OptionalFields.serializer()))
}
