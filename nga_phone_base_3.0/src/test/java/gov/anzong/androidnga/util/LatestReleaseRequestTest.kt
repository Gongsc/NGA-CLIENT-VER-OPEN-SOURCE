package gov.anzong.androidnga.util

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** 检查更新经 OkHttp 请求 GitHub；响应均为合成数据，不访问网络。 */
class LatestReleaseRequestTest {

    private val requests = mutableListOf<Request>()

    private fun client(code: Int = 200, body: String = "", fail: Boolean = false) = OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        requests += chain.request()
        if (fail) throw IOException("connection reset")
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("")
            .body(body.toResponseBody("application/json; charset=utf-8".toMediaType())).build()
    }).build()

    @Test fun parsesLatestReleaseAndSendsGithubHeaders() = runBlocking {
        val body = """{"tag_name":"v4.3.1","html_url":"$RELEASES_URL/tag/v4.3.1","body":"修复问题","assets":[]}"""
        val result = checkLatestRelease("4.3.0", client(body = body)) as UpdateResult.Available
        assertEquals(ReleaseInfo("4.3.1", "$RELEASES_URL/tag/v4.3.1", "修复问题"), result.release)
        val request = requests.single()
        assertEquals("/repos/Gongsc/NGA-CLIENT-VER-OPEN-SOURCE/releases/latest", request.url.encodedPath)
        assertEquals("application/vnd.github+json", request.header("Accept"))
        assertEquals("NGA-Client/4.3.0", request.header("User-Agent"))
    }

    @Test fun reportsHttpNetworkAndParseFailures() = runBlocking {
        assertEquals(UpdateResult.UpToDate(null), checkLatestRelease("4.3.0", client(code = 404)))
        assertEquals(UpdateResult.Failed("请求过于频繁，请稍后再试"), checkLatestRelease("4.3.0", client(code = 403)))
        assertEquals(UpdateResult.Failed("网络连接失败"), checkLatestRelease("4.3.0", client(fail = true)))
        assertTrue(checkLatestRelease("4.3.0", client(body = "<html>")) is UpdateResult.Failed)
    }
}
