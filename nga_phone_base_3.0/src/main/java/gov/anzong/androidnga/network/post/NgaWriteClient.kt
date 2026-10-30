package gov.anzong.androidnga.network.post

import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonArray
import gov.anzong.androidnga.base.json.NgaJsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import gov.anzong.androidnga.account.UserManagerImpl
import java.io.IOException
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.concurrent.TimeUnit

/**
 * 论坛写操作（发帖、贴条、短消息、头像、投票）的公共请求层，参照 SNGA：
 *
 * - 表单以 UTF-8 编码提交，附带同一账号的 `access_uid`/`access_token`；
 * - 使用 SNGA 的 User-Agent，不经过公共拦截器，凭据不会写入调试日志；
 * - 不跟随重定向、不自动重试，结果无法判断时不当作成功，避免重复提交。
 *
 * 只做阻塞请求，调用方负责切到后台线程。
 */
class NgaWriteClient(
    private val client: OkHttpClient,
    /** 论坛地址，如 `https://bbs.nga.cn/`。 */
    baseUrl: String,
    private val credentials: () -> Credentials?,
) {

    class Credentials(val uid: String, val token: String)

    /** 论坛明确拒绝或结果无法确认；[message] 可直接展示给用户。 */
    class PostException(message: String, val requiresLogin: Boolean = false) : IOException(message)

    /** 供 Java 调用方使用的结果，不抛异常。 */
    class ActionResult(@JvmField val success: Boolean, @JvmField val message: String)

    class RawResponse(val code: Int, val location: String?, val text: String)

    private val base = baseUrl.trimEnd('/') + "/"

    fun url(path: String): HttpUrl.Builder = (base + path.trimStart('/')).toHttpUrl().newBuilder()

    /** 论坛首页地址，用作 Referer。 */
    fun baseUrl(): String = base

    /**
     * 提交表单。[fields] 的值未编码时由本方法按 UTF-8 编码；[encodedFields] 为已编码的值，原样提交。
     * HTTP 错误、要求登录时抛出 [PostException]。
     */
    @Throws(IOException::class)
    fun execute(
        url: HttpUrl,
        fields: List<Pair<String, String>>,
        referer: String?,
        encodedFields: List<Pair<String, String>> = emptyList(),
    ): RawResponse {
        val account = credentials()
        val form = buildList {
            fields.forEach { (name, value) -> add(name to encode(value)) }
            addAll(encodedFields)
            if (account != null) {
                add("access_uid" to encode(account.uid))
                add("access_token" to encode(account.token))
            }
        }.joinToString("&") { (name, value) -> "$name=$value" }
        val request = Request.Builder()
            .url(url)
            .post(form.toRequestBody(FORM_TYPE))
            .header("User-Agent", USER_AGENT)
            .header("X-User-Agent", USER_AGENT)
            .header("Accept", "text/html,application/json;q=0.9,*/*;q=0.7")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
            .apply {
                if (referer != null) header("Referer", referer)
                if (account != null) header("Cookie", "ngaPassportUid=${account.uid}; ngaPassportCid=${account.token}")
            }
            .build()
        client.newBuilder().followRedirects(false).build().newCall(request).execute().use { response ->
            val bytes = response.body?.bytes() ?: ByteArray(0)
            val raw = RawResponse(response.code, response.header("Location"), decode(bytes, response.header("Content-Type")))
            validate(raw)
            return raw
        }
    }

    /**
     * `nuke.php` 等接口的写操作，按 SNGA 的规则判断结果，成功时返回论坛的提示文字。
     * 请求已发出但结果不明时抛出 [PostException]（[AMBIGUOUS]/[AMBIGUOUS_NETWORK]）。
     */
    @Throws(IOException::class)
    fun postAction(url: HttpUrl, fields: List<Pair<String, String>>, referer: String?): String {
        val response = try {
            execute(url, fields, referer)
        } catch (e: PostException) {
            throw e
        } catch (e: IOException) {
            throw PostException(AMBIGUOUS_NETWORK)
        }
        if (response.code in 300..399) {
            if (response.location?.contains("login") == true) throw PostException(LOGIN_REQUIRED, requiresLogin = true)
            return "操作成功"
        }
        return parseAction(response.text)
    }

    /** 同 [postAction]，失败时返回提示而不抛异常，供 Java 调用方在后台线程使用。 */
    fun tryPostAction(url: HttpUrl, fields: List<Pair<String, String>>, referer: String?): ActionResult = try {
        ActionResult(true, postAction(url, fields, referer))
    } catch (e: IOException) {
        ActionResult(false, e.message ?: "提交失败，请稍后重试")
    }

    private fun validate(response: RawResponse) {
        val code = response.code
        if (code in 200..399) {
            if (requiresLogin(response.text)) throw PostException(LOGIN_REQUIRED, requiresLogin = true)
            return
        }
        val message = messageText(response.text)
        throw when {
            code == 401 || requiresLogin(response.text) -> PostException(LOGIN_REQUIRED, requiresLogin = true)
            message != null -> PostException(message, requiresLogin = message.contains("登录"))
            code == 403 -> PostException("论坛暂时拒绝了本次访问（HTTP 403），请稍后重试")
            code == 429 -> PostException("请求过于频繁，请稍后再试")
            else -> PostException("论坛服务器错误（HTTP $code），请稍后重试")
        }
    }

    companion object {

        const val USER_AGENT = "NGA_WP_JW/(;WINDOWS)"

        const val LOGIN_REQUIRED = "登录已失效，请重新登录后再试"

        const val AMBIGUOUS = "无法确认是否已提交成功，请刷新后查看，避免重复提交"

        const val AMBIGUOUS_NETWORK = "网络错误，无法确认是否已提交成功，请刷新查看后再重试"

        /** 正文已发出，只是 @ 提醒超出每日上限。 */
        const val MENTION_LIMIT = "@提醒每24小时不能超过50个"

        private val FORM_TYPE = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()

        private val GB18030: Charset = Charset.forName("GB18030")

        private const val JS_STORE = "window.script_muti_get_var_store="

        private val MESSAGE_ENVELOPE = Regex("<__MESSAGE\\b[^>]*>([\\s\\S]*?)</__MESSAGE>", RegexOption.IGNORE_CASE)
        private val MESSAGE_ITEM = Regex("<item\\b[^>]*>([\\s\\S]*?)</item>", RegexOption.IGNORE_CASE)
        private val JSON_ERROR = Regex("\"error\"\\s*:\\s*\\[\\s*\"(?:\\d+:)?((?:[^\"\\\\]|\\\\.)*)\"")
        private val TAG = Regex("<[^>]+>")

        /** 当前账号与论坛地址的生产实例。 */
        @JvmStatic
        fun create(): NgaWriteClient = NgaWriteClient(
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .build(),
            Utils.getNGAHost(),
        ) {
            val user = UserManagerImpl.getInstance()
            val uid = user.userId
            val cid = user.cid
            if (uid.isNullOrEmpty() || cid.isNullOrEmpty()) null else Credentials(uid, cid)
        }

        fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

        /** 按响应声明的字符集解码；未声明时优先按 UTF-8，失败再按 GB18030。 */
        internal fun decode(bytes: ByteArray, contentType: String?): String {
            val type = contentType?.lowercase().orEmpty()
            if (type.contains("gbk") || type.contains("gb2312") || type.contains("gb18030")) {
                return String(bytes, GB18030)
            }
            return try {
                Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString()
            } catch (e: CharacterCodingException) {
                String(bytes, GB18030)
            }
        }

        /** XML `<__MESSAGE>` 中的提示，格式如 `<item>0</item><item>发帖完毕</item><item>200</item>`。 */
        internal fun messageItems(text: String): List<String> {
            val envelope = MESSAGE_ENVELOPE.find(text)?.groupValues?.get(1) ?: return emptyList()
            return MESSAGE_ITEM.findAll(envelope).map { plainText(it.groupValues[1]) }.toList()
        }

        /** XML `<__MESSAGE>` 或 JSON `error` 中的提示；没有时返回 null。 */
        internal fun messageText(text: String): String? {
            val items = messageItems(text)
            if (items.isNotEmpty()) {
                return items.drop(1).firstOrNull { it.isNotEmpty() } ?: items.firstOrNull { it.isNotEmpty() }
            }
            return JSON_ERROR.find(text)?.groupValues?.get(1)?.let { unescapeJson(it) }?.takeIf { it.isNotBlank() }
        }

        internal fun requiresLogin(text: String): Boolean {
            val hasLoginRoute = text.contains("__lib=login") || text.contains("__act=account&login")
            return hasLoginRoute && listOf("未登录", "你必须先登录论坛", "你必须登录", "必须登录后", "请先登录").any { text.contains(it) }
        }

        /**
         * 解析 `nuke.php` 写操作的结果（SNGA `actionSucceeded`）：
         * JSON 中的 `error` 即失败；`code` 非 0 即失败；`data` 中含“失败”“错误”即失败；其余视为成功。
         * 非 JSON 时，含“成功”“完成”为成功，含“失败”“ERROR”为失败，否则结果不明。
         */
        internal fun parseAction(text: String): String {
            val root = parseJson(text)
            if (root is NgaJsonObject) {
                (root["error"] ?: root["__MESSAGE"])?.let { error ->
                    val message = concise(flatten(error).replace(Regex("^\\d+:"), ""))
                    if (message.isNotEmpty()) {
                        if (message.contains(MENTION_LIMIT)) return message
                        throw PostException(message, requiresLogin = message.contains("登录"))
                    }
                }
                val code = (root["code"] as? Number)?.toInt()
                if (code != null) {
                    val message = (root["msg"] as? String)?.let { concise(it) }
                    if (code != 0) {
                        val text = message ?: concise(flatten(root))
                        throw PostException(text.ifEmpty { "提交失败，请稍后重试" }, requiresLogin = code == 5 || text.contains("登录"))
                    }
                    return message?.takeIf { it.isNotEmpty() } ?: "操作成功"
                }
                val data = root["data"] ?: root
                val flattened = concise(flatten(data))
                if (flattened.contains("失败") || flattened.contains("错误")) throw PostException(flattened)
                return firstString(data)?.let { concise(it) }?.takeIf { it.isNotEmpty() } ?: "操作成功"
            }
            if (root is NgaJsonArray) {
                val flattened = concise(flatten(root))
                if (flattened.contains("失败") || flattened.contains("错误")) throw PostException(flattened)
                return firstString(root)?.let { concise(it) }?.takeIf { it.isNotEmpty() } ?: "操作成功"
            }
            messageText(text)?.let { message ->
                if (message.contains("成功") || message.contains("完毕") || message.contains(MENTION_LIMIT)) return message
                throw PostException(message, requiresLogin = message.contains("登录"))
            }
            if (text.contains("成功") || text.contains("完成")) return concise(text)
            if (text.contains("失败") || text.contains("ERROR")) throw PostException(concise(text))
            throw PostException(AMBIGUOUS)
        }

        /**
         * 兼容 `lite=js` 的 `window.script_muti_get_var_store=` 前缀与尾部注释，
         * 以及把它包在 `<html>…<script>…</script>` 页面中的响应（如 `set_avatar` 的 `get`）；不是 JSON 时返回 null。
         */
        internal fun parseJson(text: String): Any? {
            var js = text.trim()
            val start = js.indexOf(JS_STORE)
            if (start >= 0) {
                js = js.substring(start + JS_STORE.length)
                val end = js.indexOf("</script>", ignoreCase = true)
                if (end >= 0) js = js.substring(0, end)
                js = js.trim().removeSuffix(";")
            }
            val comment = js.indexOf("/*error fill content")
            if (comment > 0) js = js.substring(0, comment)
            js = js.replace("/*\$js\$*/", "").trim()
            if (!js.startsWith("{") && !js.startsWith("[")) return null
            return try {
                NgaJson.parse(js)
            } catch (e: RuntimeException) {
                null
            }
        }

        private fun flatten(value: Any?): String = when (value) {
            null -> ""
            is String -> value
            is Map<*, *> -> value.values.joinToString(" ") { flatten(it) }
            is Iterable<*> -> value.joinToString(" ") { flatten(it) }
            else -> value.toString()
        }

        private fun firstString(value: Any?): String? = when (value) {
            is String -> value.takeIf { it.isNotBlank() }
            is Map<*, *> -> value.values.firstNotNullOfOrNull { firstString(it) }
            is Iterable<*> -> value.firstNotNullOfOrNull { firstString(it) }
            else -> null
        }

        internal fun plainText(html: String): String = html
            .replace(TAG, " ")
            .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()

        internal fun concise(text: String): String = plainText(text).take(160)

        private fun unescapeJson(value: String): String {
            val out = StringBuilder()
            var i = 0
            while (i < value.length) {
                val c = value[i]
                if (c == '\\' && i + 1 < value.length) {
                    val next = value[i + 1]
                    if (next == 'u' && i + 5 < value.length) {
                        value.substring(i + 2, i + 6).toIntOrNull(16)?.let { out.append(it.toChar()); i += 6; continue }
                    }
                    out.append(if (next == 'n') '\n' else next)
                    i += 2
                } else {
                    out.append(c)
                    i++
                }
            }
            return out.toString()
        }
    }
}
