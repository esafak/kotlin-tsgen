# Tuples

In TypeScript,
[tuples](https://www.typescriptlang.org/docs/handbook/2/objects.html#tuple-types)
are a compact format for data structures. They're like fixed-length arrays that only contain the
type, not the property names. Excluding the property names is especially useful when size and speed
is important, because the JSON will be much more compact.

### Tuple example

Here's an example of a tuple definition in TypeScript:

<!-- this code block uses four backticks ```` to workaround https://github.com/Kotlin/kotlinx-knit/issues/57 -->

````typescript
type StringNumberPair = [str: string, num: number];
````

This would get serialized to a JSON array

<!--- @formatter:off -->
```json
["some string value", 123]
```
<!--- @formatter:on -->

which is more compact than an equivalent JSON object, which requires property names.

<!--- @formatter:off -->
```json
{ "str": "some string value", "num": 123 }
```
<!--- @formatter:on -->

## Tuples in KotlinTsGen

Tuples are a bit difficult to create in Kotlinx Serialization, but KotlinTsGen includes
[TupleSerializer](../modules/kotlin-tsgen-core/src/commonMain/kotlin/io/github/esafak/kotlintsgen/core/experiments/tuple.kt)
which can help. It requires a name, an ordered list of elements, and a constructor for
deserializing.

<!--- TEST_NAME TuplesTest -->
<!--- INCLUDE .*\.kt
import io.github.esafak.kotlintsgen.*
import io.github.esafak.kotlintsgen.core.experiments.TupleSerializer
import kotlinx.serialization.*
-->

### Tuple example

Let's say we have a class, `SimpleTypes`, that we want to serialize. We need to create a bespoke
tuple serializer for it, and override the plugin-generated serializer.

```kotlin
@Serializable(with = SimpleTypes.SimpleTypesSerializer::class)
data class SimpleTypes(
  val aString: String,
  var anInt: Int,
  val aDouble: Double?,
  val bool: Boolean,
  private val privateMember: String,
) {
  // Create `SimpleTypesSerializer` inside `SimpleTypes`, so it
  // has access to the private property `privateMember`.
  object SimpleTypesSerializer : TupleSerializer<SimpleTypes>(
    "SimpleTypes",
    {
      // Provide all tuple elements, in order, using the 'elements' helper method.
      element(SimpleTypes::aString)
      element(SimpleTypes::anInt)
      element(SimpleTypes::aDouble)
      element(SimpleTypes::bool)
      element(SimpleTypes::privateMember)
    }
  ) {
    override fun tupleConstructor(elements: Iterator<*>): SimpleTypes {
      // When deserializing, the elements will be available as a list, in the order defined above
      return SimpleTypes(
        elements.next() as String,
        elements.next() as Int,
        elements.next() as Double,
        elements.next() as Boolean,
        elements.next() as String,
      )
    }
  }
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(SimpleTypes.serializer()))
}
```

> You can get the full code [here](./code/example/example-tuple-01.kt).

```typescript
export type SimpleTypes = [
  aString: string,
  anInt: Int,
  aDouble: Double | null,
  bool: boolean,
  privateMember: string,
];

export type Int = number;

export type Double = number;
```

<!--- TEST -->

### Tuple labels

By default, the tuple elements are labelled with the names of properties, not the `@SerialName`,
which will be ignored. This isn't important for serialization because the tuple will be serialized
without the name of the property.

The name of the label can be overridden if desired while defining the elements.

```kotlin
@Serializable(with = PostalAddressUSA.Serializer::class)
data class PostalAddressUSA(
  @SerialName("num") // 'SerialName' will be ignored in 'Tuple' form
  val houseNumber: String,
  val streetName: String,
  val postcode: String,
) {
  object Serializer : TupleSerializer<PostalAddressUSA>(
    "PostalAddressUSA",
    {
      element(PostalAddressUSA::houseNumber)
      // custom labels for 'streetName', 'postcode'
      element("street", PostalAddressUSA::streetName)
      element("zip", PostalAddressUSA::postcode)
    }
  ) {
    override fun tupleConstructor(elements: Iterator<*>): PostalAddressUSA {
      return PostalAddressUSA(
        elements.next() as String,
        elements.next() as String,
        elements.next() as String,
      )
    }
  }
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(PostalAddressUSA.serializer()))
}
```

> You can get the full code [here](./code/example/example-tuple-02.kt).

```typescript
export type PostalAddressUSA = [
  houseNumber: string, // @SerialName("num") was ignored
  street: string, // custom name
  zip: string, // custom name
];
```

<!--- TEST -->

### Optional elements in tuples

Tuple elements can be marked optional with `isOptional = true`. Optional elements must be trailing;
the builder rejects a required element after an optional one. This optionality is propagated to the
serial descriptor and TypeScript type, but it does not change the fixed-arity wire representation:
`TupleSerializer` still encodes and decodes every element.

```kotlin
@Serializable(with = OptionalFields.Serializer::class)
data class OptionalFields(
  val requiredString: String,
  val nullableString: String?,
  val optionalString: String = "",
  val nullableOptionalString: String? = "",
) {
  object Serializer : TupleSerializer<OptionalFields>(
    "OptionalFields",
    {
      element(OptionalFields::requiredString)
      element(OptionalFields::nullableString)
      element(OptionalFields::optionalString, isOptional = true)
      element(OptionalFields::nullableOptionalString, isOptional = true)
    }
  ) {
    override fun tupleConstructor(elements: Iterator<*>): OptionalFields {
      val iter = elements.iterator()
      return OptionalFields(
        iter.next() as String,
        iter.next() as String?,
        iter.next() as String,
        iter.next() as String?,
      )
    }
  }
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(OptionalFields.serializer()))
}
```

> You can get the full code [here](./code/example/example-tuple-03.kt).

```typescript
export type OptionalFields = [
  requiredString: string,
  nullableString: string | null,
  optionalString?: string,
  nullableOptionalString?: string | null,
];
```

<!--- TEST -->

### Properties all the same type

```kotlin
@Serializable(with = Coordinates.Serializer::class)
data class Coordinates(
  val x: Int,
  val y: Int,
  val z: Int,
) {
  object Serializer : TupleSerializer<Coordinates>(
    "Coordinates",
    {
      element(Coordinates::x)
      element(Coordinates::y)
      element(Coordinates::z)
    }
  ) {
    override fun tupleConstructor(elements: Iterator<*>): Coordinates {
      return Coordinates(
        elements.next() as Int,
        elements.next() as Int,
        elements.next() as Int,
      )
    }
  }
}

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(Coordinates.serializer()))
}
```

> You can get the full code [here](./code/example/example-tuple-04.kt).

```typescript
export type Coordinates = [
  x: Int,
  y: Int,
  z: Int,
];

export type Int = number;
```

<!--- TEST -->

### Tuples as interface properties

```kotlin
import io.github.esafak.kotlintsgen.example.exampleTuple04.Coordinates

@Serializable
class GameLocations(
  val homeLocation: Coordinates,
  val allLocations: List<Coordinates>,
  val namedLocations: Map<String, Coordinates>,
  val locationsInfo: Map<Coordinates, String>,
)

fun main() {
  val tsGenerator = KotlinTsGenerator()
  println(tsGenerator.generate(GameLocations.serializer()))
}
```

> You can get the full code [here](./code/example/example-tuple-05.kt).

```typescript
export interface GameLocations {
  homeLocation: Coordinates;
  allLocations: Coordinates[];
  namedLocations: { [key: string]: Coordinates };
  locationsInfo: Map<Coordinates, string>;
}

export type Coordinates = [
  x: Int,
  y: Int,
  z: Int,
];

export type Int = number;
```

<!--- TEST -->
