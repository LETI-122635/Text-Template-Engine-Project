package project.interpreter

import project.ast.Assign
import project.ast.BinaryOp
import project.ast.Block
import project.ast.Break
import project.ast.Expression
import project.ast.For
import project.ast.If
import project.ast.NumberLiteral
import project.ast.Operator
import project.ast.Output
import project.ast.PropertyAccess
import project.ast.Script
import project.ast.Statement
import project.ast.StringLiteral
import project.ast.Variable
import project.ast.While
import java.lang.StringBuilder
import kotlin.collections.get

class Interpreter(private val inputRoot: Any?, private val output: StringBuilder) {
    private val env = mutableMapOf<String, Any?>()

    private class BreakSignal : RuntimeException()

    fun run(script: Script) {
        for (stmt in script.statements) execute(stmt)
    }

    // evaluate a single expression and return the value
    fun runExpression(expr: Expression): Any? = eval(expr)

    private fun execute(stmt: Statement) {
        when (stmt) {
            is Block -> {
                for (s in stmt.statements) execute(s)
            }
            is Assign -> env[stmt.name] = eval(stmt.expr)
            is Output -> {
                val v = eval(stmt.expr)
                output.append(stringify(v))
            }
            is If -> {
                val cond = eval(stmt.condition)
                if (asBoolean(cond)) {
                    for (s in stmt.thenBlock) execute(s)
                } else {
                    for (s in stmt.elseBlock) execute(s)
                }
            }
            is For -> {
                val collection = eval(stmt.collection)
                if (collection is List<*>) {
                    try {
                        for (elem in collection) {
                            env[stmt.variablie] = elem
                            for (s in stmt.body) execute(s)
                        }
                    } catch (_: BreakSignal) {
                        // Exit only the nearest loop.
                    }
                }
            }
            is While -> {
                try {
                    while (asBoolean(eval(stmt.condition))) {
                        for (s in stmt.body) execute(s)
                    }
                } catch (_: BreakSignal) {
                    // Exit only the nearest loop.
                }
            }
            is Break -> throw BreakSignal()
            else -> error("Unsupported statement: $stmt")
        }
    }

    private fun eval(expr: Expression): Any? {
        return when (expr) {
            is NumberLiteral -> expr.value
            is StringLiteral -> expr.value
            is Variable -> lookupVariable(expr.name)
            is PropertyAccess -> {
                val target = eval(expr.target)
                accessProperty(target, expr.property)
            }
            is BinaryOp -> {
                val left = eval(expr.left)
                val right = eval(expr.right)
                evalBinary(left, expr.op, right)
            }
            else -> error("Unsupported expression: $expr")
        }
    }

    private fun lookupVariable(name: String): Any? {
        if (env.containsKey(name)) return env[name]
        // look into input root
        return if (inputRoot is Map<*, *>) inputRoot[name] else null
    }

    private fun accessProperty(target: Any?, prop: String): Any? {
        if (target is Map<*, *>) return target[prop]
        return null
    }

    private fun evalBinary(left: Any?, op: Operator, right: Any?): Any? {
        when (op) {
            Operator.PLUS -> {
                if (left is String || right is String) return stringify(left) + stringify(right)
                return mathOp(left, right) { a, b -> a + b }
            }
            Operator.MINUS -> return mathOp(left, right) { a, b -> a - b }
            Operator.TIMES -> return mathOp(left, right) { a, b -> a * b }
            Operator.DIV -> return mathOp(left, right) { a, b -> a / b }
            Operator.MOD -> return mathOp(left, right) { a, b -> a % b }
            Operator.EQ -> return equalsOp(left, right)
            Operator.NEQ -> return !equalsOp(left, right)
            Operator.LT -> return compareOp(left, right) < 0
            Operator.LTE -> return compareOp(left, right) <= 0
            Operator.GT -> return compareOp(left, right) > 0
            Operator.GTE -> return compareOp(left, right) >= 0
        }
    }

    private fun equalsOp(left: Any?, right: Any?): Boolean = when {
        left is Number && right is Number -> left.toDouble() == right.toDouble()
        else -> left == right
    }

    private fun compareOp(left: Any?, right: Any?): Int {
        if (left is Number && right is Number) return left.toDouble().compareTo(right.toDouble())
        if (left is String && right is String) return left.compareTo(right)
        error("Cannot compare $left and $right")
    }

    private fun mathOp(left: Any?, right: Any?, f: (Double, Double) -> Double): Any? {
        if (left is Number && right is Number) {
            val r = f(left.toDouble(), right.toDouble())
            // return integer when possible
            return if (r % 1.0 == 0.0) r.toLong() else r
        }
        error("Invalid math operands: $left, $right")
    }

    private fun asBoolean(v: Any?): Boolean {
        return when (v) {
            is Boolean -> v
            is Number -> v.toDouble() != 0.0
            is String -> v.isNotEmpty()
            null -> false
            else -> true
        }
    }

    fun stringify(v: Any?): String = when (v) {
        is String -> v
        is Number -> if (v.toDouble() % 1.0 == 0.0) (v.toLong().toString()) else v.toString()
        null -> "null"
        else -> v.toString()
    }
}