# Namespaces

<!--- TEST_NAME NamespacesTest -->
<!--- INCLUDE .*\.kt
import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*
-->

Namespace rendering is disabled by default, so generated declarations remain
top-level:

```kotlin
@Serializable
class Widget(val value: String)

fun main() {
  println(KotlinTsGenerator().generate(Widget.serializer()))
}
```

> You can get the full code [here](./code/example/example-namespaces-disabled-01.kt).

```typescript
export interface Widget {
  value: string;
}
```

<!--- TEST -->

Use `NamespaceConfig.Static` to place every generated declaration in one
namespace:

```kotlin
@Serializable
class Widget(val value: String)

fun main() {
  val config = KotlinTsConfig(
    namespaceConfig = KotlinTsConfig.NamespaceConfig.Static("models"),
  )
  println(KotlinTsGenerator(config).generate(Widget.serializer()))
}
```

> You can get the full code [here](./code/example/example-namespaces-static-01.kt).

```typescript
export namespace models {
  export interface Widget {
    value: string;
  }
}
```

<!--- TEST -->

`DescriptorNamePrefix` converts dotted serial-name prefixes into nested
namespaces:

```kotlin
@Serializable
@SerialName("org.example.Widget")
class Widget(val value: String)

fun main() {
  val config = KotlinTsConfig(
    namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
  )
  println(KotlinTsGenerator(config).generate(Widget.serializer()))
}
```

> You can get the full code [here](./code/example/example-namespaces-descriptor-prefix-01.kt).

```typescript
export namespace org {
  export namespace example {
    export interface Widget {
      value: string;
    }
  }
}
```

<!--- TEST -->
