# Inline value classes

<!--- TEST_NAME ValueClassesTest -->
<!--- INCLUDE .*\.kt
import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*
-->

Value classes are transformed to type aliases. The type of the value class is used.

```kotlin
@Serializable
@JvmInline
value class AuthToken(private val token: String)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(AuthToken.serializer()))
}
```

> You can get the full code [here](./code/example/example-value-classes-01.kt).

```typescript
export type AuthToken = string;
```

<!--- TEST -->


Value classes from other libraries are also transformed to their inner types. For example, Kotlin
unsigned numbers.

```kotlin
import kotlinx.serialization.builtins.serializer

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(
    tsGenerator.generate(
      UByte.serializer(),
      UShort.serializer(),
      UInt.serializer(),
      ULong.serializer(),
    )
  )
}
```

<!-- PREFIX -->

> You can get the full code [here](./code/example/example-value-classes-02.kt).

```typescript
export type UByte = Byte;

export type UShort = Short;

export type UInt = Int;

export type ULong = Long;

export type Byte = number;

export type Short = number;

export type Int = number;

export type Long = number;
```

This weakens the unsigned numbers, and the generated TypeScript could be used to produce
incompatible numeric values.

<!--- TEST -->

### Brand typing

To keep nominal IDs nominal in TypeScript, brand individual value classes with `@TsBrand`.
Only the alias declaration changes: every use of the type is emitted by name, so fields, lists
and map values pick the brand up unchanged. (Map keys are still unwrapped to `string`/`number`
for index signatures.)

<!-- IMPORT -->

```kotlin
import io.github.esafak.kotlintsgen.core.TsBrand

@TsBrand
@Serializable
@JvmInline
value class UserId(val value: String)

@TsBrand
@Serializable
@JvmInline
value class UserGroupId(val value: String)

@Serializable
data class Membership(val user: UserId, val group: UserGroupId)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Membership.serializer()))
}
```

<!-- PREFIX -->

> You can get the full code [here](./code/example/example-value-classes-03.kt).

```typescript
export interface Membership {
  user: UserId;
  group: UserGroupId;
}

export type UserId = string & { readonly __brand: "UserId" };

export type UserGroupId = string & { readonly __brand: "UserGroupId" };
```

Use `@TsBrand("Custom")` to override the brand literal, or `mapTypes { UserId.serializer() mapsTo branded("UserId", "string") }`
for types you don't own (e.g. library primitives with a custom serializer).

<!--- TEST -->

### Nested value classes

If the value class contains another value class, then the outer class will be aliased to other value
class

```kotlin
@Serializable
@JvmInline
value class UserCount(private val count: UInt)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(UserCount.serializer()))
}
```

> You can get the full code [here](./code/example/example-value-classes-04.kt).

```typescript
export type UserCount = UInt;

export type UInt = Int;

export type Int = number;
```

<!--- TEST -->
