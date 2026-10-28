package sp.phone.http.post

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import sp.phone.param.PostParam
import java.io.IOException
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * 发帖、回复、引用与编辑的提交协议，参照 SNGA 的实现：
 *
 * 1. 预检：`POST post.php?action=…&lite=xml&__inchst=UTF8`，从 XML 中取得提交校验字段 `auth`；
 * 2. 提交：向同一地址以 UTF-8 表单提交 `step=2`、`auth`、正文等字段；
 * 3. 结果：按 `<__MESSAGE>` 中的提示、`pid` 判断成败，无法判断时不当作成功，避免用户重复提交。
 *
 * 两次请求都在表单中附带 `access_uid`/`access_token`（同一账号的 Cookie 凭据）。
 * 本类只做阻塞请求，调用方负责切到后台线程；不经过公共拦截器，凭据不会写入调试日志。
 */
class TopicPostClient(
    private val client: OkHttpClient,
    /** 论坛地址，如 `https://bbs.nga.cn/`。 */
    private val baseUrl: String,
    private val credentials: () -> Credentials?,
) {

    class Credentials(val uid: String, val token: String)

    /** 论坛明确拒绝或结果无法确认；[message] 可直接展示给用户。 */
    class PostException(message: String, val requiresLogin: Boolean = false) : IOException(message)

    /** 预检，返回提交与附件上传所需的 `auth`；论坛未提供时返回 null。 */
    @Throws(IOException::class)
    fun prepare(param: PostParam): String? {
        val url = postUrl(param)
        val response = execute(url, emptyList(), referer = baseUrl)
        return parsePreflight(response.text)
    }

    /** 提交并返回论坛的成功提示；没有 `auth` 时先预检。失败抛出 [PostException]。 */
    @Throws(IOException::class)
    fun submit(param: PostParam): String {
        val url = postUrl(param)
        val auth = param.authCode ?: prepare(param)?.also { param.authCode = it }
        val fields = ArrayList<Pair<String, String>>()
        auth?.let { fields += "auth" to encode(it) }
        fields += "step" to "2"
        fields += "post_content" to encode(param.postContent.orEmpty())
        param.postSubject?.takeIf { it.isNotBlank() }?.let { fields += "post_subject" to encode(it) }
        param.postPid?.takeIf { it.isNotEmpty() }?.let { fields += "pid" to encode(it) }
        if (param.isAnonymous) fields += "anony" to "1"
        // 附件字段由上传接口返回，已是表单编码后的值，原样提交
        param.attachments?.let { fields += "attachments" to it }
        param.attachmentsCheck?.let { fields += "attachments_check" to it }

        val response = try {
            execute(url, fields, referer = url.toString())
        } catch (e: PostException) {
            throw e
        } catch (e: IOException) {
            throw PostException(AMBIGUOUS_NETWORK)
        }
        return parseSubmission(response)
    }

    internal fun postUrl(param: PostParam): HttpUrl {
        val action = param.postAction ?: "reply"
        val builder = (baseUrl.trimEnd('/') + "/post.php").toHttpUrl().newBuilder()
            .addQueryParameter("action", action)
        val tid = param.postTid?.takeIf { it.isNotEmpty() }
        // 回复与编辑由 tid 确定版面；版面编号缺省为 -7，提交它会被当成其他版面
        if (tid == null && param.postFid != 0) {
            builder.addQueryParameter("fid", param.postFid.toString())
        }
        tid?.let { builder.addQueryParameter("tid", it) }
        param.postPid?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("pid", it) }
        param.stid?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("stid", it) }
        return builder
            .addQueryParameter("lite", "xml")
            .addQueryParameter("__inchst", "UTF8")
            .build()
    }

    private class RawResponse(val code: Int, val location: String?, val text: String)

    private fun execute(url: HttpUrl, fields: List<Pair<String, String>>, referer: String): RawResponse {
        val account = credentials()
        val form = buildList {
            addAll(fields)
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
            .header("Referer", referer)
            .apply {
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

    private fun validate(response: RawResponse) {
        val code = response.code
        if (code in 200..399) {
            if (requiresLogin(response.text)) throw PostException(LOGIN_REQUIRED, requiresLogin = true)
            return
        }
        val message = messageText(response.text)
        throw when {
            code == 401 || requiresLogin(response.text) -> PostException(LOGIN_REQUIRED, requiresLogin = true)
            message != null -> PostException(message)
            code == 403 -> PostException("论坛暂时拒绝了本次访问（HTTP 403），请稍后重试")
            code == 429 -> PostException("请求过于频繁，请稍后再试")
            else -> PostException("论坛服务器错误（HTTP $code），请稍后重试")
        }
    }

    companion object {

        const val USER_AGENT = "NGA_WP_JW/(;WINDOWS)"

        const val LOGIN_REQUIRED = "登录已失效，请重新登录后再试"

        const val AMBIGUOUS = "无法确认是否已提交成功，请刷新主题查看，避免重复提交"

        const val AMBIGUOUS_NETWORK = "网络错误，无法确认是否已提交成功，请刷新主题查看后再重试"

        private val FORM_TYPE = "application/x-www-form-urlencoded; charset=UTF-8".toMediaType()

        private val GB18030: Charset = Charset.forName("GB18030")

        private val MESSAGE_ENVELOPE = Regex("<__MESSAGE\\b[^>]*>([\\s\\S]*?)</__MESSAGE>", RegexOption.IGNORE_CASE)
        private val MESSAGE_ITEM = Regex("<item\\b[^>]*>([\\s\\S]*?)</item>", RegexOption.IGNORE_CASE)
        private val JSON_ERROR = Regex("\"error\"\\s*:\\s*\\[\\s*\"(?:\\d+:)?((?:[^\"\\\\]|\\\\.)*)\"")
        private val JSON_PID = Regex("\"pid\"\\s*:\\s*\"?(\\d+)")
        private val XML_PID = Regex("<pid>\\s*(\\d+)\\s*</pid>", RegexOption.IGNORE_CASE)
        private val TAG = Regex("<[^>]+>")

        private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

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

        /** `<__MESSAGE>` 中的提示，格式如 `<item>0</item><item>发帖完毕</item><item>200</item>`。 */
        internal fun messageItems(text: String): List<String> {
            val envelope = MESSAGE_ENVELOPE.find(text)?.groupValues?.get(1) ?: return emptyList()
            return MESSAGE_ITEM.findAll(envelope).map { plainText(it.groupValues[1]) }.toList()
        }

        private fun messageText(text: String): String? {
            val items = messageItems(text)
            if (items.isNotEmpty()) {
                return items.drop(1).firstOrNull { it.isNotEmpty() } ?: items.firstOrNull { it.isNotEmpty() }
            }
            return JSON_ERROR.find(text)?.groupValues?.get(1)?.let { unescapeJson(it) }?.takeIf { it.isNotBlank() }
        }

        private fun requiresLogin(text: String): Boolean {
            val hasLoginRoute = text.contains("__lib=login") || text.contains("__act=account&login")
            return hasLoginRoute && listOf("未登录", "你必须先登录论坛", "你必须登录", "必须登录后", "请先登录").any { text.contains(it) }
        }

        /** 解析预检响应：论坛给出提示即为拒绝（如无权限、未登录）；否则返回 `auth`。 */
        internal fun parsePreflight(text: String): String? {
            messageText(text)?.let { message ->
                throw PostException(message, requiresLogin = message.contains("登录"))
            }
            val auth = Regex("<auth>([^<]*)</auth>", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim()
            val hasForm = listOf("auth", "content", "subject", "attach_url").any {
                Regex("<$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
            }
            if (!hasForm) throw PostException("无法读取发帖页面，请稍后重试")
            return auth?.takeIf { it.isNotEmpty() }
        }

        private fun parseSubmission(response: RawResponse): String {
            if (response.code in 300..399) {
                if (response.location?.contains("login") == true) {
                    throw PostException(LOGIN_REQUIRED, requiresLogin = true)
                }
                return "发帖完毕"
            }
            return parseSubmission(response.text)
        }

        /** 解析提交结果，成功时返回提示文字。 */
        internal fun parseSubmission(text: String): String {
            val message = messageText(text)
            if (message != null) {
                // 正文已发出，只是 @ 提醒超出每日上限
                if (message.contains("完毕") || message.contains("成功") || message.contains("@提醒每24小时不能超过50个")) {
                    return message
                }
                throw PostException(message, requiresLogin = message.contains("登录"))
            }
            if (text.contains("ERROR:") || text.contains("操作失败") || text.contains("发送失败")) {
                throw PostException(concise(text))
            }
            if (JSON_PID.containsMatchIn(text) || XML_PID.containsMatchIn(text)) return "发帖完毕"
            if (text.contains("成功") || text.contains("正在跳转")) return "发帖完毕"
            throw PostException(AMBIGUOUS)
        }

        private fun plainText(html: String): String = html
            .replace(TAG, " ")
            .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()

        private fun concise(text: String): String = plainText(text).take(120).ifEmpty { "提交失败，请稍后重试" }

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
