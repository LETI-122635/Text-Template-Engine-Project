package project.parser

import org.antlr.v4.runtime.ParserRuleContext
import project.GrammarParser.*
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
import project.ast.Variable
import project.ast.While


fun ParserRuleContext.toRange() = Triple(this.start.startIndex, this.stop.stopIndex, this.start.line)

fun ScriptContext.toAst(): Script =
    Script(statement().map { it.toAst() })

fun StatementContext.toAst(): Statement {
    return when {
        assignment() != null -> assignment().toAst()
        printStmt() != null -> printStmt().toAst()
        ifStmt() != null -> ifStmt().toAst()
        forStmt() != null -> forStmt().toAst()
        block() != null -> Block(block().toAst())
        breakStmt() != null -> breakStmt().toAst()
        whileStmt() != null -> whileStmt().toAst()
        else -> error("Unknown statement: $text")
    }
}

fun AssignmentContext.toAst(): Assign =
    Assign(ID().text, expression().toAst())

fun PrintStmtContext.toAst(): Output =
    Output(expression().toAst())

fun IfStmtContext.toAst(): If {
    val condition = expression().toAst()
    val thenBlock = block(0).statement().map { it.toAst() }
    val elseBlock = if (block().size > 1) block(1).statement().map { it.toAst() } else emptyList()
    return If(condition, thenBlock, elseBlock)
}

fun ForStmtContext.toAst(): For =
    For(ID().text, expression().toAst(), block().statement().map { it.toAst() })

fun WhileStmtContext.toAst(): While =
    While(expression().toAst(), block().statement().map { it.toAst() })

fun BreakStmtContext.toAst(): Statement = Break

fun BlockContext.toAst(): List<Statement> =
    statement().map { it.toAst() }

fun ExpressionContext.toAst(): Expression =
    equality().toAst()

fun EqualityContext.toAst(): Expression {
    val comparisons = comparison()
    var expr = comparisons[0].toAst()

    for (i in 1 until comparisons.size) {
        val op = if (EQUAL().size > i - 1 && EQUAL(i - 1) != null) Operator.EQ else Operator.NEQ
        expr = BinaryOp(expr, op, comparisons[i].toAst())
    }
    return expr
}

fun ComparisonContext.toAst(): Expression {
    val additives = additive()
    var expr = additives[0].toAst()

    for (i in 1 until additives.size) {
        val op = when {
            LESS().size > i - 1 && LESS(i - 1) != null -> Operator.LT
            LESS_EQUAL().size > i - 1 && LESS_EQUAL(i - 1) != null -> Operator.LTE
            GREATER().size > i - 1 && GREATER(i - 1) != null -> Operator.GT
            GREATER_EQUAL().size > i - 1 && GREATER_EQUAL(i - 1) != null -> Operator.GTE
            else -> error("Unknown comparison operator")
        }
        expr = BinaryOp(expr, op, additives[i].toAst())
    }
    return expr
}

fun AdditiveContext.toAst(): Expression {
    val multiplicatives = multiplicative()
    var expr = multiplicatives[0].toAst()

    for (i in 1 until multiplicatives.size) {
        val op = if (PLUS().size > i - 1 && PLUS(i - 1) != null) Operator.PLUS else Operator.MINUS
        expr = BinaryOp(expr, op, multiplicatives[i].toAst())
    }
    return expr
}

fun MultiplicativeContext.toAst(): Expression {
    val atoms = atom()
    var expr = atoms[0].toAst()

    for (i in 1 until atoms.size) {
        val op = when {
            TIMES().size > i - 1 && TIMES(i - 1) != null -> Operator.TIMES
            DIV().size > i - 1 && DIV(i - 1) != null -> Operator.DIV
            MOD().size > i - 1 && MOD(i - 1) != null -> Operator.MOD
            else -> error("Unknown multiplicative operator")
        }
        expr = BinaryOp(expr, op, atoms[i].toAst())
    }
    return expr
}

fun AtomContext.toAst(): Expression =
    when {
        INT() != null -> NumberLiteral(INT().text.toInt())
        // STRING token handling requires regenerating ANTLR sources to include STRING; skipped for now
        ID().isNotEmpty() -> {
            // Handle property access (e.g., user.name)
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

// Note: string literal handling requires the grammar and generated parser to include STRING token.

class Parser2Ast {
    fun build(tree: ScriptContext): Script = tree.toAst()
}