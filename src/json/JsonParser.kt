package project.json

// Minimal JSON parser (no external libraries). Supports objects, arrays, strings, numbers, booleans and null.
class JsonParser(private val src: String) {
    private var pos = 0
    private val len = src.length

    fun parse(): Any? {
        skipWhitespace()
        val v = parseValue()
        skipWhitespace()
        return v
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= len) return null
        return when (src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> parseLiteral("true", true)
            'f' -> parseLiteral("false", false)
            'n' -> parseLiteral("null", null)
            else -> parseNumber()
        }
    }

    private fun parseLiteral(name: String, value: Any?): Any? {
        if (src.regionMatches(pos, name, 0, name.length)) {
            pos += name.length
            return value
        }
        error("Invalid token at $pos")
    }

    private fun parseNumber(): Number {
        val start = pos
        if (src[pos] == '-') pos++
        while (pos < len && src[pos].isDigit()) pos++
        var isDouble = false
        if (pos < len && src[pos] == '.') {
            isDouble = true
            pos++
            while (pos < len && src[pos].isDigit()) pos++
        }
        if (pos < len && (src[pos] == 'e' || src[pos] == 'E')) {
            isDouble = true
            pos++
            if (pos < len && (src[pos] == '+' || src[pos] == '-')) pos++
            while (pos < len && src[pos].isDigit()) pos++
        }
        val token = src.substring(start, pos)
        return if (isDouble) token.toDouble() else token.toLong()
    }

    private fun parseString(): String {
        val sb = StringBuilder()
        if (src[pos] != '"') error("Expected '\"' at $pos")
        pos++
        while (pos < len) {
            val c = src[pos++]
            if (c == '"') break
            if (c == '\\') {
                if (pos >= len) break
                val e = src[pos++]
                when (e) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        val hex = src.substring(pos, pos + 4)
                        sb.append(hex.toInt(16).toChar())
                        pos += 4
                    }
                    else -> sb.append(e)
                }
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun parseArray(): List<Any?> {
        val list = mutableListOf<Any?>()
        expect('[')
        skipWhitespace()
        if (peek() == ']') { pos++; return list }
        while (true) {
            skipWhitespace()
            list.add(parseValue())
            skipWhitespace()
            val c = peek()
            if (c == ',') { pos++; continue }
            if (c == ']') { pos++; break }
            error("Expected ',' or ']' in array at $pos")
        }
        return list
    }

    private fun parseObject(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        expect('{')
        skipWhitespace()
        if (peek() == '}') { pos++; return map }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            if (peek() != ':') error("Expected ':' after object key at $pos")
            pos++
            skipWhitespace()
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            val c = peek()
            if (c == ',') { pos++; continue }
            if (c == '}') { pos++; break }
            error("Expected ',' or '}' in object at $pos")
        }
        return map
    }

    private fun peek(): Char = if (pos < len) src[pos] else '\u0000'

    private fun expect(ch: Char) {
        if (pos < len && src[pos] == ch) { pos++ } else error("Expected '$ch' at $pos")
    }

    private fun skipWhitespace() {
        while (pos < len && src[pos].isWhitespace()) pos++
    }
}

fun parseJson(text: String): Any? = JsonParser(text).parse()


