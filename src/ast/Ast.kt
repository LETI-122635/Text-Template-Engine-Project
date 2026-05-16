package project.ast

/**
 * The Abstract Syntax Tree (AST) represents the language semantics.
 *
 * It is independent from:
 * - how it is parsed (ANTLR grammar)
 * - how it is executed (interpreter)
 * - how it is rendered (template engine)
 *
 * This AST is the contract between the compiler and runtime.
 */

// Root of a language script.
data class Script(val statements: List<Statement>)

// All executable constructs in the language.
sealed interface Statement

// Assignment: bind an expression to a named variable.
// Variables persist throughout the execution context.
data class Assign(val name: String, val expr: Expression) : Statement

// Print: output an expression value to the rendered result.
data class Output(val expr: Expression) : Statement

// Conditional: execute one of two blocks based on a condition.
data class If(val condition: Expression, val thenBlock: List<Statement>, val elseBlock: List<Statement> = emptyList()) : Statement

// Loop: iterate over a JSON array value, executing a body for each element.
data class For(val variable: String, val collection: Expression, val body: List<Statement>) : Statement

// While loop: repeated execution while condition holds.
data class While(val condition: Expression, val body: List<Statement>) : Statement

// Break statement: interrupts the nearest enclosing loop.
object Break : Statement

// All value-producing expressions.
sealed interface Expression

// Numeric literal.
data class NumberLiteral(val value: Double) : Expression

// String literal.
data class StringLiteral(val value: String) : Expression

// Variable reference.
// Resolves to the variable environment first, then to the root input object.
data class Variable(val name: String) : Expression

// Property access: structured field navigation.
// Examples: user.name, item.price, user.address.city
// This is a first-class language feature, not a parsing trick.
data class PropertyAccess(val target: Expression, val property: String) : Expression

// Binary operation: arithmetic, comparison, or boolean operation.
data class BinaryOp(val left: Expression, val op: Operator, val right: Expression) : Expression

// All operators supported by the language.
enum class Operator {
    PLUS, MINUS, TIMES, DIV, MOD,
    EQ, NEQ,
    LT, LTE, GT, GTE
}