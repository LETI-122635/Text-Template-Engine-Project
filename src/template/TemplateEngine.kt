package project.template

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import gen_parser.GrammarLexer
import gen_parser.GrammarParser
import project.ast.Expression
import project.ast.Script
import project.interpreter.Interpreter
import project.json.JValue
import parser.toAst

/**
 * TemplateEngine is a FRONTEND for the scripting language.
 *
 * It is responsible ONLY for:
 * - Splitting text and code blocks (delimited by <rc>...<cr>)
 * - Delegating code execution to the Language runtime
 * - Collecting output into a single result
 *
 * It contains NO parsing logic, NO semantic logic.
 * It is purely a rendering adapter.
 */
object TemplateEngine {
    // Tag delimiters that mark executable code regions in the template.
    private val rcOpen = "<rc>"
    private val rcClose = "<cr>"

    /**
     * Render a template by executing embedded code blocks.
     *
     * The template can contain:
     * - Plain text (passed through to output)
     * - <rc>...code...<cr> blocks (executed as language code)
     *
     * Returns the complete rendered result.
     */
    fun render(template: String, input: JValue): String {
        val out = StringBuilder()
        val interpreter = Interpreter(input, out)
        var idx = 0

        while (true) {
            val open = template.indexOf(rcOpen, idx)
            if (open == -1) {
                // No more code blocks; append remaining text.
                out.append(template.substring(idx))
                break
            }

            // Append text before this code block.
            out.append(template.substring(idx, open))

            val close = template.indexOf(rcClose, open + rcOpen.length)
            if (close == -1) error("Unclosed <rc> tag in template")

            val code = template.substring(open + rcOpen.length, close).trim()
            if (code.isNotEmpty()) {
                executeCodeBlock(code, interpreter, out)
            }

            idx = close + rcClose.length
        }

        return out.toString()
    }

    /**
     * Execute a single code block from the template.
     *
     * The block may be:
     * - A full statement/script (assignments, loops, conditionals, prints)
     * - A simple expression (evaluated and rendered)
     *
     * Strategy: Try script parse first (since for/while/if/assignment are more likely),
     * then fall back to expression parse. This avoids heuristics while keeping parsing simple.
     */
    private fun executeCodeBlock(code: String, interpreter: Interpreter, out: StringBuilder) {
        // Try to compile as a full script first (handles statements).
        val script = tryParseScript(code)
        if (script != null) {
            try {
                interpreter.run(script)
            } catch (e: Exception) {
                // Runtime execution error - propagate with context.
                val errorMsg = e.message ?: "unknown error"
                throw RuntimeException("Error executing code block: $errorMsg")
            }
            return
        }

        // If not a script, try as an expression (simpler, typically expressions).
        val expr = tryParseExpression(code)
        if (expr != null) {
            try {
                val value = interpreter.runExpression(expr)
                out.append(interpreter.stringify(value))
            } catch (e: Exception) {
                // Runtime evaluation error.
                val errorMsg = e.message ?: "unknown error"
                throw RuntimeException("Error evaluating expression: $errorMsg")
            }
            return
        }

        // If neither script nor expression parsed successfully, report the failure.
        throw RuntimeException("Code block could not be parsed as a script or expression: '$code'")
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