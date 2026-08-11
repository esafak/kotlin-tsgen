package io.github.esafak.kotlintsgen

import io.github.esafak.kotlintsgen.core.TsDeclaration
import io.github.esafak.kotlintsgen.core.UnimplementedKotlinTsGenApi
import kotlin.jvm.JvmInline
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule


/**
 * @param[indent] Define the indentation that is used when generating source code
 * @param[declarationSeparator] The string that is used when joining [TsDeclaration]s
 * @param[namespaceConfig] (UNIMPLEMENTED) How elements are grouped into [TsDeclaration.TsNamespace]s.
 * @param[typeAliasTyping] Control whether type aliases are simple, or 'branded'.
 * @param[serializersModule] Used to obtain contextual and polymorphic information.
 */
data class KotlinTsConfig(
  val indent: String = "  ",
  val declarationSeparator: String = "\n\n",
  @UnimplementedKotlinTsGenApi
  val namespaceConfig: NamespaceConfig = NamespaceConfig.Disabled,
  val typeAliasTyping: TypeAliasTypingConfig = TypeAliasTypingConfig.None,
  val serializersModule: SerializersModule = EmptySerializersModule(),
) {

  sealed interface NamespaceConfig {
    /** Use the prefix of the [SerialDescriptor]  */
    object DescriptorNamePrefix : NamespaceConfig
    /** don't generate a namespace */
    object Disabled : NamespaceConfig
    @JvmInline
    value class Static(val namespace: String) : NamespaceConfig
  }

  sealed interface TypeAliasTypingConfig {
    object None : TypeAliasTypingConfig
    object BrandTyping : TypeAliasTypingConfig
  }

}
