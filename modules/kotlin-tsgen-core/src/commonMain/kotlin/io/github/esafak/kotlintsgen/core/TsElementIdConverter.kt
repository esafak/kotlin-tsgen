package io.github.esafak.kotlintsgen.core

import kotlinx.serialization.descriptors.SerialDescriptor


fun interface TsElementIdConverter {

  operator fun invoke(descriptor: SerialDescriptor): TsElementId

  object Default : TsElementIdConverter {
    override operator fun invoke(descriptor: SerialDescriptor): TsElementId {

      val serialName = descriptor.serialName.removeSuffix("?")

      val namespace = serialName.substringBeforeLast('.')

      val id = serialName
        .substringAfterLast('.')
        .substringAfter("<")
        .substringBeforeLast(">")

      TsIdentifierValidator.assertValidIdentifier(id, "type name from serial name '$serialName'")
      if (namespace.isNotBlank()) {
        namespace.split('.').forEach { segment ->
          TsIdentifierValidator.assertValidIdentifier(
            segment,
            "namespace segment from serial name '$serialName'",
          )
        }
      }

      return when {
        namespace.isBlank() -> TsElementId(id)
        else                -> TsElementId("$namespace.$id")
      }
    }
  }
}
