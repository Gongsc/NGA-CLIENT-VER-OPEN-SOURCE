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
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import sp.phone.http.retrofit.converter.JsonStringConvertFactory
import java.nio.charset.Charset

/** 挂起式接口与 RetrofitService 共用转换器与适配方式，对同一响应得到相同结果。 */
class NgaApiTest {

    private val requests = mutableListOf<String>()

    private fun retrofit(code: Int, body: String) = Retrofit.Builder()
        .baseUrl("https://bbs.nga.cn/")
        .addConverterFactory(JsonStringConvertFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += "${request.method} ${request.url}"
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                .body(body.toByteArray(Charset.forName("GBK")).toResponseBody("text/html".toMediaType()))
                .build()
        }.build())
        .build()

    @Test fun stringResponsesAreDecodedLikeRetrofitService() {
        val retrofit = retrofit(200, "{\"data\":{\"0\":\"操作成功\"}}")
        val legacy = retrofit.create(RetrofitService::class.java)
        val api = retrofit.create(NgaApi::class.java)
        val query = mapOf("__lib" to "topic_key", "__act" to "get")
        assertEquals(legacy.get(query).blockingFirst(), runBlocking { api.get(query) })
        assertEquals(legacy.post("read.php?tid=1").blockingFirst(), runBlocking { api.post("read.php?tid=1") })
        assertEquals("{\"data\":{\"0\":\"操作成功\"}}", runBlocking { api.get("nuke.php?x=1", mapOf("Cookie" to "c")) })
        assertEquals(
            listOf(
                "GET https://bbs.nga.cn/nuke.php?__lib=topic_key&__act=get",
                "GET https://bbs.nga.cn/nuke.php?__lib=topic_key&__act=get",
                "POST https://bbs.nga.cn/read.php?tid=1",
                "POST https://bbs.nga.cn/read.php?tid=1",
                "GET https://bbs.nga.cn/nuke.php?x=1",
            ),
            requests,
        )
    }

    @Test fun httpErrorsThrowLikeRetrofitService() {
        val retrofit = retrofit(403, "禁止访问")
        val legacy = try {
            retrofit.create(RetrofitService::class.java).get("read.php").blockingFirst()
            null
        } catch (e: HttpException) {
            e
        }
        val current = try {
            runBlocking { retrofit.create(NgaApi::class.java).get("read.php") }
            fail("应抛出 HttpException")
            null
        } catch (e: HttpException) {
            e
        }
        assertEquals(legacy!!.code(), current!!.code())
        assertEquals(legacy.message, current.message)
    }

    @Test fun rawResponsesKeepBodyBytes() {
        val retrofit = retrofit(200, "<root>中文</root>")
        val body = runBlocking { retrofit.create(NgaApi::class.java).getArticleResponse("read.php", emptyMap()) }
        assertEquals("<root>中文</root>", String(body.bytes(), Charset.forName("GBK")))
    }
}
