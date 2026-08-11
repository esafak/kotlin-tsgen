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
export type UByte = number;

export type UShort = number;

export type UInt = number;

export type ULong = number;
```

This weakens the unsigned numbers, and the generated TypeScript could be used to produce
incompatible numeric values.

<!--- TEST -->

### Brand typing

To make value classes a little more strict, we can use brand typing

<!-- IMPORT -->

```kotlin
import io.github.esafak.kotlintsgen.KotlinTsConfig.TypeAliasTypingConfig.BrandTyping
import kotlinx.serialization.builtins.serializer

fun main() {

  val tsConfig = KotlinTsConfig(typeAliasTyping = BrandTyping)

  val tsGenerator = KotlinTsGenerator(config = tsConfig)
  println(
    tsGenerator.generate(
      ULong.serializer(),
    )
  )
}
```

<!-- PREFIX -->

> You can get the full code [here](./code/example/example-value-classes-03.kt).

```typescript
export type ULong = number & { __ULong__: void };
```

Now numeric types must be manually converted type aliases.

This does not mean

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

export type UInt = number;
```

<!--- TEST -->
