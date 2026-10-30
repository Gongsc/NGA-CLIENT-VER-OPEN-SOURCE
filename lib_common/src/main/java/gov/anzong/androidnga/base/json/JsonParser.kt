package gov.anzong.androidnga.base.json

import java.math.BigDecimal
import java.math.BigInteger

/**
 * 宽松 JSON 解析器，接受范围与 fastjson 1.1 对齐。
 *
 * 接受前导零数字、无引号键、单引号字符串、块注释、多余逗号、`\x` 转义与值 `undefined`；拒绝文档后的多余文本、
 * `+1`、`.5`、`NaN`、大写关键字与行注释。对象保持插入顺序。
 */
internal class JsonParser private constructor(private val text: String) {

    private var pos = 0

    private fun readValue(): Any? {
        val c = peek()
        return when (c) {
            '{' -> readObject()
            '[' -> readArray()
            '"', '\'' -> readString(c)
            't' -> readKeyword("true", true)
            'f' -> readKeyword("false", false)
            'n' -> readKeyword("null", null)
            'u' -> readKeyword("undefined", UNDEFINED)
            else -> if (c == '-' || isDigit(c)) readNumber() else throw error("无法识别的值")
        }
    }

    private fun readObject(): NgaJsonObject {
        val obj = NgaJsonObject()
        pos++
        while (true) {
            skipBlank()
            var c = peek()
            if (c == '}') {
                pos++
                return obj
            }
            if (c == ',') {
                pos++
                continue
            }
            val key = if (c == '"' || c == '\'') readString(c) else readIdentifier()
            skipBlank()
            if (peek() != ':') {
                throw error("缺少冒号")
            }
            pos++
            skipBlank()
            val value = readValue()
            if (value !== UNDEFINED) {
                obj[key] = value
            }
            skipBlank()
            c = peek()
            when (c) {
                ',' -> pos++
                '}' -> {
                    pos++
                    return obj
                }
                else -> throw error("对象未正确结束")
            }
        }
    }

    private fun readArray(): NgaJsonArray {
        val array = NgaJsonArray()
        pos++
        while (true) {
            skipBlank()
            var c = peek()
            if (c == ']') {
                pos++
                return array
            }
            if (c == ',') {
                pos++
                continue
            }
            val value = readValue()
            array.add(if (value === UNDEFINED) null else value)
            skipBlank()
            c = peek()
            when (c) {
                ',' -> pos++
                ']' -> {
                    pos++
                    return array
                }
                else -> throw error("数组未正确结束")
            }
        }
    }

    private fun readString(quote: Char): String {
        val builder = StringBuilder()
        pos++
        while (true) {
            if (pos >= text.length) {
                throw error("字符串未结束")
            }
            val c = text[pos++]
            if (c == quote) {
                return builder.toString()
            }
            if (c != '\\') {
                builder.append(c)
                continue
            }
            if (pos >= text.length) {
                throw error("字符串未结束")
            }
            when (val escaped = text[pos++]) {
                '"', '\'', '\\', '/' -> builder.append(escaped)
                'b' -> builder.append('\b')
                'f' -> builder.append('\u000C')
                'n' -> builder.append('\n')
                'r' -> builder.append('\r')
                't' -> builder.append('\t')
                'u' -> builder.append(readHex(4).toChar())
                'x' -> builder.append(readHex(2).toChar())
                else -> throw error("无效的转义字符")
            }
        }
    }

    private fun readHex(length: Int): Int {
        if (pos + length > text.length) {
            throw error("转义字符不完整")
        }
        try {
            val value = Integer.parseInt(text.substring(pos, pos + length), 16)
            pos += length
            return value
        } catch (e: NumberFormatException) {
            throw error("无效的十六进制转义")
        }
    }

    /** 无引号键：纯数字，或以字母、下划线开头并由字母、数字、下划线组成。 */
    private fun readIdentifier(): String {
        val start = pos
        val first = peek()
        if (isDigit(first)) {
            skipDigits()
        } else if (Character.isLetter(first) || first == '_') {
            while (pos < text.length && (Character.isLetterOrDigit(text[pos]) || text[pos] == '_')) {
                pos++
            }
        } else {
            throw error("无效的键名")
        }
        return text.substring(start, pos)
    }

    private fun readKeyword(keyword: String, value: Any?): Any? {
        if (!text.startsWith(keyword, pos)) {
            throw error("无法识别的值")
        }
        pos += keyword.length
        if (pos < text.length && isIdentifierPart(text[pos])) {
            throw error("无法识别的值")
        }
        return value
    }

    private fun readNumber(): Any {
        val start = pos
        if (peek() == '-') {
            pos++
        }
        val digits = skipDigits()
        if (digits == 0) {
            throw error("数字格式错误")
        }
        var fraction = false
        var exponent = false
        if (pos < text.length && text[pos] == '.') {
            pos++
            fraction = true
            if (skipDigits() == 0) {
                throw error("数字格式错误")
            }
        }
        if (pos < text.length && (text[pos] == 'e' || text[pos] == 'E')) {
            pos++
            exponent = true
            if (pos < text.length && (text[pos] == '+' || text[pos] == '-')) {
                pos++
            }
            if (skipDigits() == 0) {
                throw error("数字格式错误")
            }
        }
        if (pos < text.length && (isIdentifierPart(text[pos]) || text[pos] == '.')) {
            throw error("数字格式错误")
        }
        val number = text.substring(start, pos)
        if (exponent) {
            return java.lang.Double.parseDouble(number)
        }
        if (fraction) {
            return BigDecimal(number)
        }
        // fastjson 按位数而非数值区分：9 位以内为 Integer，否则为 Long 或 BigInteger。
        if (digits < 10) {
            return Integer.parseInt(number)
        }
        val value = BigInteger(number)
        return if (value.bitLength() < 64) value.toLong() else value
    }

    private fun skipDigits(): Int {
        val start = pos
        while (pos < text.length && isDigit(text[pos])) {
            pos++
        }
        return pos - start
    }

    private fun skipBlank() {
        while (pos < text.length) {
            val c = text[pos]
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\u000C' || c == '\b') {
                pos++
            } else if (c == '/' && text.startsWith("/*", pos)) {
                val end = text.indexOf("*/", pos + 2)
                if (end < 0) {
                    throw error("注释未结束")
                }
                pos = end + 2
            } else {
                return
            }
        }
    }

    private fun peek(): Char {
        if (pos >= text.length) {
            throw error("文档意外结束")
        }
        return text[pos]
    }

    private fun error(message: String): NgaJsonException = NgaJsonException("$message，位置 $pos")

    companion object {

        /** 对象中值为 undefined 的键被丢弃，数组中视为 null。 */
        private val UNDEFINED = Any()

        /** 空白文档与 `null` 返回 null。 */
        fun parse(text: String?): Any? {
            if (text == null) {
                return null
            }
            val parser = JsonParser(text)
            parser.skipBlank()
            if (parser.pos >= text.length) {
                return null
            }
            val value = parser.readValue()
            parser.skipBlank()
            if (parser.pos < text.length) {
                throw parser.error("文档末尾存在多余内容")
            }
            return if (value === UNDEFINED) null else value
        }

        private fun isDigit(c: Char): Boolean = c in '0'..'9'

        /** 关键字与数字之后不能紧跟的字符。 */
        private fun isIdentifierPart(c: Char): Boolean =
            c in 'a'..'z' || c in 'A'..'Z' || c == '_' || c == '$' || isDigit(c)
    }
}
