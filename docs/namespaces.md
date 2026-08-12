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

> Full example: [disabled namespaces](./code/example/example-namespaces-disabled-01.kt).

```typescript
export interface Widget {
  value: string;
}
```

<!--- TEST -->

References between descriptor namespaces are qualified so they remain valid
after declarations are nested:

```kotlin
@Serializable
@SerialName("org.one.Parent")
class Parent(val child: Child)

@Serializable
@SerialName("org.two.Child")
class Child(val value: String)

fun main() {
  val config = KotlinTsConfig(
    namespaceConfig = KotlinTsConfig.NamespaceConfig.DescriptorNamePrefix,
  )
  println(KotlinTsGenerator(config).generate(Parent.serializer()))
}
```

> Full example: [cross-namespace references](./code/example/example-namespaces-cross-reference-01.kt).

```typescript
export namespace org {
  export namespace one {
    export interface Parent {
      child: org.two.Child;
    }
  }

  export namespace two {
    export interface Child {
      value: string;
    }
  }
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

> Full example: [static namespace](./code/example/example-namespaces-static-01.kt).

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

> Full example: [descriptor-prefix namespaces](./code/example/example-namespaces-descriptor-prefix-01.kt).

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
