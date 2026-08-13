# Open Polymorphism

Support for abstract classes is limited, as there is no concrete type. If a class must be
polymorphic, instead prefer [sealed polymorphism](./polymorphism-sealed.md).

<!--- TEST_NAME PolymorphismOpenTest -->
<!--- INCLUDE .*\.kt
import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*
-->

### Abstract class with a single field

```kotlin
@Serializable
abstract class Color(val rgb: Int)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Color.serializer()))
}
```

> You can get the full code [here](./code/example/example-abstract-class-single-field-01.kt).

```typescript
export type Color = any;
// interface Color {
//   rgb: number;
// }
```

<!--- TEST -->

### Abstract class with primitive fields

```kotlin
@Serializable
abstract class SimpleTypes(
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

> You can get the full code [here](./code/example/example-abstract-class-primitive-fields-01.kt).

```typescript
export type SimpleTypes = any;
// export interface SimpleTypes {
//   aString: string;
//   anInt: number;
//   aDouble: number;
//   bool: boolean;
//   privateMember: string;
// }
```

<!--- TEST -->

### Abstract class, abstract value

```kotlin
@Serializable
abstract class AbstractSimpleTypes {
  abstract val aString: String
  abstract var anInt: Int
  abstract val aDouble: Double
  abstract val bool: Boolean
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(AbstractSimpleTypes.serializer()))
}
```

> You can get the full code [here](./code/example/example-abstract-class-abstract-field-01.kt).

```typescript
export type AbstractSimpleTypes = any;
// export interface AbstractSimpleTypes {
//   rgb: number;
// }
```

<!--- TEST -->

## Open Polymorphism

### Generics

Kotlinx Serialization doesn't have 'generic' SerialDescriptors, so KotlinTsGen can't generate generic
TypeScript classes.

```kotlin
import kotlinx.serialization.builtins.serializer

@Serializable
class Box<T : Number>(
  val value: T,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()

  println(
    tsGenerator.generate(
      Box.serializer(Double.serializer()),
    )
  )
}
```

> You can get the full code [here](./code/example/example-generics-01.kt).

```typescript
export interface Box {
  value: Double;
}

export type Double = number;
```

<!--- TEST -->

### JSON content polymorphism

Using a
[`JsonContentPolymorphicSerializer`](https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/index.html)
means there's not enough data in the `SerialDescriptor` to generate a TypeScript interface. Instead,
a named TypeScript union can be configured explicitly because the possible serializers are hidden in
the custom `selectDeserializer` implementation.

```kotlin
import kotlinx.serialization.json.*

@Serializable
abstract class Project {
  abstract val name: String
}

@Serializable
data class BasicProject(override val name: String) : Project()

@Serializable
data class OwnedProject(override val name: String, val owner: String) : Project()

object ProjectSerializer : JsonContentPolymorphicSerializer<Project>(Project::class) {
  override fun selectDeserializer(element: JsonElement) = when {
    "owner" in element.jsonObject -> OwnedProject.serializer()
    else                          -> BasicProject.serializer()
  }
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  tsGenerator.mapTypes {
    contentPolymorphism(ProjectSerializer) {
      subtype<BasicProject>()
      subtype<OwnedProject>()
    }
  }
  println(tsGenerator.generate(ProjectSerializer))
}
```

> You can get the full code in [`example-json-polymorphic-01.kt`](./code/example/example-json-polymorphic-01.kt).

```typescript
export type Project =
  | BasicProject
  | OwnedProject;

export interface BasicProject {
  name: string;
}

export interface OwnedProject {
  name: string;
  owner: string;
}
```

<!--- TEST -->
