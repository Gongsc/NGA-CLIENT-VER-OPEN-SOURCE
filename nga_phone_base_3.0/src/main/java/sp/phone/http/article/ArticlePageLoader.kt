package sp.phone.http.article

import gov.anzong.androidnga.base.json.NgaJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.ResponseBody
import java.nio.charset.Charset

/**
 * 同一账号下按有限顺序尝试服务端响应格式，不触发页面导航。
 *
 * 依次请求 JSON、普通网页、XML 与备用 XML，每个来源至多一次。只有格式不兼容或数据不完整时才切换来源；
 * 网络错误与论坛业务错误直接抛出。详见 docs/topic-opening-fix.md。
 */
class ArticlePageLoader<T>(
    private val fetcher: suspend (url: String, headers: Map<String, String>) -> ResponseBody,
    private val parser: Parser<T>,
) {
    fun interface Parser<T> {
        fun parse(json: String): T?
    }

    /** 在调用方的调度器上执行，调用方负责切换到后台线程。 */
    suspend fun load(baseUrl: String, headers: Map<String, String>?): T {
        val snapshot = HashMap<String, String>()
        if (headers != null) snapshot.putAll(headers)
        snapshot["User-Agent"] = READ_USER_AGENT
        snapshot["X-User-Agent"] = READ_USER_AGENT
        snapshot["Referer"] = baseUrl
        var format = 0
        while (true) {
            try {
                return attempt(baseUrl, snapshot, format)
            } catch (error: ResponseFormatException) {
                if (format + 1 >= FORMATS.size) throw error
                format++
            }
        }
    }

    private suspend fun attempt(baseUrl: String, headers: Map<String, String>, format: Int): T {
        var url = baseUrl + if (FORMATS[format].isEmpty()) "" else "&" + FORMATS[format]
        val requestHeaders = HashMap(headers)
        if (format > 0) {
            url += "&nga_client_retry=" + System.nanoTime()
            requestHeaders["Cache-Control"] = "no-cache, no-store"
            requestHeaders["Pragma"] = "no-cache"
        }
        val source = fetcher(url, requestHeaders).use { response ->
            var charset = Charset.forName("GB18030")
            response.contentType()?.let { charset = it.charset(charset) }
            // NGA 常以 GBK 标头发送 GB18030 内容，使用其超集避免丢失扩展字符。
            if (charset.name().equals("GBK", ignoreCase = true) || charset.name().equals("GB2312", ignoreCase = true)) {
                charset = Charset.forName("GB18030")
            }
            String(response.bytes(), charset)
        }
        if (ArticleResponse.isIncomplete(source)) throw ResponseFormatException(true)
        val data = try {
            ArticleResponse.decode(source)
        } catch (error: Exception) {
            throw ResponseFormatException()
        }
        val message = ArticleResponse.businessError(data)
        if (!message.isNullOrEmpty()) throw BusinessException(message)
        try {
            validate(data, baseUrl)
            val root = NgaJsonObject()
            root["data"] = data
            return parser.parse(root.toJSONString()) ?: throw ResponseFormatException()
        } catch (error: RuntimeException) {
            throw ResponseFormatException()
        }
    }

    class ResponseFormatException internal constructor(val isIncomplete: Boolean) :
        Exception(if (isIncomplete) "主题数据接收不完整，请稍后重试。" else "主题数据暂时无法解析，请稍后重试。") {
        constructor() : this(false)
    }

    class BusinessException(message: String) : Exception(message)

    companion object {
        private val FORMATS = arrayOf("__output=8&noprefix&v2", "", "lite=xml", "__output=10")
        private const val READ_USER_AGENT = "NGA_WP_JW/(;WINDOWS)"

        /** 所有来源都先校验，再进入正文转换和缓存，禁止把半页内容当成完整页。 */
        private fun validate(data: NgaJsonObject, baseUrl: String) {
            val topic = data.getJSONObject("__T")
            val rows = data.getJSONObject("__R")
            if (topic == null || rows == null || !data.containsKey("__ROWS") || !data.containsKey("__R__ROWS") ||
                data.getIntValue("__ROWS") < 0 || rows.size != data.getIntValue("__R__ROWS")
            ) throw ResponseFormatException()
            val tid = baseUrl.toHttpUrlOrNull()?.queryParameter("tid")
            if (tid != null && tid != topic.getString("tid")) throw ResponseFormatException()
            for (i in 0 until rows.size) {
                val row = rows.getJSONObject(i.toString())
                if (row == null || (!row.containsKey("content") && !row.containsKey("subject"))) {
                    throw ResponseFormatException()
                }
            }
        }
    }
}
