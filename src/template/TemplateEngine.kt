package project.template

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import project.GrammarLexer
import project.GrammarParser
import project.ast.Expression
import project.ast.Script
import project.interpreter.Interpreter
import project.parser.toAst

object TemplateEngine {
    private val rcOpen = "<rc>"
    private val rcClose = "<rc>"

    fun render(template: String, input: Any?): String {
        val out = StringBuilder()
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
            processCode(code, input, out)
            idx = close + rcClose.length
        }
        return out.toString()
    }

    private fun processCode(code: String, input: Any?, out: StringBuilder) {
        if (code.contains("\n") || code.contains('{') || code.contains('}') || code.contains("if ") || code.contains("for ")) {
            // script block: ensure statements end with semicolons where needed
            val fixed = preprocessScript(code)
            val script = parseScript(fixed)
            val interpreter = Interpreter(input, out)
            interpreter.run(script)
        } else {
            // expression mode
            val expr = parseExpression(code)
            val interpreter = Interpreter(input, out)
            val v = interpreter.runExpression(expr)
            out.append(interpreter.stringify(v))
        }
    }

    private fun preprocessScript(code: String): String {
        val sb = StringBuilder()
        val lines = code.lines()
        for (line in lines) {
            val t = line.trimEnd()
            if (t.isBlank()) { sb.append(line).append('\n'); continue }
            val trimmed = t.trimStart()
            if (trimmed.startsWith("if ") || trimmed.startsWith("for ") || trimmed.startsWith("}") || trimmed.endsWith("{") || trimmed == "else") {
                sb.append(line).append('\n')
            } else {
                if (t.endsWith(";")) sb.append(line).append('\n') else sb.append(line).append(";").append('\n')
            }
        }
        return sb.toString()
    }

    private fun parseScript(code: String): Script {
        val lexer = GrammarLexer(CharStreams.fromString(code))
        val tokens = CommonTokenStream(lexer)
        val parser = GrammarParser(tokens)
        val tree = parser.script()
        return tree.toAst()
    }

    private fun parseExpression(code: String): Expression {
        val lexer = GrammarLexer(CharStreams.fromString(code))
        val tokens = CommonTokenStream(lexer)
        val parser = GrammarParser(tokens)
        val tree = parser.expression()
        return tree.toAst()
    }
}