package gov.anzong.androidnga.base.json

import java.util.Locale

/**
 * JSON 序列化，输出格式与 fastjson 1.1 一致。
 *
 * 省略值为 null 的键；不转义中文与 `/`；控制字符、DEL 与 U+2028 写作大写的 `\uXXXX`；
 * 整数值的浮点数去掉末尾 ".0"；实体按属性名字母顺序输出。
 */
internal object JsonWriter {

    fun write(value: Any?): String = StringBuilder().also { append(it, value) }.toString()

    private fun append(out: StringBuilder, value: Any?) {
        when {
            value == null -> out.append("null")
            value is CharSequence || value is Char -> appendString(out, value.toString())
            value is Boolean -> out.append(value)
            value is Double || value is Float -> appendFloating(out, (value as Number).toDouble(), value.toString())
            value is Number -> out.append(value)
            value is Map<*, *> -> appendMap(out, value)
            value is Collection<*> -> appendArray(out, value.toTypedArray())
            value.javaClass.isArray ->
                appendArray(out, Array(java.lang.reflect.Array.getLength(value)) { java.lang.reflect.Array.get(value, it) })
            value is Enum<*> -> appendString(out, value.name)
            else -> appendMap(out, JsonBeans.toMap(value))
        }
    }

    private fun appendFloating(out: StringBuilder, number: Double, text: String) {
        when {
            number.isNaN() || number.isInfinite() -> out.append("null")
            text.endsWith(".0") -> out.append(text, 0, text.length - 2)
            else -> out.append(text)
        }
    }

    private fun appendMap(out: StringBuilder, map: Map<*, *>) {
        out.append('{')
        var first = true
        for ((key, value) in map) {
            if (value == null) {
                continue
            }
            if (!first) {
                out.append(',')
            }
            first = false
            appendString(out, key.toString())
            out.append(':')
            append(out, value)
        }
        out.append('}')
    }

    private fun appendArray(out: StringBuilder, items: Array<*>) {
        out.append('[')
        items.forEachIndexed { i, item ->
            if (i > 0) {
                out.append(',')
            }
            append(out, item)
        }
        out.append(']')
    }

    private fun appendString(out: StringBuilder, text: String) {
        out.append('"')
        for (c in text) {
            when (c) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                else ->
                    if (c.code < 0x20 || c.code == 0x7F || c == '\u2028') {
                        out.append(String.format(Locale.ROOT, "\\u%04X", c.code))
                    } else {
                        out.append(c)
                    }
            }
        }
        out.append('"')
    }
}
