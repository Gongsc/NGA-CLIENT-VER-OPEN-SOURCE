package sp.phone.http.post

import sp.phone.param.MessagePostParam
import java.io.IOException

/**
 * 发送与回复短消息，参照 SNGA：`POST nuke.php?__lib=message&__act=message&act=…&__output=11`，
 * 回复带 `mid`，新消息带收件人 `to` 与标题；结果按 [NgaWriteClient.postAction] 判断。
 */
class MessagePostClient(private val writer: NgaWriteClient) {

    /** 成功时返回论坛的提示；失败抛出 [NgaWriteClient.PostException]。 */
    @Throws(IOException::class)
    fun send(param: MessagePostParam): String {
        val action = param.action ?: "reply"
        val url = writer.url("nuke.php")
            .addQueryParameter("__lib", "message")
            .addQueryParameter("__act", "message")
            .addQueryParameter("act", action)
            .addQueryParameter("__output", "11")
            .addQueryParameter("__inchst", "UTF8")
            .build()
        val fields = buildList {
            if (param.mid != 0) add("mid" to param.mid.toString())
            // 多个收件人以英文逗号分隔
            param.recipient?.replace('，', ',')?.trim()?.takeIf { it.isNotEmpty() }?.let { add("to" to it) }
            param.postSubject?.takeIf { it.isNotBlank() }?.let { add("subject" to it) }
            add("content" to param.postContent.orEmpty())
        }
        return writer.postAction(url, fields, referer = null)
    }

    companion object {
        @JvmStatic
        fun create(): MessagePostClient = MessagePostClient(NgaWriteClient.create())
    }
}
