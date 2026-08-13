package io.github.esafak.kotlintsgen.core

import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor

internal fun SerialDescriptor.isJsonContentPolymorphicSerializer(): Boolean =
  kind == PolymorphicKind.SEALED &&
    elementsCount == 0 &&
    serialName.startsWith("JsonContentPolymorphicSerializer<")
