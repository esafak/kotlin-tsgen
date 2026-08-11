[![GitHub license](https://img.shields.io/github/license/esafak/kotlin-tsgen?style=for-the-badge)](https://github.com/esafak/kotlin-tsgen/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.esafak.kotlintsgen/kotlin-tsgen-core?style=for-the-badge&logo=apache-maven&color=6545e7&link=https%3A%2F%2Fsearch.maven.org%2Fsearch%3Fq%3Dg%3Aio.github.esafak.kotlintsgen)](https://search.maven.org/search?q=g:io.github.esafak.kotlintsgen)

# Kotlinx Serialization TypeScript Generator (kotlin-tsgen)

> [!NOTE]
> **Maintained continuation:** This repository is a fork of [Kotlinx Serialization TypeScript Generator](https://github.com/adamko-dev/kotlinx-serialization-typescript-generator) (`KxsTsGen`),
> building on its original implementation while continuing development and releases
> under the `io.github.esafak` namespace.

`kotlin-tsgen` creates TypeScript interfaces from
[kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization/)
classes, allowing for quick and easy communication via JSON with a Kotlin-first approach.

```kotlin
import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*

@Serializable
class MyClass(
  val aString: String,
  var anInt: Int,
  val aDouble: Double,
  val bool: Boolean,
  private val privateMember: String,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(MyClass.serializer()))
}
```

Generated TypeScript interface:

```typescript
export interface MyClass {
  aString: string;
  anInt: number;
  aDouble: number;
  bool: boolean;
  privateMember: string;
}
```

Only Kotlinx Serialization
[`SerialDescriptor`s](https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/-serial-descriptor/index.html)
are used to generate TypeScript.
They are flexible and comprehensive enough to allow for accurate TypeScript code, without any
surprises.

See
[the docs](https://github.com/esafak/kotlin-tsgen/tree/main/docs)
for working examples.

## Status

|                                       | Status                                                          | Notes                                                                                                          |
|---------------------------------------|-----------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------|
| Kotlin multiplatform                  | ❓                                                               | The codebase is multiplatform, but only JVM has been tested                                                    |
| `@SerialName`                         | ✅/⚠                                                             | The serial name is directly converted and might produce invalid TypeScript                                     |
| Basic classes                         | ✅   [example](./docs/basic-classes.md)                          |                                                                                                                |
| Nullable and default-value properties | ✅   [example](./docs/default-values.md)                         |                                                                                                                |
| Value classes                         | ✅   [example](./docs/value-classes.md)                          |                                                                                                                |
| Enums                                 | ✅   [example](./docs/enums.md)                                  |                                                                                                                |
| Lists                                 | ✅   [example](./docs/lists.md)                                  |                                                                                                                |
| Maps                                  | ✅/⚠ [example](./docs/maps.md)                                   | Maps with complex keys are converted to an ES6 Map, [see documentation](./docs/maps.md#maps-with-complex-keys) |
| Polymorphism - Sealed classes         | ✅/⚠ [example](./docs/polymorphism-sealed.md#sealed-classes)     | Nested sealed classes are ignored, [see documentation](./docs/polymorphism-sealed.md#nested-sealed-classes)    |
| Polymorphism - Open classes           | ❌   [example](./docs/polymorphism-open.md)                       | Not implemented. Converted to `type MyClass = any`                                                             |
| `@JsonClassDiscriminator`             | ❌                                                               | Not implemented                                                                                                |
| JSON Content polymorphism             | ❌   [example](./docs/polymorphism-open.md#json-content-polymorphism) | Not implemented. Converted to `type MyClass = any`                                                             |
| Edge cases - circular dependencies    | ✅   [example](./docs/edgecases.md)                              |                                                                                                                |
