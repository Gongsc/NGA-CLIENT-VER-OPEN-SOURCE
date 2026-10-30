package gov.anzong.androidnga.network.article

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.util.javaTrim

/** 只读取网页启动脚本中的字面量；不执行 JavaScript。 */
internal object ArticleScriptData {

    /** 脚本中每次调用 [name] 的参数原文，按出现顺序。 */
    @JvmStatic
    fun calls(script: String, name: String): List<List<String>> {
        val result = ArrayList<List<String>>()
        var offset = script.indexOf("$name(")
        while (offset >= 0) {
            val start = offset + name.length + 1
            var depth = 0
            var quote = 0.toChar()
            var escaped = false
            val args = ArrayList<String>()
            var part = start
            offset = start
            while (offset < script.length) {
                val c = script[offset]
                if (quote.code != 0) {
                    if (escaped) {
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == quote) {
                        quote = 0.toChar()
                    }
                } else if (c == '\'' || c == '"' || c == '`') {
                    quote = c
                } else if (c == '(' || c == '[' || c == '{') {
                    depth++
                } else if (c == ')' && depth == 0) {
                    args.add(script.substring(part, offset).javaTrim())
                    result.add(args)
                    offset++
                    break
                } else if (c == ')' || c == ']' || c == '}') {
                    depth--
                } else if (c == ',' && depth == 0) {
                    args.add(script.substring(part, offset).javaTrim())
                    part = offset + 1
                }
                offset++
            }
            offset = script.indexOf("$name(", offset)
        }
        return result
    }

    /** 第 [index] 个参数的值：字符串字面量解码，null 与 undefined 为空串。 */
    @JvmStatic
    fun literal(args: List<String>, index: Int): String {
        if (index >= args.size) return ""
        val value = args[index]
        if ("null" == value || "undefined" == value) return ""
        if (value.startsWith("\"") || value.startsWith("'")) {
            return NgaJson.parse(value)?.toString() ?: ""
        }
        return value
    }

    @JvmStatic
    fun number(args: List<String>, index: Int): Int = literal(args, index).toInt()
}
