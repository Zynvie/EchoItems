package cat.emir.echoitems.text

import kotlin.math.round

/**
 * Syntax:
 *   ${input, string,  key='value with # in it', key2='...', else='...'}
 *   ${input, long,    1='input is #', 2='...', else='# is input'}
 *   ${input, double,  1.5='...', else='...'}
 *   ${input, boolean, true='...', false='...', else='...'}
 *
 */
object MessageFormat {

    /**
     * @param resolver returns the raw value for input
     */
    fun format(pattern: String, resolver: (input: String) -> Any?): String {
        val out = StringBuilder()
        var i = 0
        val len = pattern.length
        while (i < len) {
            val c = pattern[i]
            if (c == '$' && i + 1 < len && pattern[i + 1] == '{') {
                val exprStart = i + 2
                val exprEnd = findClosingBrace(pattern, exprStart)
                if (exprEnd < 0) {
                    out.append(pattern, i, len) // unterminated -> copy verbatim rather than throw
                    break
                }
                out.append(evaluate(pattern.substring(exprStart, exprEnd), resolver))
                i = exprEnd + 1
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private fun findClosingBrace(s: String, start: Int): Int {
        var inQuote = false
        for (i in start until s.length) {
            val c = s[i]
            if (c == '\'') inQuote = !inQuote
            else if (c == '}' && !inQuote) return i
        }
        return -1
    }

    private fun evaluate(expr: String, resolver: (String) -> Any?): String {
        val cur = Cursor(expr)

        val modifierName = cur.readToken()
        cur.expect(',')
        val type = cur.readToken().lowercase()
        cur.expect(',')

        val cases = LinkedHashMap<String, String>()
        while (cur.hasMore()) {
            val key = cur.readToken()
            cur.expect('=')
            val value = cur.readValue()
            cases[key] = value
            cur.skipWs()
            if (cur.peek() == ',') cur.pos++
        }

        val rawValue = resolver(modifierName)
        val matched = matchCase(type, rawValue, cases) ?: return ""
        return matched.replace("#", formatValue(type, rawValue))
    }

    private fun matchCase(type: String, rawValue: Any?, cases: Map<String, String>): String? =
        when (type) {
            "string" -> {
                val s = rawValue?.toString()
                if (s != null && cases.containsKey(s)) cases[s] else cases["else"]
            }
            "integer", "long" -> {
                val l = toLong(rawValue)
                l?.let { v ->
                    cases.entries.firstOrNull { (k, _) -> k != "else" && k.toLongOrNull() == v }?.value
                } ?: cases["else"]
            }
            "float", "double" -> {
                val d = toDouble(rawValue)
                d?.let { v ->
                    cases.entries.firstOrNull { (k, _) -> k != "else" && k.toDoubleOrNull() == v }?.value
                } ?: cases["else"]
            }
            "boolean" -> {
                val b = toBoolean(rawValue)
                if (b != null && cases.containsKey(b.toString())) cases[b.toString()] else cases["else"]
            }
            else -> cases["else"]
        }

    private fun formatValue(type: String, rawValue: Any?): String {
        if (rawValue == null) return ""
        return when (type) {
            "integer", "long" -> toLong(rawValue)?.toString() ?: rawValue.toString()
            "float", "double" -> {
                val d = toDouble(rawValue) ?: return rawValue.toString()
                if (d == round(d) && !d.isInfinite()) d.toLong().toString() else d.toString()
            }
            "boolean" -> toBoolean(rawValue)?.toString() ?: rawValue.toString()
            else -> rawValue.toString()
        }
    }

    private fun toLong(v: Any?): Long? = when (v) {
        is Number -> v.toLong()
        null -> null
        else -> v.toString().trim().toLongOrNull()
    }

    private fun toDouble(v: Any?): Double? = when (v) {
        is Number -> v.toDouble()
        null -> null
        else -> v.toString().trim().toDoubleOrNull()
    }

    private fun toBoolean(v: Any?): Boolean? = when (v) {
        is Boolean -> v
        null -> null
        else -> when (v.toString().trim().lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    private class Cursor(val s: String) {
        var pos = 0

        fun hasMore(): Boolean {
            skipWs()
            return pos < s.length
        }

        fun peek(): Char = if (pos < s.length) s[pos] else '\u0000'

        fun skipWs() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        fun expect(c: Char) {
            skipWs()
            require(pos < s.length && s[pos] == c) { "Expected '$c' at position $pos in: $s" }
            pos++
        }

        fun readToken(): String {
            skipWs()
            if (peek() == '\'') return readQuoted()
            val start = pos
            while (pos < s.length) {
                val c = s[pos]
                if (c.isWhitespace() || c == ',' || c == '=') break
                pos++
            }
            return s.substring(start, pos)
        }

        fun readValue(): String {
            skipWs()
            if (peek() == '\'') return readQuoted()
            val start = pos
            while (pos < s.length && s[pos] != ',') pos++
            return s.substring(start, pos).trim()
        }

        private fun readQuoted(): String {
            pos++ // opening '
            val start = pos
            while (pos < s.length && s[pos] != '\'') pos++
            val value = s.substring(start, pos)
            if (pos < s.length) pos++ // closing '
            return value
        }
    }
}