package sp.phone.http.favorites

import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import sp.phone.http.retrofit.RetrofitHelper
import sp.phone.http.retrofit.NgaApi

class ForumFavoriteTransportTest {
    @Test fun addAndRemoveUseForumEndpointAccountCookieAndCollectionId() {
        val actions = mutableListOf<String>()
        val helper = RetrofitHelper.getInstance()
        val builder = helper.createOkHttpClientBuilder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("POST", request.method)
            assertEquals("forum_favor2", request.url.queryParameter("__lib"))
            assertEquals("forum_favor", request.url.queryParameter("__act"))
            assertEquals("fixture-account", request.header("Cookie"))
            val body = request.body as FormBody
            val fields = (0 until body.size).associate { body.name(it) to body.value(it) }
            actions += fields.getValue("action")
            assertEquals("42", fields["fid"])
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("""{"data":{"0":"操作成功"}}""".toResponseBody("application/json; charset=UTF-8".toMediaType())).build()
        }
        val service = helper.createRetrofit("https://bbs.nga.cn/", builder).create(NgaApi::class.java)
        val board = ForumFavoriteBoard(-7, 42, "测试合集")
        for (add in listOf(true, false)) {
            runBlocking {
                service.forumFavorites("https://bbs.nga.cn/nuke.php?__lib=forum_favor2&__act=forum_favor&__output=8",
                    mapOf("Cookie" to "fixture-account"), ForumFavoritesParser.mutationFields(board, add))
            }.use { ForumFavoritesParser.parseMutationResponse(it.string()) }
        }
        assertEquals(listOf("add", "del"), actions)
    }
}
