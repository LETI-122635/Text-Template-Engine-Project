package project.template

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import project.GrammarLexer
import project.GrammarParser
import project.ast.Expression
import project.ast.Script
import project.interpreter.Interpreter
import project.json.JValue
import project.parser.toAst

// The renderer scans plain text and <rc>...<cr> blocks, evaluating each block in order.
object TemplateEngine {
    // Opening and closing tags for executable template regions.
    private val rcOpen = "<rc>"
    private val rcClose = "<cr>"

    // Render the full template into a single output string.
    fun render(template: String, input: JValue): String {
        val out = StringBuilder()
        val interpreter = Interpreter(input, out)
        var idx = 0
        while (true) {
            val open = template.indexOf(rcOpen, idx)
            if (open == -1) {
                out.append(template.substring(idx))
                break
            }
            out.append(template.substring(idx, open))
            val close = template.indexOf(rcClose, open + rcOpen.length)
            if (close == -1) error("Unclosed <rc> marker")
            val code = template.substring(open + rcOpen.length, close).trim()
            if (code.isNotEmpty()) {
                processCode(code, interpreter, out)
            }
            idx = close + rcClose.length
        }
        return out.toString()
    }

    // Try to interpret a block as a full script first, then as a plain expression.
    private fun processCode(code: String, interpreter: Interpreter, out: StringBuilder) {
        val script = tryParseScript(code)
        if (script != null) {
            interpreter.run(script)
            return
        }

        val expr = tryParseExpression(code)
        if (expr != null) {
            val v = interpreter.runExpression(expr)
            out.append(interpreter.stringify(v))
            return
        }

        System.err.println("DEBUG: Could not parse code block: '$code'")
    }

    // Parsing as a script lets the template support assignments, loops, and conditionals.
    private fun tryParseScript(code: String): Script? {
        val lexer = GrammarLexer(CharStreams.fromString(code))
        lexer.removeErrorListeners()
        lexer.addErrorListener(DebugErrorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = GrammarParser(tokens)
        // make parser fail fast
        parser.removeErrorListeners()
        parser.addErrorListener(DebugErrorListener)
        parser.errorHandler = BailErrorStrategy()
        return try {
            val tree = parser.script()
            tree.toAst()
        } catch (_: Exception) {
            null
        }
    }

    // If the block is just an expression, render its value directly into the template output.
    private fun tryParseExpression(code: String): Expression? {
        val lexer = GrammarLexer(CharStreams.fromString(code))
        lexer.removeErrorListeners()
        lexer.addErrorListener(DebugErrorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = GrammarParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(DebugErrorListener)
        parser.errorHandler = BailErrorStrategy()
        return try {
            val tree = parser.expression()
            // ensure whole input consumed
            val next = tokens.LA(1)
            if (next != Token.EOF) return null
            tree.toAst()
        } catch (_: Exception) {
            null
        }
    }

    // Lexer/parser errors are reported as debug so broken template fragments are visible.
    private object DebugErrorListener : BaseErrorListener() {
        override fun syntaxError(recognizer: org.antlr.v4.runtime.Recognizer<*, *>?, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?) {
            System.err.println("DEBUG: $msg")
            throw ParseCancellationException(msg)
        }
    }
}