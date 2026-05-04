package project.ast

//TEMPLATE SCRIPT
data class Script(val statements: List<Statement>)

//STATEMENTS
sealed interface Statement

data class Assign(val name:String, val expr: Expression) : Statement

data class Output(val expr: Expression) : Statement

data class Block(val statements: List<Statement>) : Statement

data class If(val condition: Expression, val thenBlock: List<Statement>, val elseBlock: List<Statement> = emptyList()) : Statement

data class For(val variablie: String, val collection: Expression, val body: List<Statement>) : Statement

data class While(val condition: Expression, val body: List<Statement>) : Statement

object Break : Statement

//EXPRESSIONS
sealed interface Expression

data class NumberLiteral(val value: Int) : Expression

data class StringLiteral(val value: String) : Expression

data class Variable(val name: String) : Expression

//user.name / user.age
data class PropertyAccess(val target: Expression, val property: String) : Expression

data class BinaryOp(val left: Expression, val op: Operator, val right: Expression) : Expression

//OPERATORS
enum class Operator {
    PLUS, MINUS, TIMES, DIV, MOD,
    EQ, NEQ,
    LT, LTE, GT, GTE
}