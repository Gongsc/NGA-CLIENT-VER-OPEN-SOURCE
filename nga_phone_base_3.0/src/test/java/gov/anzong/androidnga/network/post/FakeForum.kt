package gov.anzong.androidnga.network.post

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import java.net.URLDecoder

/** 合成的论坛响应，按顺序返回，并记录请求与表单；不访问真实论坛。 */
internal class Reply(val body: String, val code: Int = 200, val contentType: String = "application/xml; charset=utf-8")

internal class FakeForum(vararg replies: Reply) : Interceptor {
    private val pending = ArrayDeque(replies.toList())
    val requests = mutableListOf<Request>()
    val bodies = mutableListOf<String>()
    var failNext = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        requests += request
        bodies += Buffer().also { request.body?.writeTo(it) }.readUtf8()
        if (failNext) throw IOException("connection reset")
        val reply = pending.removeFirst()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(reply.code)
            .message("OK")
            .body(reply.body.toByteArray(charset(if (reply.contentType.contains("gbk")) "GBK" else "UTF-8"))
                .toResponseBody(reply.contentType.toMediaType()))
            .build()
    }

    fun writer(loggedIn: Boolean = true) = NgaWriteClient(
        OkHttpClient.Builder().addInterceptor(this).build(),
        "https://bbs.nga.cn/",
    ) { if (loggedIn) NgaWriteClient.Credentials("123", "secret-token") else null }
}

/** 解析 UTF-8 表单为字段表。 */
internal fun form(body: String): Map<String, String> = body.split("&").filter { it.isNotEmpty() }.associate {
    val (name, value) = (it.split("=", limit = 2) + "").take(2)
    name to URLDecoder.decode(value, "UTF-8")
}
