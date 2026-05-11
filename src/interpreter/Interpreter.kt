package project.interpreter

import project.ast.*
import project.json.*

// Executes a parsed script against the JSON input and appends rendered text to the output buffer.
class Interpreter(private val inputRoot: JValue, val output: StringBuilder) {
    // Variables declared in one script segment remain available in later segments.
    private val env = mutableMapOf<String, JValue>()

    // Run a whole script statement-by-statement.
    fun run(script: Script) {
        for (stmt in script.statements) execute(stmt)
    }

    // Evaluate a single expression when the template contains an inline expression block.
    fun runExpression(expr: Expression): JValue = eval(expr)


    // Statement execution is kept separate from expression evaluation for clarity.
    private fun execute(stmt: Statement) {
        when (stmt) {
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
                if (collection is JArray) {
                    for (elem in collection.elements) {
                        env[stmt.variable] = elem
                        for (s in stmt.body) execute(s)
                    }
                }
            }
        }
    }

    // Expression evaluation returns structured JSON values so later operations stay typed.
    private fun eval(expr: Expression): JValue {
        return when (expr) {
            is NumberLiteral -> JNumber(expr.value)
            is StringLiteral -> JString(expr.value)
            is Variable -> lookupVariable(expr.name)
            is PropertyAccess -> {
                val target = eval(expr.target)
                return accessProperty(target, expr.property)
            }
            is BinaryOp -> {
                val left = eval(expr.left)
                val right = eval(expr.right)
                return evalBinary(left, expr.op, right)
            }
        }
    }

    // Look up a name first in the script environment, then in the root JSON object.
    private fun lookupVariable(name: String): JValue {
        if (env.containsKey(name)) return env[name]!!
        if (inputRoot is JObject) {
            return inputRoot.fields[name] ?: JNull
        }
        return JNull
    }

    // Property access is only meaningful on objects; other values fall back to null.
    private fun accessProperty(target: JValue, prop: String): JValue {
        if (target is JObject) {
            return target.fields[prop] ?: JNull
        }
        return JNull
    }

    // Binary operations cover arithmetic, equality, and relational comparisons.
    private fun evalBinary(left: JValue, op: Operator, right: JValue): JValue {
        return when (op) {
            Operator.PLUS -> {
                when {
                    left is JString || right is JString -> JString(stringify(left) + stringify(right))
                    left is JNumber && right is JNumber -> JNumber(left.value + right.value)
                    else -> JNull
                }
            }
            Operator.MINUS -> {
                if (left is JNumber && right is JNumber) JNumber(left.value - right.value) else JNull
            }
            Operator.TIMES -> {
                if (left is JNumber && right is JNumber) JNumber(left.value * right.value) else JNull
            }
            Operator.DIV -> {
                if (left is JNumber && right is JNumber && right.value != 0.0) JNumber(left.value / right.value) else JNull
            }
            Operator.MOD -> {
                if (left is JNumber && right is JNumber && right.value != 0.0) JNumber(left.value % right.value) else JNull
            }
            Operator.EQ -> JBoolean(equalsOp(left, right))
            Operator.NEQ -> JBoolean(!equalsOp(left, right))
            Operator.LT -> {
                if (left is JNumber && right is JNumber) JBoolean(left.value < right.value) else JNull
            }
            Operator.LTE -> {
                if (left is JNumber && right is JNumber) JBoolean(left.value <= right.value) else JNull
            }
            Operator.GT -> {
                if (left is JNumber && right is JNumber) JBoolean(left.value > right.value) else JNull
            }
            Operator.GTE -> {
                if (left is JNumber && right is JNumber) JBoolean(left.value >= right.value) else JNull
            }
        }
    }

    // Equality is value-based for primitive JSON values.
    private fun equalsOp(left: JValue, right: JValue): Boolean = when {
        left is JNumber && right is JNumber -> left.value == right.value
        left is JString && right is JString -> left.value == right.value
        left is JBoolean && right is JBoolean -> left.value == right.value
        left is JNull && right is JNull -> true
        else -> false
    }

    // Truthiness rules keep conditionals predictable in the template language.
    private fun asBoolean(v: JValue): Boolean {
        return when (v) {
            is JBoolean -> v.value
            is JNumber -> v.value != 0.0
            is JString -> v.value.isNotEmpty()
            is JNull -> false
            else -> true
        }
    }

    // Convert structured values into the textual form that gets appended to the output.
    fun stringify(v: JValue): String = when (v) {
        is JString -> v.value
        is JNumber -> {
            val intVal = v.value.toLong()
            if (v.value == intVal.toDouble()) intVal.toString() else v.value.toString()
        }
        is JBoolean -> v.value.toString()
        is JNull -> "null"
        is JObject -> "{...}"
        is JArray -> "[...]"
    }
}