package sp.phone.http.retrofit

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import sp.phone.http.retrofit.converter.JsonStringConvertFactory
import java.nio.charset.Charset

/**
 * 论坛接口的转换与错误行为。期望值在移除 RetrofitService 前已与其逐项比对一致。
 */
class NgaApiTest {

    private val requests = mutableListOf<String>()

    private fun api(code: Int, body: String) = Retrofit.Builder()
        .baseUrl("https://bbs.nga.cn/")
        .addConverterFactory(JsonStringConvertFactory.create())
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += "${request.method} ${request.url}"
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                .body(body.toByteArray(Charset.forName("GBK")).toResponseBody("text/html".toMediaType()))
                .build()
        }.build())
        .build()
        .create(NgaApi::class.java)

    @Test fun stringResponsesAreDecodedAsGbk() {
        val api = api(200, "{\"data\":{\"0\":\"操作成功\"}}")
        val query = mapOf("__lib" to "topic_key", "__act" to "get")
        assertEquals("{\"data\":{\"0\":\"操作成功\"}}", runBlocking { api.get(query) })
        assertEquals("{\"data\":{\"0\":\"操作成功\"}}", runBlocking { api.post("read.php?tid=1") })
        assertEquals("{\"data\":{\"0\":\"操作成功\"}}", runBlocking { api.get("nuke.php?x=1", mapOf("Cookie" to "c")) })
        assertEquals(
            listOf(
                "GET https://bbs.nga.cn/nuke.php?__lib=topic_key&__act=get",
                "POST https://bbs.nga.cn/read.php?tid=1",
                "GET https://bbs.nga.cn/nuke.php?x=1",
            ),
            requests,
        )
    }

    @Test fun httpErrorsThrowHttpException() {
        try {
            runBlocking { api(403, "禁止访问").get("read.php") }
            fail("应抛出 HttpException")
        } catch (e: HttpException) {
            assertEquals(403, e.code())
            assertEquals("HTTP 403 fixture", e.message)
        }
    }

    @Test fun rawResponsesKeepBodyBytes() {
        val body = runBlocking { api(200, "<root>中文</root>").getArticleResponse("read.php", emptyMap()) }
        assertEquals("<root>中文</root>", String(body.bytes(), Charset.forName("GBK")))
    }
}
