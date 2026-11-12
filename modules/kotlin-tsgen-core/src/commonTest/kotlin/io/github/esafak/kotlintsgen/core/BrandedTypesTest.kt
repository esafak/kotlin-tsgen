package io.github.esafak.kotlintsgen.core

import io.github.esafak.kotlintsgen.KotlinTsGenerator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlin.jvm.JvmInline

class BrandedTypesTest :
  FunSpec({
    test("TsBrand emits a branded alias and usages stay by-name") {
      KotlinTsGenerator().generate(Membership.serializer()) shouldBe
        """
        |export interface Membership {
        |  user: UserId;
        |  group: UserGroupId;
        |}
        |
        |export type UserId = string & { readonly __brand: "UserId" };
        |
        |export type UserGroupId = string & { readonly __brand: "UserGroupId" };
        """.trimMargin()
    }

    test("unbranded value classes stay plain aliases") {
      val ts = KotlinTsGenerator().generate(Membership.serializer())
      ts shouldContain "export type UserId = string & { readonly __brand: \"UserId\" };"
      KotlinTsGenerator().generate(PlainToken.serializer()) shouldBe
        "export type PlainToken = string;"
    }

    test("custom brand name is honored") {
      KotlinTsGenerator().generate(CustomBranded.serializer()) shouldBe
        "export type CustomBranded = string & { readonly __brand: \"Custom\" };"
    }

    test("branded() builder works for types without annotations") {
      val generator = KotlinTsGenerator()
      generator.mapTypes {
        PlainToken.serializer() mapsTo branded("PlainToken", "string")
      }
      generator.generate(PlainToken.serializer()) shouldBe
        "export type PlainToken = string & { readonly __brand: \"PlainToken\" };"
    }

    test("branding does not leak to unrelated aliases") {
      val ts = KotlinTsGenerator().generate(Membership.serializer())
      ts shouldNotContain "__brand: \"Membership\""
      ts shouldNotContain "export type string"
    }

    test("branded aliases work in lists") {
      KotlinTsGenerator().generate(Team.serializer()) shouldContain "members: UserId[];"
    }

    test("nullable branded alias keeps null outside the intersection") {
      KotlinTsGenerator().generate(NullableId.serializer()) shouldBe
        "export type NullableId = string & { readonly __brand: \"NullableId\" } | null;"

      val generator = KotlinTsGenerator()
      generator.mapTypes {
        PlainToken.serializer() mapsTo branded("PlainToken", ref("string", nullable = true))
      }
      generator.generate(PlainToken.serializer()) shouldBe
        "export type PlainToken = string & { readonly __brand: \"PlainToken\" } | null;"
    }

    test("brand literals are escaped") {
      val generator = KotlinTsGenerator()
      generator.mapTypes {
        PlainToken.serializer() mapsTo branded("PlainToken", "string", "a\"b\\c")
      }
      generator.generate(PlainToken.serializer()) shouldBe
        "export type PlainToken = string & { readonly __brand: \"a\\\"b\\\\c\" };"
    }

    test("nested branded value classes fail with a diagnostic") {
      shouldThrow<InvalidTsIdentifierException> {
        KotlinTsGenerator().generate(OuterBranded.serializer())
      }
    }

    test("transitively nested branded value classes fail") {
      shouldThrow<InvalidTsIdentifierException> {
        KotlinTsGenerator().generate(OuterViaMiddle.serializer())
      }
    }

    test("union targets are parenthesized when branded") {
      val generator = KotlinTsGenerator()
      generator.mapTypes {
        PlainToken.serializer() mapsTo branded("PlainToken", custom("string | number"))
      }
      generator.generate(PlainToken.serializer()) shouldBe
        "export type PlainToken = (string | number) & { readonly __brand: \"PlainToken\" };"
    }

    test("custom branded mapping is applied inside lists") {
      val generator = KotlinTsGenerator()
      generator.mapTypes {
        AccountId.serializer() mapsTo branded("AccountId", "string")
      }

      generator.generate(Request.serializer()) shouldContain "accountIds: AccountId[];"
    }

    test("custom branded mapping is applied to map values") {
      val generator = KotlinTsGenerator()
      generator.mapTypes {
        AccountId.serializer() mapsTo branded("AccountId", "string")
      }

      generator.generate(AccountMapValueHolder.serializer()) shouldContain
        "{ [key: string]: AccountId }"
    }

    test("custom branded string map keys preserve object shape") {
      val generator = KotlinTsGenerator()
      generator.mapTypes {
        AccountId.serializer() mapsTo branded("AccountId", "string")
      }

      val ts = generator.generate(AccountMapKeyHolder.serializer())
      ts shouldContain "values: { [key: string]: string };"
      ts shouldContain "export type AccountId = string & { readonly __brand: \"AccountId\" };"
    }

    test("Default trailing-lambda still binds to index-signature override") {
      val converter = TsTypeRefConverter.Default(
        TsElementIdConverter.Default,
        TsMapTypeConverter.Default,
        EmptySerializersModule(),
        null,
      ) { null }

      converter.indexSignatureKeyTypeRefOverride shouldNotBe null
      converter.nestedTypeRefConverterProvider shouldBe null
    }
  })

@TsBrand
@Serializable
@JvmInline
private value class UserId(val value: String)

@TsBrand
@Serializable
@JvmInline
private value class UserGroupId(val value: String)

@Serializable
private data class Membership(val user: UserId, val group: UserGroupId)

@Serializable
@JvmInline
private value class PlainToken(val value: String)

@TsBrand("Custom")
@Serializable
@JvmInline
private value class CustomBranded(val value: String)

@Serializable
private data class Team(val members: List<UserId>)

@TsBrand
@Serializable
@JvmInline
private value class NullableId(val value: String?)

@TsBrand
@Serializable
@JvmInline
private value class InnerBranded(val value: String)

@TsBrand
@Serializable
@JvmInline
private value class OuterBranded(val inner: InnerBranded)

@Serializable
@JvmInline
private value class MiddleUnbranded(val inner: InnerBranded)

@TsBrand
@Serializable
@JvmInline
private value class OuterViaMiddle(val middle: MiddleUnbranded)

@Serializable(with = AccountIdSerializer::class)
@JvmInline
private value class AccountId(val value: Long)

private object AccountIdSerializer : KSerializer<AccountId> {
  override val descriptor =
    PrimitiveSerialDescriptor("AccountId", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: AccountId) =
    encoder.encodeString(value.value.toString())

  override fun deserialize(decoder: Decoder) = AccountId(decoder.decodeString().toLong())
}

@Serializable
private data class Request(val accountIds: List<AccountId>)

@Serializable
private data class AccountMapValueHolder(val values: Map<String, AccountId>)

@Serializable
private data class AccountMapKeyHolder(val values: Map<AccountId, String>)
