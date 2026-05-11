package project.parser

import project.GrammarParser.*
import project.ast.*

// Entry point: a script becomes a flat list of AST statements.
fun ScriptContext.toAst(): Script =
    Script(statement().map { it.toAst() })

// Dispatch each parsed statement to the corresponding AST node.
fun StatementContext.toAst(): Statement {
    return when {
        assignment() != null -> assignment().toAst()
        printStmt() != null -> printStmt().toAst()
        ifStmt() != null -> ifStmt().toAst()
        forStmt() != null -> forStmt().toAst()
        else -> error("Unknown statement: $text")
    }
}

fun AssignmentContext.toAst(): Assign =
    Assign(ID().text, expression().toAst())

fun PrintStmtContext.toAst(): Output =
    Output(expression().toAst())

// if/else keeps block contents as lists of statements for easy execution.
fun IfStmtContext.toAst(): If {
    val condition = expression().toAst()
    val thenBlock = block(0).statement().map { it.toAst() }
    val elseBlock = if (block().size > 1) block(1).statement().map { it.toAst() } else emptyList()
    return If(condition, thenBlock, elseBlock)
}

// for stores the loop variable, the collection expression, and the body statements.
fun ForStmtContext.toAst(): For =
    For(ID().text, expression().toAst(), block().statement().map { it.toAst() })

// Blocks are converted directly into statement lists so nested execution stays simple.
fun BlockContext.toAst(): List<Statement> =
    statement().map { it.toAst() }

// Expression precedence is handled top-down, from equality down to atoms.
fun ExpressionContext.toAst(): Expression =
    equality().toAst()

// Equality is left-associative and supports == and !=.
fun EqualityContext.toAst(): Expression {
    val comparisons = comparison()
    var expr = comparisons[0].toAst()
    var compIdx = 0
    for (i in 1 until childCount step 2) {
        val opChild = getChild(i)
        compIdx++
        val op = when (opChild.text) {
            "==" -> Operator.EQ
            "!=" -> Operator.NEQ
            else -> continue
        }
        expr = BinaryOp(expr, op, comparisons[compIdx].toAst())
    }
    return expr
}

// Comparison chains relational operators such as <, <=, >, and >=.
fun ComparisonContext.toAst(): Expression {
    val additives = additive()
    var expr = additives[0].toAst()
    var addIdx = 0
    for (i in 1 until childCount step 2) {
        val opChild = getChild(i)
        addIdx++
        val op = when (opChild.text) {
            "<" -> Operator.LT
            "<=" -> Operator.LTE
            ">" -> Operator.GT
            ">=" -> Operator.GTE
            else -> continue
        }
        expr = BinaryOp(expr, op, additives[addIdx].toAst())
    }
    return expr
}

// Additive handles + and - over multiplicative subexpressions.
fun AdditiveContext.toAst(): Expression {
    val multiplicatives = multiplicative()
    var expr = multiplicatives[0].toAst()
    var multIdx = 0
    for (i in 1 until childCount step 2) {
        val opChild = getChild(i)
        multIdx++
        val op = when (opChild.text) {
            "+" -> Operator.PLUS
            "-" -> Operator.MINUS
            else -> continue
        }
        expr = BinaryOp(expr, op, multiplicatives[multIdx].toAst())
    }
    return expr
}

// Multiplicative handles *, /, and % over atoms.
fun MultiplicativeContext.toAst(): Expression {
    val atoms = atom()
    var expr = atoms[0].toAst()
    var atomIdx = 0
    for (i in 1 until childCount step 2) {
        val opChild = getChild(i)
        atomIdx++
        val op = when (opChild.text) {
            "*" -> Operator.TIMES
            "/" -> Operator.DIV
            "%" -> Operator.MOD
            else -> continue
        }
        expr = BinaryOp(expr, op, atoms[atomIdx].toAst())
    }
    return expr
}

// Atoms cover literals, names, property chains, and grouped expressions.
fun AtomContext.toAst(): Expression {
    val txt = text
    return when {
        INT() != null -> NumberLiteral(INT().text.toDouble())
        txt.startsWith('"') && txt.endsWith('"') -> StringLiteral(unescapeString(txt))
        ID().isNotEmpty() -> {
            if (ID().size > 1) {
                var expr: Expression = Variable(ID(0).text)
                for (i in 1 until ID().size) {
                    expr = PropertyAccess(expr, ID(i).text)
                }
                expr
            } else {
                Variable(ID(0).text)
            }
        }
        expression() != null -> expression().toAst()
        else -> error("Unknown atom: $text")
    }
}

// Simple escape decoding for template string literals.
private fun unescapeString(quoted: String): String {
    if (quoted.length < 2) return ""
    val content = quoted.substring(1, quoted.length - 1)
    val sb = StringBuilder()
    var i = 0
    while (i < content.length) {
        if (content[i] == '\\' && i + 1 < content.length) {
            when (content[i + 1]) {
                'n' -> sb.append('\n')
                't' -> sb.append('\t')
                'r' -> sb.append('\r')
                '\\' -> sb.append('\\')
                '"' -> sb.append('"')
                else -> sb.append(content[i + 1])
            }
            i += 2
        } else {
            sb.append(content[i])
            i++
        }
    }
    return sb.toString()
}