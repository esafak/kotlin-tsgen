# Basic classes

<!--- TEST_NAME BasicClassesTest -->
<!--- INCLUDE .*\.kt
import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*
-->

### Plain class with a single field

```kotlin
@Serializable
class Color(val rgb: Int)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Color.serializer()))
}
```

> You can get the full code [here](./code/example/example-plain-class-single-field-01.kt).

```typescript
export interface Color {
  rgb: Int;
}

export type Int = number;
```

<!--- TEST -->

### Plain class with primitive fields

```kotlin
@Serializable
class SimpleTypes(
  val aString: String,
  var anInt: Int,
  val aDouble: Double,
  val bool: Boolean,
  private val privateMember: String,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(SimpleTypes.serializer()))
}
```

> You can get the full code [here](./code/example/example-plain-class-primitive-fields-01.kt).

```typescript
export interface SimpleTypes {
  aString: string;
  anInt: Int;
  aDouble: Double;
  bool: boolean;
  privateMember: string;
}

export type Int = number;

export type Double = number;
```

<!--- TEST -->

### Data class with primitive fields

```kotlin
@Serializable
data class SomeDataClass(
  val aString: String,
  var anInt: Int,
  val aDouble: Double,
  val bool: Boolean,
  private val privateMember: String,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(SomeDataClass.serializer()))
}
```

> You can get the full code [here](./code/example/example-plain-data-class-01.kt).

```typescript
export interface SomeDataClass {
  aString: string;
  anInt: Int;
  aDouble: Double;
  bool: boolean;
  privateMember: string;
}

export type Int = number;

export type Double = number;
```

<!--- TEST -->
