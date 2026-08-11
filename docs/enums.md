# Enums

<!--- TEST_NAME EnumClassTest -->

<!--- INCLUDE .*\.kt
import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*
-->

### Simple enum

```kotlin
@Serializable
enum class SomeType {
  Alpha,
  Beta,
  Gamma
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(SomeType.serializer()))
}
```

> You can get the full code [here](./code/example/example-enum-class-01.kt).

```typescript
export enum SomeType {
  Alpha = "Alpha",
  Beta = "Beta",
  Gamma = "Gamma",
}
```

<!--- TEST -->

### Enum with properties

Because enums are static, fields aren't converted.

```kotlin
@Serializable
enum class SomeType2(val coolName: String) {
  Alpha("alpha") {
    val extra: Long = 123L
  },
  Beta("be_beta"),
  Gamma("gamma 3 3 3")
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(SomeType2.serializer()))
}
```

> You can get the full code [here](./code/example/example-enum-class-02.kt).

```typescript
export enum SomeType2 {
  Alpha = "Alpha",
  Beta = "Beta",
  Gamma = "Gamma",
}
```

<!--- TEST -->

### Enum with serial names

`@SerialName` controls the serialized enum member value and the generated
TypeScript member name. Names used as TypeScript identifiers must be valid;
invalid names fail generation with a clear error.

```kotlin
@Serializable
enum class WireType {
  @SerialName("discussion")
  DISCUSSION,
  @SerialName("chat_room")
  CHAT_ROOM,
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(WireType.serializer()))
}
```

> You can get the full code [here](./code/example/example-enum-class-03.kt).

```typescript
export enum WireType {
  discussion = "discussion",
  chat_room = "chat_room",
}
```

<!--- TEST -->
