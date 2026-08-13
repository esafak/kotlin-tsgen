package io.github.esafak.kotlintsgen.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule


/**
 * Recursively extract all descriptors from a serializer and its elements.
 */
fun interface SerializerDescriptorsExtractor {

  operator fun invoke(
    serializer: KSerializer<*>
  ): Set<SerialDescriptor>

  companion object {
    /** The default [SerializerDescriptorsExtractor], for easy use. */
    fun default(
      serializersModule: SerializersModule,
      contentPolymorphicSubtypes: Map<SerialDescriptor, List<SerialDescriptor>> = emptyMap(),
    ): SerializerDescriptorsExtractor {
      return WithSerializersModule(
        elementDescriptorsExtractor = TsElementDescriptorsExtractor.default(
          serializersModule,
          contentPolymorphicSubtypes,
        )
      )
    }
  }

  object Default : SerializerDescriptorsExtractor {
    private val delegate: SerializerDescriptorsExtractor by lazy {
      WithSerializersModule(
        elementDescriptorsExtractor = TsElementDescriptorsExtractor.default(EmptySerializersModule())
      )
    }

    override fun invoke(serializer: KSerializer<*>): Set<SerialDescriptor> = delegate(serializer)
  }

  private class WithSerializersModule(
    private val elementDescriptorsExtractor: TsElementDescriptorsExtractor,
  ) : SerializerDescriptorsExtractor {

    override operator fun invoke(
      serializer: KSerializer<*>
    ): Set<SerialDescriptor> {
      return extractDescriptors(serializer.descriptor)
        .distinctBy { it.serialName.removeSuffix("?") to it.kind }
        .toSet()
    }

    private tailrec fun extractDescriptors(
      current: SerialDescriptor? = null,
      queue: ArrayDeque<SerialDescriptor> = ArrayDeque(),
      extracted: Set<SerialDescriptor> = emptySet(),
      visited: Set<SerialDescriptor> = emptySet(),
    ): Set<SerialDescriptor> {
      return if (current == null) {
        extracted
      } else if (current in visited) {
        extractDescriptors(queue.removeFirstOrNull(), queue, extracted, visited)
      } else {
        val visitedWithCurrent = visited + current
        val currentDescriptors = elementDescriptorsExtractor.elementDescriptors(current)
        queue.addAll(currentDescriptors - visitedWithCurrent)

        // A contextual descriptor is just a placeholder (e.g. `ContextualSerializer<Foo>`).
        // When it resolves to a concrete descriptor via the SerializersModule, that concrete
        // descriptor (e.g. `interface Foo`) is already queued above. The placeholder itself
        // must NOT be emitted as a declaration, otherwise it renders as `type Foo = any` and
        // collides with the resolved `interface Foo` (TS2300 duplicate identifier). When it
        // cannot be resolved we keep it, so the referencing field still resolves to
        // `type Foo = any` rather than an undefined type.
        val nextExtracted =
          if (current.kind == SerialKind.CONTEXTUAL && currentDescriptors.any()) extracted
          else extracted + current

        extractDescriptors(queue.removeFirstOrNull(), queue, nextExtracted, visitedWithCurrent)
      }
    }
  }
}


@OptIn(ExperimentalSerializationApi::class)
fun interface TsElementDescriptorsExtractor {
  fun elementDescriptors(descriptor: SerialDescriptor): Iterable<SerialDescriptor>

  companion object {

    fun default(serializersModule: SerializersModule) =
      default(serializersModule, emptyMap())

    fun default(
      serializersModule: SerializersModule,
      contentPolymorphicSubtypes: Map<SerialDescriptor, List<SerialDescriptor>>,
    ) = TsElementDescriptorsExtractor { descriptor ->
        when (descriptor.kind) {
          SerialKind.ENUM       -> emptyList()

          SerialKind.CONTEXTUAL ->
            runCatching { serializersModule.getContextualDescriptor(descriptor) }
              .getOrNull()
              ?.let(::listOf)
              .orEmpty()

          PrimitiveKind.BOOLEAN,
          PrimitiveKind.BYTE,
          PrimitiveKind.CHAR,
          PrimitiveKind.SHORT,
          PrimitiveKind.INT,
          PrimitiveKind.LONG,
          PrimitiveKind.FLOAT,
          PrimitiveKind.DOUBLE,
          PrimitiveKind.STRING -> emptyList()

          StructureKind.CLASS,
          StructureKind.LIST,
          StructureKind.MAP,
          StructureKind.OBJECT -> descriptor.elementDescriptors

          PolymorphicKind.SEALED -> contentPolymorphicSubtypes[descriptor]
            ?.let { subclasses -> subclasses + subclasses.flatMap { it.elementDescriptors } }
            ?: sealedSubclassPropertyDescriptors(descriptor)

          PolymorphicKind.OPEN -> {
            val subclasses = serializersModule.getPolymorphicDescriptors(descriptor)
            subclasses + subclasses.flatMap { it.elementDescriptors }
          }
        }
      }

    private fun sealedSubclassPropertyDescriptors(
      descriptor: SerialDescriptor,
    ): Iterable<SerialDescriptor> =
      descriptor.elementDescriptors
        .filter { it.kind == SerialKind.CONTEXTUAL }
        .flatMap { it.elementDescriptors }
        .flatMap { subclass ->
          if (subclass.kind == PolymorphicKind.SEALED) {
            sealedSubclassPropertyDescriptors(subclass)
          } else {
            subclass.elementDescriptors
          }
        }
  }
}
