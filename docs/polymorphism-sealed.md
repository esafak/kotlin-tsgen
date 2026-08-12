# Closed Polymorphism

<!--- TEST_NAME PolymorphismSealedTest -->
<!--- INCLUDE .*\.kt
import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*
-->

https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#closed-polymorphism

### Static types

```kotlin
@Serializable
open class Project(val name: String)

class OwnedProject(name: String, val owner: String) : Project(name)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Project.serializer()))
}
```

> You can get the full code [here](./code/example/example-polymorphic-static-types-01.kt).

Since `OwnedProject` is not `@Serializable`, only the properties of `Project` generated.

```typescript
export interface Project {
  name: string;
}
```

<!--- TEST -->

```kotlin
import kotlinx.serialization.modules.*

@Serializable
abstract class Project {
  abstract val name: String
}

@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

val module = SerializersModule {
  polymorphic(Project::class) {
    subclass(OwnedProject::class)
  }
}

fun main() {
  val config = KotlinTsConfig(serializersModule = module)

  val tsGenerator = KotlinTsGenerator(config)

  println(tsGenerator.generate(Project.serializer()))
}
```

> You can get the full code [here](./code/example/example-polymorphic-static-types-02.kt).

```typescript
export type Project =
  | OwnedProject;

export interface OwnedProject {
  name: string;
  owner: string;
}
```

<!--- TEST -->

### Sealed classes

Sealed classes are the best way to generate TypeScript interface so far, because all subclasses are
defined in the `SerialDescriptor`.

A sealed class will be converted as a
[union enum, with enum member types](https://www.typescriptlang.org/docs/handbook/enums.html#union-enums-and-enum-member-types)
.

This has many benefits that closely match how sealed classes work in Kotlin.

```kotlin
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("kind")
sealed class Project {
  abstract val name: String
}

@Serializable
@SerialName("OProj")
class OwnedProject(override val name: String, val owner: String) : Project()

@Serializable
class DeprecatedProject(override val name: String, val reason: String) : Project()

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Project.serializer()))
}
```

> You can get the full code [here](./code/example/example-polymorphic-sealed-class-01.kt).

```typescript
export type Project =
  | Project.DeprecatedProject
  | Project.OProj;

export namespace Project {
  export enum Kind {
    DeprecatedProject = "io.github.esafak.kotlintsgen.example.examplePolymorphicSealedClass01.DeprecatedProject",
    OProj = "OProj",
  }

  export interface DeprecatedProject {
    kind: Project.Kind.DeprecatedProject;
    name: string;
    reason: string;
  }

  export interface OProj {
    kind: Project.Kind.OProj;
    name: string;
    owner: string;
  }
}
```

<!--- TEST -->

### Nested sealed classes

Kotlinx Serialization recursively includes nested sealed subclasses in the
parent sealed descriptor. The generator flattens the hierarchy: concrete
descendants become branches of the parent union, while intermediate sealed
classes such as `Retriever` are not emitted as branches themselves.

If two concrete descendants have the same simple name, generation fails with
a clear declaration-collision error. Use distinct `@SerialName` values with
different simple names or
`NamespaceConfig.DescriptorNamePrefix` to disambiguate them.

```kotlin
@Serializable
sealed class Dog {
  abstract val name: String

  @Serializable
  @SerialName("Dog.Mutt")
  class Mutt(override val name: String, val loveable: Boolean = true) : Dog()

  @Serializable
  sealed class Retriever : Dog() {
    abstract val colour: String

    @Serializable
    @SerialName("Dog.Retriever.Golden")
    data class Golden(
      override val name: String,
      override val colour: String,
      val cute: Boolean = true,
    ) : Retriever()

    @Serializable
    @SerialName("Dog.Retriever.NovaScotia")
    data class NovaScotia(
      override val name: String,
      override val colour: String,
      val adorable: Boolean = true,
    ) : Retriever()
  }
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Dog.serializer()))
}
```

> You can get the full code [here](./code/example/example-polymorphic-sealed-class-02.kt).

```typescript
export type Dog =
  | Dog.Golden
  | Dog.Mutt
  | Dog.NovaScotia;

export namespace Dog {
  export enum Type {
    Mutt = "Dog.Mutt",
    Golden = "Dog.Retriever.Golden",
    NovaScotia = "Dog.Retriever.NovaScotia",
  }

  export interface Mutt {
    type: Dog.Type.Mutt;
    name: string;
    loveable?: boolean;
  }

  export interface Golden {
    type: Dog.Type.Golden;
    name: string;
    colour: string;
    cute?: boolean;
  }

  export interface NovaScotia {
    type: Dog.Type.NovaScotia;
    name: string;
    colour: string;
    adorable?: boolean;
  }
}
```

<!--- TEST -->

### Objects

```kotlin
@Serializable
sealed class Response

@Serializable
object EmptyResponse : Response()

@Serializable
class TextResponse(val text: String) : Response()

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(
    tsGenerator.generate(Response.serializer())
  )
}
```

> You can get the full code [here](./code/example/example-polymorphic-objects-01.kt).

```typescript
export type Response =
  | Response.EmptyResponse
  | Response.TextResponse;

export namespace Response {
  export enum Type {
    EmptyResponse = "io.github.esafak.kotlintsgen.example.examplePolymorphicObjects01.EmptyResponse",
    TextResponse = "io.github.esafak.kotlintsgen.example.examplePolymorphicObjects01.TextResponse",
  }

  export interface EmptyResponse {
    type: Response.Type.EmptyResponse;
  }

  export interface TextResponse {
    type: Response.Type.TextResponse;
    text: string;
  }
}
```

<!--- TEST -->
