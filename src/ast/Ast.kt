package project.ast

// The AST is the shared contract between parsing, interpretation, and rendering.

// A complete template script contains zero or more executable statements.
data class Script(val statements: List<Statement>)

// All executable constructs in the template language.
sealed interface Statement

// Stores the result of an expression in a named variable.
data class Assign(val name: String, val expr: Expression) : Statement

// Writes an expression value to the rendered output.
data class Output(val expr: Expression) : Statement

// Standard conditional with an optional else branch.
data class If(val condition: Expression, val thenBlock: List<Statement>, val elseBlock: List<Statement> = emptyList()) : Statement

// Loop over a JSON array, binding each element to the loop variable.
data class For(val variable: String, val collection: Expression, val body: List<Statement>) : Statement

// All value-producing nodes in the language.
sealed interface Expression

// Numeric literal stored as Double to simplify arithmetic operations.
data class NumberLiteral(val value: Double) : Expression

// String literal after escape processing.
data class StringLiteral(val value: String) : Expression

// Variable reference resolved against the environment or input JSON.
data class Variable(val name: String) : Expression

// Nested property access such as user.name or item.price.
data class PropertyAccess(val target: Expression, val property: String) : Expression

// Binary operator node used for arithmetic and boolean comparisons.
data class BinaryOp(val left: Expression, val op: Operator, val right: Expression) : Expression

// Operators are kept explicit so interpretation stays simple and typed.
enum class Operator {
    PLUS, MINUS, TIMES, DIV, MOD,
    EQ, NEQ,
    LT, LTE, GT, GTE
}