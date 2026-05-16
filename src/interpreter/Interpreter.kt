package project.interpreter

import project.ast.*
import project.json.*

/**
 * Interpreter is the runtime system for the language.
 *
 * It executes an AST against JSON input data and produces output.
 *
 * The runtime maintains:
 * - A persistent global environment (variable bindings that persist across template blocks)
 * - A scope stack for local blocks (loop variables, if blocks, nested scopes)
 * - An input root (the original JSON data structure)
 * - An output buffer (accumulating rendered output)
 *
 * Key semantic features:
 * - Variable resolution: scope stack → global environment → input root
 * - Property access: structured field navigation with clear error messages
 * - Type coercion: JSON values behave sensibly in expressions
 * - Control flow: if/else and for/while loops with proper nested semantics
 * - Break semantics: only valid inside loops, with runtime validation
 */
class Interpreter(private val inputRoot: JValue, val output: StringBuilder) {
    // Global persistent environment: variable bindings that survive across template blocks.
    private val globalEnv = mutableMapOf<String, JValue>()

    // Scope stack for local variable bindings in loops and blocks.
    // Each frame is a temporary local scope (e.g., loop variable or block scope).
    private val scopeStack = mutableListOf<MutableMap<String, JValue>>()

    // Signal used to interrupt loop execution (break semantics).
    class BreakSignal : RuntimeException()

    // Exception for language-level runtime errors (wrapped with context).
    class LanguageError(message: String) : RuntimeException(message)

    // Run a whole script statement-by-statement.
    // Returns the output that was generated during execution.
    fun executeScript(script: Script): String {
        try {
            for (stmt in script.statements) execute(stmt)
        } catch (_: BreakSignal) {
            // A break that escapes the top-level script means 'break' used outside a loop.
            throw LanguageError("semantic error: 'break' used outside of a loop")
        }
        return output.toString()
    }

    // Evaluate a single expression and return its value.
    fun evaluateExpression(expr: Expression): JValue = eval(expr)

    // Convenience API used by TemplateEngine.
    fun run(script: Script) { executeScript(script) }
    fun runExpression(expr: Expression): JValue = evaluateExpression(expr)


