package io.github.esafak.kotlintsgen.core


/** Thrown when a generator-created TypeScript identifier cannot be emitted safely. */
class InvalidTsIdentifierException(
  val identifier: String,
  val context: String,
  reason: String,
) : IllegalArgumentException(
  "Invalid TypeScript identifier '$identifier' for $context: $reason"
)


/** Validates names that are emitted in TypeScript identifier positions. */
internal object TsIdentifierValidator {

  // TODO: consider supporting Unicode TypeScript identifiers without weakening diagnostics.
  private val identifierPattern = Regex("^[A-Za-z_\\$][A-Za-z0-9_\\$]*$")

  // These vocabulary names are rejected conservatively because they collide with types emitted
  // by TsSourceCodeGenerator.generatePrimitive, even when they are not all strict JS keywords.
  private val reservedNames = setOf(
    "as",
    "any",
    "async",
    "await",
    "bigint",
    "boolean",
    "break",
    "case",
    "catch",
    "class",
    "const",
    "constructor",
    "continue",
    "debugger",
    "declare",
    "default",
    "delete",
    "do",
    "else",
    "enum",
    "export",
    "extends",
    "false",
    "finally",
    "for",
    "from",
    "function",
    "get",
    "if",
    "implements",
    "import",
    "in",
    "infer",
    "instanceof",
    "interface",
    "is",
    "keyof",
    "let",
    "module",
    "namespace",
    "never",
    "new",
    "null",
    "number",
    "object",
    "of",
    "package",
    "private",
    "protected",
    "public",
    "readonly",
    "require",
    "return",
    "set",
    "static",
    "string",
    "super",
    "switch",
    "symbol",
    "this",
    "throw",
    "true",
    "try",
    "type",
    "typeof",
    "undefined",
    "unique",
    "unknown",
    "var",
    "void",
    "while",
    "with",
    "yield",
  )

  fun assertValidIdentifier(
    identifier: String,
    context: String,
  ) {
    val reason = when {
      !isValidIdentifier(identifier) -> when {
        !identifierPattern.matches(identifier) ->
          "it must match ^[A-Za-z_$][A-Za-z0-9_$]*$"

        else ->
          "it is reserved by TypeScript or conflicts with a generated primitive type"
      }

      else -> null
    }

    if (reason != null) {
      throw InvalidTsIdentifierException(identifier, context, reason)
    }
  }

  fun isValidIdentifier(identifier: String): Boolean {
    return isValidIdentifierSyntax(identifier) && identifier !in reservedNames
  }

  fun isValidIdentifierSyntax(identifier: String): Boolean {
    return identifierPattern.matches(identifier)
  }
}
