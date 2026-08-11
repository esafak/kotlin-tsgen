package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

/**
 * End-to-end tests for [KotlinTsGenerator] driven by a [SerializersModule].
 *
 * These assert on the *generated TypeScript* (not just the extracted descriptor set) so that
 * regressions like a `type Foo = any` placeholder colliding with a resolved `interface Foo`
 * (TS2300 duplicate identifier) are caught.
 */
class KotlinTsGeneratorSerializersModuleTest : FunSpec({

  context("contextual serializers") {

    test("a contextual type registered in the module is emitted as an interface, with no duplicate type-alias") {
      val module = SerializersModule {
        contextual(ContextualExample.SomeType::class, ContextualExample.SomeType.serializer())
      }

      val ts = KotlinTsGenerator(serializersModule = module)
        .generate(ContextualExample.TypeHolder.serializer())

      // the resolved type is generated as an interface...
      ts shouldContain "export interface SomeType {"
      // ...and the contextual placeholder must NOT also be rendered as `type SomeType = any`
      // (that would collide with the interface and produce a TS2300 duplicate identifier).
      ts shouldNotContain "type SomeType = any"
      // the referencing field must point at the resolved type
      ts shouldContain "required: SomeType;"

      ts shouldBe """
        |export interface TypeHolder {
        |  required: SomeType;
        |}
        |
        |export interface SomeType {
        |  a: string;
        |}
      """.trimMargin()
    }

    test("a contextual type NOT registered in the module falls back to `type Foo = any`") {
      val ts = KotlinTsGenerator()
        .generate(ContextualExample.TypeHolder.serializer())

      // no dangling reference: the placeholder becomes a type-alias to `any`...
      ts shouldContain "type SomeType = any"
      // ...and no interface is invented
      ts shouldNotContain "export interface SomeType {"
    }
  }

  context("polymorphic serializers") {

    test("a sealed hierarchy is rendered as a discriminated namespace, with no flat duplicate subclass") {
      val module = SerializersModule {
        polymorphic(SealedExample.Parent::class, SealedExample.SubClass::class, SealedExample.SubClass.serializer())
      }

      val ts = KotlinTsGenerator(serializersModule = module)
        .generate(SealedExample.Parent.serializer())

      // the discriminated namespace, discriminator enum and union are all present...
      ts shouldContain "export namespace Parent {"
      ts shouldContain "export enum Type {"
      ts shouldContain "| Parent.SubClass"
      // ...the subclass lives (correctly) inside the namespace...
      ts shouldContain "  export interface SubClass {"

      // ...and there must be NO flat top-level duplicate of the subclass interface.
      // (A line starting with `export interface SubClass` at column 0 would be such a duplicate.)
      ts.lineSequence()
        .filter { it.startsWith("export interface SubClass") }
        .toList()
        .shouldBeEmpty()
    }
  }
}) {
  @Suppress("unused")
  private object ContextualExample {
    @Serializable
    class SomeType(val a: String)

    @Serializable
    class TypeHolder(
      @kotlinx.serialization.Contextual
      val required: SomeType,
    )
  }

  @Suppress("unused")
  private object SealedExample {
    @Serializable
    sealed class Parent

    @Serializable
    class SubClass(val x: String) : Parent()
  }
}