    // Execute a statement: assignment, output, control flow, etc.
    private fun execute(stmt: Statement) {
        when (stmt) {
            is Assign -> {
                globalEnv[stmt.name] = eval(stmt.expr)
            }
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
                if (collection !is JArray) {
                    throw LanguageError("type error: 'for' expects an array, got ${collection::class.simpleName}")
                }

                // Create a local scope for the loop variable.
                // This keeps loop variables from persisting after the loop.
                val localScope = mutableMapOf<String, JValue>()
                scopeStack.add(localScope)

                try {
                    for (elem in collection.elements) {
                        localScope[stmt.variable] = elem
                        try {
                            for (s in stmt.body) execute(s)
                        } catch (_: BreakSignal) {
                            // break exits the current for loop
                            break
                        }
                    }
                } finally {
                    // Always remove the local scope when exiting the loop.
                    scopeStack.removeAt(scopeStack.size - 1)
                }
            }
            is While -> {
                try {
                    while (true) {
                        val cond = eval(stmt.condition)
                        if (!asBoolean(cond)) break
                        try {
                            for (s in stmt.body) execute(s)
                        } catch (_: BreakSignal) {
                            // break exits the while loop
                            break
                        }
                    }
                } catch (_: BreakSignal) {
                    // This shouldn't happen if break is caught in the inner loop,
                    // but if it does, convert it to an error.
                    throw LanguageError("semantic error: 'break' used outside of a loop")
                }
            }
            is Break -> {
                // Signal a break to be caught by an enclosing loop.
                throw BreakSignal()
            }
        }
    }

    // Evaluate an expression: produce a JSON value from the AST node.
    private fun eval(expr: Expression): JValue {
        return when (expr) {
            is NumberLiteral -> JNumber(expr.value)
            is StringLiteral -> JString(expr.value)
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
        }
    }

    // Variable resolution: scope stack → global environment → input root.
    // Searches through local scopes first (innermost first), then global, then input root.
    private fun lookupVariable(name: String): JValue {
        // Check local scopes from innermost to outermost.
        for (i in scopeStack.size - 1 downTo 0) {
            if (scopeStack[i].containsKey(name)) {
                return scopeStack[i][name]!!
            }
        }

        // Check global environment.
        if (globalEnv.containsKey(name)) {
            return globalEnv[name]!!
        }

        // Check input root as fallback.
        if (inputRoot is JObject) {
            inputRoot.fields[name]?.let { return it }
        }

        throw LanguageError("undefined variable: '$name'")
    }

    // Property access: object field lookup with clear error messages.
    private fun accessProperty(target: JValue, prop: String): JValue {
        return when (target) {
            is JObject -> {
                target.fields[prop]
                    ?: throw LanguageError("property error: field '$prop' not found in object")
            }
            is JNull -> {
                throw LanguageError("property error: cannot access property '$prop' on null")
            }
            is JArray -> {
                throw LanguageError("property error: cannot access property '$prop' on array (use index access)")
            }
            else -> {
                throw LanguageError("property error: cannot access property '$prop' on ${target::class.simpleName}")
            }
        }
    }

    // Binary operations: arithmetic, equality, comparison.
    // Type coercion is minimal but allows string concatenation with +.
    private fun evalBinary(left: JValue, op: Operator, right: JValue): JValue {
        return when (op) {
            Operator.PLUS -> {
                // String concatenation if either operand is a string.
                // Both operands must be numbers or strings.
                when {
                    left is JString || right is JString -> {
                        JString(stringify(left) + stringify(right))
                    }
                    left is JNumber && right is JNumber -> {
                        JNumber(left.value + right.value)
                    }
                    else -> {
                        throw LanguageError("type error: '+' not supported between ${left::class.simpleName} and ${right::class.simpleName}")
                    }
                }
            }
            Operator.MINUS -> {
                if (left !is JNumber || right !is JNumber) {
                    throw LanguageError("type error: '-' requires two numbers, got ${left::class.simpleName} and ${right::class.simpleName}")
                }
                JNumber(left.value - right.value)
            }
            Operator.TIMES -> {
                if (left !is JNumber || right !is JNumber) {
                    throw LanguageError("type error: '*' requires two numbers, got ${left::class.simpleName} and ${right::class.simpleName}")
                }
                JNumber(left.value * right.value)
            }
            Operator.DIV -> {
                if (left !is JNumber || right !is JNumber) {
                    throw LanguageError("type error: '/' requires two numbers, got ${left::class.simpleName} and ${right::class.simpleName}")
                }
                if (right.value == 0.0) {
                    throw LanguageError("runtime error: division by zero")
                }
                JNumber(left.value / right.value)
            }
            Operator.MOD -> {
                if (left !is JNumber || right !is JNumber) {
                    throw LanguageError("type error: '%' requires two numbers, got ${left::class.simpleName} and ${right::class.simpleName}")
                }
                if (right.value == 0.0) {
                    throw LanguageError("runtime error: modulo by zero")
                }
                JNumber(left.value % right.value)
            }
            Operator.EQ -> JBoolean(equalsOp(left, right))
            Operator.NEQ -> JBoolean(!equalsOp(left, right))
            Operator.LT -> {
                if (left !is JNumber || right !is JNumber) {
                    throw LanguageError("type error: '<' requires two numbers, got ${left::class.simpleName} and ${right::class.simpleName}")
                }
                JBoolean(left.value < right.value)
            }
            Operator.LTE -> {
                if (left !is JNumber || right !is JNumber) {
                    throw LanguageError("type error: '<=' requires two numbers, got ${left::class.simpleName} and ${right::class.simpleName}")
                }
                JBoolean(left.value <= right.value)
            }
            Operator.GT -> {
                if (left !is JNumber || right !is JNumber) {
                    throw LanguageError("type error: '>' requires two numbers, got ${left::class.simpleName} and ${right::class.simpleName}")
                }
                JBoolean(left.value > right.value)
            }
            Operator.GTE -> {
                if (left !is JNumber || right !is JNumber) {
                    throw LanguageError("type error: '>=' requires two numbers, got ${left::class.simpleName} and ${right::class.simpleName}")
                }
                JBoolean(left.value >= right.value)
            }
        }
    }

    // Equality is value-based for primitive types.
    private fun equalsOp(left: JValue, right: JValue): Boolean = when {
        left is JNumber && right is JNumber -> left.value == right.value
        left is JString && right is JString -> left.value == right.value
        left is JBoolean && right is JBoolean -> left.value == right.value
        left is JNull && right is JNull -> true
        else -> false
    }

    // Truthiness conversion for conditionals.
    private fun asBoolean(v: JValue): Boolean {
        return when (v) {
            is JBoolean -> v.value
            is JNumber -> v.value != 0.0
            is JString -> v.value.isNotEmpty()
            is JNull -> false
            else -> true // objects and arrays are truthy in this language
        }
    }

    // Convert JSON values to string for output.
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