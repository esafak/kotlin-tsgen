package io.github.esafak.kotlintsgen.core


/** Create an inline TypeScript expression used as a type. */
fun custom(expression: String): TsLiteral.Custom = TsLiteral.Custom(expression)


/** Reference a named TypeScript type that is provided outside of generated source. */
fun external(name: String): TsLiteral.ExternalType = external(TsElementId(name))


/** Reference a named TypeScript type that is provided outside of generated source. */
fun external(id: TsElementId): TsLiteral.ExternalType = TsLiteral.ExternalType(id)


/** Create a reference to a named TypeScript declaration. */
fun ref(name: String, nullable: Boolean = false): TsTypeRef.Declaration =
  TsTypeRef.Declaration(TsElementId(name), null, nullable)


/** Create an emitted TypeScript type alias. */
fun typeAlias(name: String, target: TsTypeRef): TsDeclaration.TsTypeAlias =
  TsDeclaration.TsTypeAlias(TsElementId(name), target)


/** Create an emitted TypeScript type alias to an inline or external type. */
fun typeAlias(name: String, target: TsLiteral): TsDeclaration.TsTypeAlias =
  typeAlias(name, TsTypeRef.Literal(target, nullable = false))


/** Create an emitted TypeScript type alias to another named type. */
fun typeAlias(name: String, target: String): TsDeclaration.TsTypeAlias =
  typeAlias(name, ref(target))
