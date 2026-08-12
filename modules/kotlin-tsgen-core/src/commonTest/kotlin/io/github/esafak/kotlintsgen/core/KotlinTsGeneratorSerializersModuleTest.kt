package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsConfig
import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.jvm.JvmInline
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

/**
 * End-to-end tests for [KotlinTsGenerator] driven by a [SerializersModule].
 *
 * These assert on the *generated TypeScript* (not just the extracted descriptor set) so that
 * regressions like a `type Foo = any` placeholder colliding with a resolved `interface Foo`
 * (TS2300 duplicate identifier) are caught.
 */
@OptIn(ExperimentalSerializationApi::class)
class KotlinTsGeneratorSerializersModuleTest : FunSpec({

  test("contextual - a registered type is emitted as an interface, with no duplicate type-alias") {
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

  test("contextual - an unregistered type falls back to `type Foo = any`") {
      val ts = KotlinTsGenerator()
        .generate(ContextualExample.TypeHolder.serializer())

      // no dangling reference: the placeholder becomes a type-alias to `any`...
      ts shouldContain "type SomeType = any"
      // ...and no interface is invented
      ts shouldNotContain "export interface SomeType {"
  }

  test("contextual - a generic provider that needs type arguments falls back without throwing") {
      val module = SerializersModule {
        contextual(GenericContextualExample.Box::class) { typeArguments ->
          GenericContextualExample.Box.serializer(typeArguments.single())
        }
      }

      val ts = KotlinTsGenerator(serializersModule = module)
        .generate(GenericContextualExample.Holder.serializer())

      ts shouldContain "type Box = any"
  }

  test("contextual - a primitive serializer is rendered using a named type") {
      val module = SerializersModule {
        contextual(PrimitiveContextualExample.EntityType::class, PrimitiveContextualExample.EntityTypeSerializer)
      }

      val ts = KotlinTsGenerator(serializersModule = module)
        .generate(PrimitiveContextualExample.Holder.serializer())

       ts shouldContain "required: EntityType;"
       ts shouldContain "optional: EntityType | null;"
       ts shouldContain "export type EntityType = string;"
  }

  test("custom primitive serializer - a root enum is emitted as a named type alias") {
      val ts = KotlinTsGenerator()
        .generate(PrimitiveContextualExample.EntityType.serializer())

      ts shouldBe "export type EntityType = string;"
  }

  test("custom primitive serializer - a root value class is emitted as a named type alias") {
      val ts = KotlinTsGenerator()
        .generate(CustomPrimitiveExample.StringId.serializer())

      ts shouldBe "export type StringId = string;"
  }

  test("contextual primitive serializer - a resolved root type is emitted as a named type alias") {
      val module = SerializersModule {
        contextual(PrimitiveContextualExample.EntityType::class, PrimitiveContextualExample.EntityTypeSerializer)
      }

      val ts = KotlinTsGenerator(serializersModule = module)
        .generate(ContextualSerializer(PrimitiveContextualExample.EntityType::class))

      ts shouldBe "export type EntityType = string;"
  }

  test("open polymorphic - registered subclasses are emitted as a union") {
      val module = SerializersModule {
        polymorphic(OpenExample.Parent::class, OpenExample.SubClass::class, OpenExample.SubClass.serializer())
        polymorphic(OpenExample.OtherParent::class, OpenExample.OtherSubClass::class, OpenExample.OtherSubClass.serializer())
      }

      val ts = KotlinTsGenerator(serializersModule = module)
        .generate(OpenExample.TypeHolder.serializer())

      ts shouldContain "export type Parent ="
      ts shouldContain "| SubClass;"
      ts shouldContain "export interface SubClass {"
      ts shouldNotContain "OtherSubClass"
      ts shouldNotContain "type Parent = any"
  }

  test("open polymorphic - the config module is also used for resolution") {
      val module = SerializersModule {
        polymorphic(OpenExample.Parent::class, OpenExample.SubClass::class, OpenExample.SubClass.serializer())
      }

      val ts = KotlinTsGenerator(KotlinTsConfig(serializersModule = module))
        .generate(OpenExample.TypeHolder.serializer())

      ts shouldContain "| SubClass;"
      ts shouldContain "export interface SubClass {"
      ts shouldNotContain "type Parent = any"
  }

  test("open polymorphic - without registrations falls back to any") {
      val ts = KotlinTsGenerator()
        .generate(OpenExample.TypeHolder.serializer())

      ts shouldContain "type Parent = any"
      ts shouldNotContain "export interface SubClass {"
  }

  test("sealed polymorphic - module registrations do not duplicate subclasses") {
      val module = SerializersModule {
        polymorphic(SealedExample.Parent::class, SealedExample.SubClass::class, SealedExample.SubClass.serializer())
      }

      val ts = KotlinTsGenerator(serializersModule = module)
        .generate(SealedExample.Parent.serializer())
      val tsWithoutModule = KotlinTsGenerator()
        .generate(SealedExample.Parent.serializer())

      ts shouldBe tsWithoutModule

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

  test("sealed polymorphic - the parent discriminator annotation selects the property and enum") {
    val ts = KotlinTsGenerator().generate(AnnotatedSealedExample.Parent.serializer())

    ts shouldContain "export enum Kind {"
    ts shouldContain "kind: Parent.Kind.Child;"
    ts shouldNotContain "type: Parent.Kind.Child;"
  }

  test("sealed polymorphic - a subclass carrying the parent discriminator does not disrupt generation") {
    val ts = KotlinTsGenerator().generate(AnnotatedSealedExample.Parent.serializer())

    ts shouldContain "kind: Parent.Kind.Child;"
  }

  test("sealed polymorphic - nested sealed subclasses are flattened recursively") {
    val ts = KotlinTsGenerator().generate(NestedSealedExample.Parent.serializer())

    ts shouldContain "export type Parent ="
    ts shouldContain "| Parent.Direct"
    ts shouldContain "| Parent.Leaf"
    ts shouldContain "| Parent.Sibling"
    ts shouldContain "export interface Leaf {"
    ts shouldContain "export interface Sibling {"
    ts shouldContain "name: string;"
    ts shouldContain "colour: string;"
    ts shouldContain "leaf: string;"
    ts shouldContain "Sibling ="
    ts shouldContain "Leaf ="
    ts shouldNotContain "| Parent.Middle"
    ts shouldNotContain "| Parent.Inner"
  }

  test("sealed polymorphic - an invalid discriminator name fails clearly") {
    val exception = shouldThrow<InvalidTsIdentifierException> {
      KotlinTsGenerator().generate(InvalidDiscriminatorExample.Parent.serializer())
    }

    exception.identifier shouldBe "bad-name"
    exception.context shouldContain "discriminator property"
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

  @Suppress("unused")
  private object AnnotatedSealedExample {
    @Serializable
    @JsonClassDiscriminator("kind")
    sealed class Parent

    @Serializable
    @JsonClassDiscriminator("kind")
    class Child : Parent()
  }

  @Suppress("unused")
  private object NestedSealedExample {
    @Serializable
    sealed class Parent {
      abstract val name: String

      @Serializable
      class Direct(override val name: String, val direct: String) : Parent()

      @Serializable
      sealed class Middle : Parent() {
        abstract val colour: String

        @Serializable
        class Sibling(
          override val name: String,
          override val colour: String,
          val sibling: String,
        ) : Middle()

        @Serializable
        sealed class Inner : Middle() {
          @Serializable
          class Leaf(
            override val name: String,
            override val colour: String,
            val leaf: String,
          ) : Inner()
        }
      }
    }
  }

  @Suppress("unused")
  private object InvalidDiscriminatorExample {
    @Serializable
    @JsonClassDiscriminator("bad-name")
    sealed class Parent

    @Serializable
    class Child : Parent()
  }

  @Suppress("unused")
  private object OpenExample {
    @Serializable
    abstract class Parent

    @Serializable
    class SubClass(val x: String) : Parent()

    @Serializable
    abstract class OtherParent

    @Serializable
    class OtherSubClass(val y: String) : OtherParent()

    @Serializable
    class TypeHolder(
      @kotlinx.serialization.Polymorphic
      val value: Parent,
    )
  }

  @Suppress("unused")
  private object GenericContextualExample {
    @Serializable
    class Box<T>(val value: T)

    @Serializable
    class Holder(
      @kotlinx.serialization.Contextual
      val value: Box<String>,
    )
  }

  @Suppress("unused")
  private object PrimitiveContextualExample {
    @Serializable(with = EntityTypeSerializer::class)
    enum class EntityType {
      DISCUSSION,
      CHAT_ROOM,
    }

    object EntityTypeSerializer : KSerializer<EntityType> {
      override val descriptor = PrimitiveSerialDescriptor("EntityType", PrimitiveKind.STRING)

      override fun serialize(encoder: Encoder, value: EntityType) {
        encoder.encodeString(value.name)
      }

      override fun deserialize(decoder: Decoder): EntityType =
        EntityType.valueOf(decoder.decodeString())
    }

    @Serializable
    class Holder(
      @kotlinx.serialization.Contextual
      val required: EntityType,
      @kotlinx.serialization.Contextual
      val optional: EntityType?,
    )
  }

  @Suppress("unused")
  private object CustomPrimitiveExample {
    @Serializable(with = StringIdSerializer::class)
    @JvmInline
    value class StringId(val value: String)

    object StringIdSerializer : KSerializer<StringId> {
      override val descriptor = PrimitiveSerialDescriptor("StringId", PrimitiveKind.STRING)

      override fun serialize(encoder: Encoder, value: StringId) {
        encoder.encodeString(value.value)
      }

      override fun deserialize(decoder: Decoder): StringId =
        StringId(decoder.decodeString())
    }
  }
}
