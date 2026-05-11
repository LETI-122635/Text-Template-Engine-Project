package project.json

// The tool uses its own JSON model so it does not depend on an external parser library.
sealed interface JValue

// Object, array, string, number, boolean, and null cover the JSON types used by templates.
data class JObject(val fields: Map<String, JValue>) : JValue

data class JArray(val elements: List<JValue>) : JValue

data class JString(val value: String) : JValue

data class JNumber(val value: Double) : JValue

data class JBoolean(val value: Boolean) : JValue

object JNull : JValue

// A minimal recursive-descent parser for the subset of JSON needed by the project.
class JsonParser(private val src: String) {
    private var pos = 0
    private val len = src.length

    // Parse one complete JSON value from the input text.
    fun parse(): JValue {
        skipWhitespace()
        val v = parseValue()
        skipWhitespace()
        return v
    }

    // Dispatch based on the next token-like character.
    private fun parseValue(): JValue {
        skipWhitespace()
        if (pos >= len) return JNull
        return when (src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JString(parseString())
            't' -> parseLiteral("true", JBoolean(true))
            'f' -> parseLiteral("false", JBoolean(false))
            'n' -> parseLiteral("null", JNull)
            else -> JNumber(parseNumber().toDouble())
        }
    }

    // Recognize fixed literals such as true, false, and null.
    private fun parseLiteral(name: String, value: JValue): JValue {
        if (src.regionMatches(pos, name, 0, name.length)) {
            pos += name.length
            return value
        }
        error("Invalid token at $pos")
    }

    // Numbers are parsed manually so we do not depend on any JSON library.
    private fun parseNumber(): Number {
        val start = pos
        if (pos < len && src[pos] == '-') pos++
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

    // Strings handle the standard escape sequences used by the templates.
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
                        if (pos + 4 <= len) {
                            val hex = src.substring(pos, pos + 4)
                            sb.append(hex.toInt(16).toChar())
                            pos += 4
                        }
                    }
                    else -> sb.append(e)
                }
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    // Arrays are parsed left-to-right and stored as a list of JValue nodes.
    private fun parseArray(): JArray {
        val list = mutableListOf<JValue>()
        expect('[')
        skipWhitespace()
        if (peek() == ']') { pos++; return JArray(list) }
        while (true) {
            skipWhitespace()
            list.add(parseValue())
            skipWhitespace()
            val c = peek()
            if (c == ',') { pos++; continue }
            if (c == ']') { pos++; break }
            error("Expected ',' or ']' in array at $pos")
        }
        return JArray(list)
    }

    // Objects map string keys to parsed JSON values.
    private fun parseObject(): JObject {
        val map = mutableMapOf<String, JValue>()
        expect('{')
        skipWhitespace()
        if (peek() == '}') { pos++; return JObject(map) }
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
        return JObject(map)
    }

    // Small helpers keep the parser logic easy to read.
    private fun peek(): Char = if (pos < len) src[pos] else '\u0000'

    private fun expect(ch: Char) {
        if (pos < len && src[pos] == ch) { pos++ } else error("Expected '$ch' at $pos")
    }

    private fun skipWhitespace() {
        while (pos < len && src[pos].isWhitespace()) pos++
    }
}

// Convenience wrapper used by the rest of the application.
fun parseJson(text: String): JValue = JsonParser(text).parse()
