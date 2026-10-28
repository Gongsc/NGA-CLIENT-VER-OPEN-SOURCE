package sp.phone.http.post

import okhttp3.HttpUrl
import sp.phone.http.post.NgaWriteClient.PostException
import sp.phone.param.PostParam
import java.io.IOException

/**
 * 发帖、回复、引用、编辑与贴条的提交协议，参照 SNGA 的实现：
 *
 * 1. 预检：`POST post.php?action=…&lite=xml&__inchst=UTF8`，从 XML 中取得提交校验字段 `auth`；
 * 2. 提交：向同一地址以 UTF-8 表单提交 `step=2`、`auth`、正文等字段；
 * 3. 结果：按 `<__MESSAGE>` 中的提示、`pid` 判断成败，无法判断时不当作成功，避免用户重复提交。
 *
 * 请求细节（凭据、请求头、编码、错误处理）见 [NgaWriteClient]。
 */
class TopicPostClient(private val writer: NgaWriteClient) {

    /** 预检，返回提交与附件上传所需的 `auth`；论坛未提供时返回 null。 */
    @Throws(IOException::class)
    fun prepare(param: PostParam): String? = prepare(param, comment = false)

    /** 提交并返回论坛的成功提示；没有 `auth` 时先预检。失败抛出 [PostException]。 */
    @Throws(IOException::class)
    fun submit(param: PostParam): String = submit(param, comment = false)

    /** 贴条：对指定楼层回复短评，协议与回复相同，另带 `comment=1`。 */
    @Throws(IOException::class)
    fun submitComment(tid: String, pid: String, content: String, anonymous: Boolean): String {
        val param = PostParam(tid, null, content).apply {
            postAction = "reply"
            postPid = pid
            isAnonymous = anonymous
        }
        return submit(param, comment = true).replace("发贴完毕", "贴条成功").replace("发帖完毕", "贴条成功").trim()
    }

    private fun prepare(param: PostParam, comment: Boolean): String? {
        val response = writer.execute(postUrl(param, comment), emptyList(), referer = writer.baseUrl())
        return parsePreflight(response.text)
    }

    private fun submit(param: PostParam, comment: Boolean): String {
        val url = postUrl(param, comment)
        val auth = param.authCode ?: prepare(param, comment)?.also { param.authCode = it }
        val fields = ArrayList<Pair<String, String>>()
        auth?.let { fields += "auth" to it }
        fields += "step" to "2"
        fields += "post_content" to param.postContent.orEmpty()
        param.postSubject?.takeIf { it.isNotBlank() }?.let { fields += "post_subject" to it }
        param.postPid?.takeIf { it.isNotEmpty() }?.let { fields += "pid" to it }
        if (param.isAnonymous) fields += "anony" to "1"
        // 附件字段由上传接口返回，已是表单编码后的值，原样提交
        val encoded = ArrayList<Pair<String, String>>()
        param.attachments?.let { encoded += "attachments" to it }
        param.attachmentsCheck?.let { encoded += "attachments_check" to it }

        val response = try {
            writer.execute(url, fields, referer = url.toString(), encodedFields = encoded)
        } catch (e: PostException) {
            throw e
        } catch (e: IOException) {
            throw PostException(NgaWriteClient.AMBIGUOUS_NETWORK)
        }
        if (response.code in 300..399) {
            if (response.location?.contains("login") == true) {
                throw PostException(NgaWriteClient.LOGIN_REQUIRED, requiresLogin = true)
            }
            return "发帖完毕"
        }
        return parseSubmission(response.text)
    }

    internal fun postUrl(param: PostParam, comment: Boolean = false): HttpUrl {
        val action = param.postAction ?: "reply"
        val builder = writer.url("post.php").addQueryParameter("action", action)
        val tid = param.postTid?.takeIf { it.isNotEmpty() }
        // 回复与编辑由 tid 确定版面；版面编号缺省为 -7，提交它会被当成其他版面
        if (tid == null && param.postFid != 0) {
            builder.addQueryParameter("fid", param.postFid.toString())
        }
        tid?.let { builder.addQueryParameter("tid", it) }
        param.postPid?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("pid", it) }
        param.stid?.takeIf { it.isNotEmpty() }?.let { builder.addQueryParameter("stid", it) }
        if (comment) builder.addQueryParameter("comment", "1")
        return builder
            .addQueryParameter("lite", "xml")
            .addQueryParameter("__inchst", "UTF8")
            .build()
    }

    companion object {

        /** 当前账号与论坛地址的生产实例。 */
        @JvmStatic
        fun create(): TopicPostClient = TopicPostClient(NgaWriteClient.create())

        /** 解析预检响应：论坛给出提示即为拒绝（如无权限、未登录）；否则返回 `auth`。 */
        internal fun parsePreflight(text: String): String? {
            NgaWriteClient.messageText(text)?.let { message ->
                throw PostException(message, requiresLogin = message.contains("登录"))
            }
            val auth = Regex("<auth>([^<]*)</auth>", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.trim()
            val hasForm = listOf("auth", "content", "subject", "attach_url").any {
                Regex("<$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
            }
            if (!hasForm) throw PostException("无法读取发帖页面，请稍后重试")
            return auth?.takeIf { it.isNotEmpty() }
        }

        private val JSON_PID = Regex("\"pid\"\\s*:\\s*\"?(\\d+)")
        private val XML_PID = Regex("<pid>\\s*(\\d+)\\s*</pid>", RegexOption.IGNORE_CASE)

        /** 解析提交结果，成功时返回提示文字。 */
        internal fun parseSubmission(text: String): String {
            val message = NgaWriteClient.messageText(text)
            if (message != null) {
                if (message.contains("完毕") || message.contains("成功") || message.contains(NgaWriteClient.MENTION_LIMIT)) {
                    return message
                }
                throw PostException(message, requiresLogin = message.contains("登录"))
            }
            if (text.contains("ERROR:") || text.contains("操作失败") || text.contains("发送失败")) {
                throw PostException(NgaWriteClient.concise(text).ifEmpty { "提交失败，请稍后重试" })
            }
            if (JSON_PID.containsMatchIn(text) || XML_PID.containsMatchIn(text)) return "发帖完毕"
            if (text.contains("成功") || text.contains("正在跳转")) return "发帖完毕"
            throw PostException(NgaWriteClient.AMBIGUOUS)
        }
    }
}
