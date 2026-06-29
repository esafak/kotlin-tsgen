package dev.adamko.kxstsgen.core

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.modules.SerializersModule

class SerializerDescriptorsExtractorTest : FunSpec({

  val module = SerializersModule { }
  val extractor = SerializerDescriptorsExtractor.default(module)

  test("Example1: given parent class, expect subclass property descriptor extracted") {

    val expected = listOf(
      Example1.Parent.serializer().descriptor,
      Example1.Nested.serializer().descriptor,
      String.serializer().descriptor,
    )

    val actual = extractor(Example1.Parent.serializer())

    actual shouldContainDescriptors expected
  }

  test("Example2: given parent class, expect subclass property descriptor extracted") {

    val expected = listOf(
      Example2.Parent.serializer().descriptor,
      Example2.Nested.serializer().descriptor,
      String.serializer().descriptor,
    )

    val actual = extractor(Example2.Parent.serializer())

    actual shouldContainDescriptors expected
  }

  test("Example3: expect nullable/non-nullable SerialDescriptors be de-duplicated") {

    val expected = listOf(
      Example3.SomeType.serializer().descriptor,
      Example3.TypeHolder.serializer().descriptor,
      String.serializer().descriptor,
    )

    val actual = extractor(Example3.TypeHolder.serializer())

    actual shouldContainDescriptors expected
  }

  test("Example4: contextual serializer is extracted from SerializersModule") {
    val module = SerializersModule {
      contextual(Example4.SomeType::class, Example4.SomeType.serializer())
    }
    val extractor = SerializerDescriptorsExtractor.default(module)

    val actual = extractor(Example4.TypeHolder.serializer())

    val someTypeDescriptor = Example4.SomeType.serializer().descriptor
    withClue("Should contain SomeType descriptor from module") {
      actual.any { it.serialName == someTypeDescriptor.serialName } shouldBe true
    }
  }

  test("Example4b: contextual placeholder is suppressed when resolvable from the module") {
    val module = SerializersModule {
      contextual(Example4.SomeType::class, Example4.SomeType.serializer())
    }
    val extractor = SerializerDescriptorsExtractor.default(module)

    val actual = extractor(Example4.TypeHolder.serializer())

    // the resolved SomeType descriptor must be present...
    val someTypeDescriptor = Example4.SomeType.serializer().descriptor
    withClue("resolved SomeType descriptor should be present") {
      actual.any { it.serialName == someTypeDescriptor.serialName } shouldBe true
    }
    // ...and the contextual placeholder must NOT survive into the extracted set - otherwise it
    // renders as `type SomeType = any`, colliding with the resolved `interface SomeType`
    // (TS2300 duplicate identifier).
    withClue("no contextual placeholder should remain") {
      actual.none { it.kind == SerialKind.CONTEXTUAL } shouldBe true
    }
  }

  test("Example4c: contextual resolution matches by name boundary, not a loose suffix") {
    val module = SerializersModule {
      contextual(Example4c.Foo::class, Example4c.Foo.serializer())
      contextual(Example4c.BarFoo::class, Example4c.BarFoo.serializer())
    }
    val extractor = SerializerDescriptorsExtractor.default(module)

    val actual = extractor(Example4c.Holder.serializer())

    val fooDescriptor = Example4c.Foo.serializer().descriptor
    val barFooDescriptor = Example4c.BarFoo.serializer().descriptor

    // A `@Contextual Foo` must resolve to Foo only, and must NOT also pick up the unrelated
    // BarFoo, whose simple name merely ends in "Foo".
    withClue("Should contain Foo") {
      actual.any { it.serialName == fooDescriptor.serialName } shouldBe true
    }
    withClue("Should NOT contain BarFoo") {
      actual.any { it.serialName == barFooDescriptor.serialName } shouldBe false
    }
  }

  test("Example6: contextual without module registration does not extract descriptor") {
    val emptyModule = SerializersModule { }
    val extractor = SerializerDescriptorsExtractor.default(emptyModule)

    val actual = extractor(Example4.TypeHolder.serializer())

    val someTypeDescriptor = Example4.SomeType.serializer().descriptor
    withClue("Should NOT contain SomeType descriptor when not registered") {
      actual.any { it.serialName == someTypeDescriptor.serialName } shouldBe false
    }
  }

  }) {
  companion object {
    private infix fun Collection<SerialDescriptor>.shouldContainDescriptors(expected: Collection<SerialDescriptor>) {
      val actual = this
      withClue(
        """
          expected: ${expected.map { it.serialName }.sorted().joinToString()}
          actual:   ${actual.map { it.serialName }.sorted().joinToString()}
        """.trimIndent()
      ) {
        actual shouldContainExactlyInAnyOrder expected
      }
    }
  }
}


@Suppress("unused")
private object Example1 {
  @Serializable
  class Nested(val x: String)

  @Serializable
  sealed class Parent

  @Serializable
  class SubClass(val n: Nested) : Parent()
}


@Suppress("unused")
private object Example2 {
  @Serializable
  class Nested(val x: String)

  @Serializable
  sealed class Parent

  @Serializable
  sealed class SealedSub : Parent()

  @Serializable
  class SubClass1(val n: Nested) : SealedSub()
}


@Suppress("unused")
private object Example3 {

  @Serializable
  class SomeType(val a: String)

  @Serializable
  class TypeHolder(
    val required: SomeType,
    val optional: SomeType?,
  )
}


@Suppress("unused")
private object Example4 {

  @Serializable
  class SomeType(val a: String)

  @Serializable
  class TypeHolder(
    @kotlinx.serialization.Contextual
    val required: SomeType,
  )
}


@Suppress("unused")
private object Example4c {

  @Serializable
  class Foo(val a: String)

  @Serializable
  class BarFoo(val b: String)

  @Serializable
  class Holder(
    @kotlinx.serialization.Contextual
    val x: Foo,
  )
}
