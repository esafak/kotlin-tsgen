# Customising Output

<!--- TEST_NAME CustomisingOutputTest -->
<!--- INCLUDE .*\.kt
import kotlinx.serialization.*
import io.github.esafak.kotlintsgen.*
-->

### Overriding output

If you want to override what KotlinTsGen produces, then you can provide overrides.

By default, `Double` is transformed to `number`, but now we want to alias `Double` to `double`.

```kotlin
import kotlinx.serialization.builtins.serializer
import io.github.esafak.kotlintsgen.core.*

@Serializable
data class Item(
  val price: Double,
  val count: Int,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()

  tsGenerator.descriptorOverrides +=
    Double.serializer().descriptor to TsDeclaration.TsTypeAlias(
      id = TsElementId("Double"),
      typeRef = TsTypeRef.Declaration(
        id = TsElementId("double"),
        parent = null,
        nullable = false,
      )
    )

  println(tsGenerator.generate(Item.serializer()))
}
```

> You can get the full code [here](./code/example/example-customising-output-01.kt).

```typescript
export interface Item {
  price: Double;
  count: Int;
}

export type Double = double; // assume that 'double' will be provided by another library

export type Int = number;
```

<!--- TEST TS_COMPILE_OFF -->

Instead of changing the output to be a type alias, a custom 'literal' type can be set instead.

```kotlin
import kotlinx.serialization.builtins.serializer
import io.github.esafak.kotlintsgen.core.*

@Serializable
data class Item(
  val price: Double,
  val count: Int,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()

  tsGenerator.descriptorOverrides +=
    Double.serializer().descriptor to TsLiteral.Custom("customDouble")

  println(tsGenerator.generate(Item.serializer()))
}
```

> You can get the full code [here](./code/example/example-customising-output-02.kt).

This produces no type alias, and `Double` is overridden to be `customDouble`.

```typescript
export interface Item {
  price: customDouble;
  count: Int;
}

export type Int = number;
```

<!--- TEST TS_COMPILE_OFF -->

### Override nullable properties

Even though UInt is nullable, it should be overridden by the UInt defined in `descriptorOverrides`.

```kotlin
import kotlinx.serialization.builtins.serializer
import io.github.esafak.kotlintsgen.core.*

@Serializable
data class ItemHolder(
  val item: Item,
)

@Serializable
data class Item(
  val count: UInt? = 0u,
  val score: Int? = 0,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()

  tsGenerator.descriptorOverrides +=
    UInt.serializer().descriptor to TsDeclaration.TsTypeAlias(
      id = TsElementId("kotlin.UInt"),
      typeRef = TsTypeRef.Declaration(id = TsElementId("uint"), parent = null, nullable = false)
    )

  tsGenerator.descriptorOverrides += Int.serializer().descriptor to TsLiteral.Custom("customInt")

  println(tsGenerator.generate(ItemHolder.serializer()))
}
```

> You can get the full code [here](./code/example/example-customising-output-03.kt).

```typescript
export interface ItemHolder {
  item: Item;
}

export interface Item {
  count?: UInt | null;
  score?: customInt | null;
}

export type UInt = uint;
```

<!--- TEST TS_COMPILE_OFF -->

### Override both nullable and non-nullable descriptors

`Tick` has a non-nullable UInt, while `Item` has a nullable UInt. Also, in `ItemHolder`, `Tick` is
nullable. Even though a non-nullable override for UInt is supplied, the output shouldn't have
conflicting overrides.

```kotlin
import kotlinx.serialization.builtins.serializer
import io.github.esafak.kotlintsgen.core.*


@Serializable
@JvmInline
value class Tick(val value: UInt)

@Serializable
@JvmInline
value class Phase(val value: Int)

@Serializable
data class ItemHolder(
  val item: Item,
  val tick: Tick?,
  val phase: Phase?,
)

@Serializable
data class Item(
  val count: UInt? = 0u,
  val score: Int? = 0,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()

  tsGenerator.descriptorOverrides +=
    UInt.serializer().descriptor to TsDeclaration.TsTypeAlias(
      id = TsElementId("kotlin.UInt"),
      typeRef = TsTypeRef.Declaration(id = TsElementId("uint"), parent = null, nullable = false)
    )

  tsGenerator.descriptorOverrides += Int.serializer().descriptor to TsLiteral.Custom("customInt")

  println(tsGenerator.generate(ItemHolder.serializer()))
}
```

> You can get the full code [here](./code/example/example-customising-output-04.kt).

```typescript
export interface ItemHolder {
  item: Item;
  tick: Tick | null;
  phase: Phase | null;
}

export interface Item {
  count?: UInt | null;
  score?: customInt | null;
}

export type Tick = UInt;

export type Phase = customInt;

export type UInt = uint;
```

<!--- TEST TS_COMPILE_OFF -->
