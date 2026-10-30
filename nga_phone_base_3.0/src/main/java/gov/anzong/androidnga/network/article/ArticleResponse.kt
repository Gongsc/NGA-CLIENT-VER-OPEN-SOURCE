package gov.anzong.androidnga.network.article

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonArray
import gov.anzong.androidnga.base.json.NgaJsonObject
import gov.anzong.androidnga.base.util.javaTrim
import java.io.StringReader
import java.util.ArrayDeque
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

/** 将主题接口的 JSON 与 XML 响应转换为统一的数据结构。 */
object ArticleResponse {

    private val XML_ROOT = Regex("(?s)(?:<\\?xml[^>]*>\\s*)?<root(?:\\s[^>]*)?>.*")
    private val XML_END = Regex("(?s).*</root>\\s*(?:<!--.*?-->\\s*)?")
    private val NUMERIC_TEXT = Regex("\"(content|subject)\":([+]\\d+|0\\d+),")
    private val NUMERIC_AUTHOR = Regex("\"author\":(0\\d+),")
    private val NUMBERED_TAG = Regex("_-?\\d+")

    /** 去掉 BOM、首尾空白（与 Java 的 String.trim 一致）与脚本标记。 */
    private fun normalize(text: String): String =
        text.replace("\uFEFF", "").javaTrim().replace("/*\$js\$*/", "")

    /** 仅检测明确的未结束文档，不把登录 HTML 或完整但格式不兼容的响应当作截断。 */
    @JvmStatic
    fun isIncomplete(text: String): Boolean {
        val source = normalize(text)
        if (source.isEmpty()) return true
        if (source.startsWith("<")) {
            return source.matches(XML_ROOT) && !source.matches(XML_END)
        }
        if (!source.startsWith("{") && !source.startsWith("[")) return false
        val stack = ArrayDeque<Char>()
        var quoted = false
        var escaped = false
        for (value in source) {
            if (quoted) {
                if (escaped) {
                    escaped = false
                } else if (value == '\\') {
                    escaped = true
                } else if (value == '"') {
                    quoted = false
                }
            } else if (value == '"') {
                quoted = true
            } else if (value == '{' || value == '[') {
                stack.push(value)
            } else if (value == '}' || value == ']') {
                if (stack.isEmpty() || stack.pop() != (if (value == '}') '{' else '[')) return false
                // NGA 有时在完整 JSON 后追加非数据内容，不将其参与结构检查。
                if (stack.isEmpty()) return false
            }
        }
        return quoted || !stack.isEmpty()
    }

    @JvmStatic
    @Throws(Exception::class)
    fun decode(text: String): NgaJsonObject {
        var source = normalize(text)
        if (!source.startsWith("<")) {
            val errorFill = source.indexOf("/*error fill content")
            if (errorFill >= 0) source = source.substring(0, errorFill)
            source = source.replace(NUMERIC_TEXT, "\"$1\":\"$2\",").replace(NUMERIC_AUTHOR, "\"author\":\"$1\",")
            val root = NgaJson.parseObject(source) ?: throw IllegalArgumentException("空主题响应")
            return root.getJSONObject("data") ?: root
        }
        val lower = source.lowercase(Locale.ROOT)
        if (lower.startsWith("<!doctype html") || lower.startsWith("<html")) {
            return ArticleHtmlResponse.decode(source)
        }
        val upper = source.uppercase(Locale.ROOT)
        if (upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY")) {
            throw IllegalArgumentException("不支持的 XML 声明")
        }
        val factory = DocumentBuilderFactory.newInstance()
        factory.isExpandEntityReferences = false
        val builder = factory.newDocumentBuilder()
        builder.setEntityResolver { _, _ -> throw IllegalArgumentException("禁止访问外部 XML 资源") }
        val root = builder.parse(InputSource(StringReader(source))).documentElement
        if ("root" != root.tagName) throw IllegalArgumentException("非主题响应")
        val data = elementValue(root) as NgaJsonObject
        for (key in arrayOf("__R", "__U")) {
            if ("" == data[key]) data[key] = NgaJsonObject()
        }
        if (!data.containsKey("__R__ROWS") && data["__R"] is NgaJsonObject) {
            data["__R__ROWS"] = data.getJSONObject("__R")!!.size
        }
        val users = data.getJSONObject("__U")
        if (users != null) {
            val byId = NgaJsonObject()
            for ((key, user) in users) {
                val uid = (user as? NgaJsonObject)?.getString("uid")
                byId[uid ?: key] = user
            }
            data["__U"] = byId
        }
        return data
    }

    private fun elementValue(element: Element): Any {
        val values = NgaJsonObject()
        var index = 0
        var hasElements = false
        var node = element.firstChild
        while (node != null) {
            if (node is Element) {
                hasElements = true
                var key = node.tagName
                if ("item" == key) {
                    key = (index++).toString()
                } else if (key.matches(NUMBERED_TAG)) {
                    key = key.substring(1)
                }
                var value = elementValue(node)
                if ("" == value && key in EMPTY_OBJECT_KEYS) {
                    value = NgaJsonObject()
                }
                values[key] = value
            }
            node = node.nextSibling
        }
        return if (hasElements) values else element.textContent
    }

    /** XML 中为空时表示空对象的字段。 */
    private val EMPTY_OBJECT_KEYS = setOf("comment", "attachs", "buffs", "__GROUPS")

    @JvmStatic
    fun businessError(data: NgaJsonObject): String? {
        val error = data["error"] ?: data["__MESSAGE"]
        return when (error) {
            is NgaJsonObject -> error.getString("1") ?: error.getString("0") ?: error.getString("message")
            is NgaJsonArray -> if (error.isEmpty()) null else error.getString(error.size - 1)
            is String -> error
            else -> null
        }
    }
}
