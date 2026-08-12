@file:OptIn(InternalSerializationApi::class)

package io.github.esafak.kotlintsgen.core.experiments

import kotlin.reflect.KProperty1
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.serializer


/**
 * A single element of a Tuple.
 * Used to encode and decode a single element of a Tuple.
 *
 * See [TupleSerializer] for more information.
 */
data class TupleElement<T, E>(
  val name: String,
  val index: Int,
  val elementSerializer: KSerializer<E>,
  val elementAccessor: T.() -> E,
  val isOptional: Boolean = false,
) {
  internal val elementDescriptor: SerialDescriptor
    get() = elementSerializer.descriptor

  fun encodeElement(
    encoder: CompositeEncoder,
    tupleDescriptor: SerialDescriptor,
    value: T,
  ) {
    encoder.encodeSerializableElement(
      descriptor = tupleDescriptor,
      index = index,
      serializer = elementSerializer,
      value = value.elementAccessor(),
    )
  }

  fun decodeElement(
    decoder: CompositeDecoder,
    tupleDescriptor: SerialDescriptor,
  ): E {
    return decoder.decodeSerializableElement(
      descriptor = tupleDescriptor,
      index = index,
      deserializer = elementSerializer,
    )
  }
}


/**
 * Create a new [TupleElement].
 */
inline fun <T, reified E> tupleElement(
  index: Int,
  name: String,
  noinline elementAccessor: T.() -> E,
  serializer: KSerializer<E> = serializer(),
  isOptional: Boolean = false,
): TupleElement<T, E> {
  return TupleElement(
    name = name,
    index = index,
    elementSerializer = serializer,
    elementAccessor = elementAccessor,
    isOptional = isOptional,
  )
}


fun <T> tupleElements(
  builder: TupleElementsBuilder<T>.() -> Unit
): List<TupleElement<T, *>> {
  val tupleElementsBuilder = TupleElementsBuilder<T>()
  tupleElementsBuilder.builder()
  return tupleElementsBuilder.elements
}


class TupleElementsBuilder<T> {

  private val _elements: ArrayDeque<TupleElement<T, *>> = ArrayDeque()
  val elements: List<TupleElement<T, *>>
    get() = _elements.toList()

  @PublishedApi
  internal val elementsSize by _elements::size

  inline fun <reified E> element(
    property: KProperty1<T, E>,
    isOptional: Boolean = false,
  ) {
    element(property.name, property, isOptional)
  }

  inline fun <reified E> element(
    name: String,
    noinline elementAccessor: T.() -> E,
    isOptional: Boolean = false,
  ) {
    element(tupleElement(elementsSize, name, elementAccessor, isOptional = isOptional))
  }

  fun element(element: TupleElement<T, *>) {
    val indexedElements = (_elements + element).sortedBy { it.index }
    require(indexedElements.zipWithNext().none { (current, next) ->
      current.isOptional && !next.isOptional
    }) {
      "Optional tuple elements must be trailing; required element '${element.name}' follows an optional element"
    }
    _elements.addLast(element)
  }
}


/**
 * Encode a serializable class as a Tuple: a collection of the class' properties.
 *
 * In JSON a tuple is represented as an array.
 * Encoding as a tuple saves space because the names of the properties are not encoded.
 */
abstract class TupleSerializer<T>(
  serialName: String,
  buildElements: TupleElementsBuilder<T>.() -> Unit
) : KSerializer<T> {

  val tupleElements: List<TupleElement<T, *>> = run {
    val tupleElementsBuilder = TupleElementsBuilder<T>()
    tupleElementsBuilder.buildElements()
    tupleElementsBuilder.elements.sortedBy { it.index }
  }
  private val indexedTupleElements = tupleElements.associateBy { it.index }

  abstract fun tupleConstructor(elements: Iterator<*>): T

  override val descriptor: SerialDescriptor = buildSerialDescriptor(
    serialName = serialName,
    kind = StructureKind.LIST,
    typeParameters = emptyArray(),
  ) {
    tupleElements
      .sortedBy { it.index }
      .forEach { tupleElement ->
        element(
          elementName = tupleElement.name,
          descriptor = tupleElement.elementDescriptor,
          isOptional = tupleElement.isOptional,
        )
      }
  }

  override fun serialize(encoder: Encoder, value: T) {
    encoder.encodeCollection(descriptor, tupleElements) { i, tupleElement ->
      tupleElement.encodeElement(this, descriptor, value)
    }
  }

  override fun deserialize(decoder: Decoder): T = decoder.decodeStructure(descriptor) {

    // the collection size isn't required here, but we need to decode it to get it out of the way
    decodeCollectionSize(descriptor)

    val elements = if (decodeSequentially()) {
      tupleElements.asSequence().map {
        it.decodeElement(this@decodeStructure, descriptor)
      }
    } else {
      generateSequence { decodeElementIndex(descriptor) }
        .takeWhile { index ->
          when (index) {
            CompositeDecoder.UNKNOWN_NAME         -> error("unknown name at index:$index")
            CompositeDecoder.DECODE_DONE          -> false
            !in indexedTupleElements.keys.indices -> error("unexpected index:$index")
            else                                  -> true
          }
        }.map { index ->
          val tupleElement = indexedTupleElements.getOrElse(index) {
            error("no tuple element at index:$index")
          }
          tupleElement.decodeElement(this@decodeStructure, descriptor)
        }
    }
    // elements sequence *must* be collected inside 'decodeStructure'
    tupleConstructor(elements.iterator())
  }
}
