package io.github.esafak.kotlintsgen.core

import kotlinx.serialization.SerialInfo

/**
 * Brands the annotated inline value class in TypeScript output.
 *
 * ```kotlin
 * @TsBrand
 * @Serializable
 * @JvmInline
 * value class UserId(val value: String)
 * // -> export type UserId = string & { readonly __brand: "UserId" };
 * ```
 *
 * Only the alias declaration changes: every use of the type is emitted by name,
 * so fields, lists and map values pick the brand up unchanged.
 * Map keys are still unwrapped to `string`/`number` for index signatures.
 *
 * Limits:
 * - Kotlin `typealias` declarations are invisible to descriptors, so they cannot be branded.
 * Use `@JvmInline value class` + `@TsBrand`, or `mapTypes { X.serializer() mapsTo branded(...) }`.
 * - Custom serializers using `PrimitiveSerialDescriptor` do not carry class annotations.
 * Brand those via `branded()`.
 * - The default brand is the short TypeScript name. If two namespaces both emit `Id`,
 * their default brands are structurally identical. Give explicit distinct names
 * (`@TsBrand("com.foo.Id")`) when that matters.
 * - Do not brand both an outer value class and the value class it wraps: the intersection
 * of two `__brand` literals is `never`. Brand only the leaf, or leave the outer unbranded.
 *
 * @param[name] Brand literal. Empty (the default) uses the generated TypeScript type name.
 */
@SerialInfo
@Target(AnnotationTarget.CLASS)
annotation class TsBrand(val name: String = "")
