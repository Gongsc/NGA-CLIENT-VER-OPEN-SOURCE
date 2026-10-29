package gov.anzong.androidnga.ui.compose


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** 版块目录：请求地址、按响应字符集解码与加载状态；响应均为合成数据。 */
@OptIn(ExperimentalCoroutinesApi::class)
class ForumDirectoryTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    private class BodyApi(val body: () -> ResponseBody) : gov.anzong.androidnga.coroutines.FakeNgaApi() {
        val urls = mutableListOf<String>()
        override suspend fun getArticleResponse(url: String, headers: Map<String, String>): ResponseBody {
            urls += url
            return body()
        }
    }

    @Test fun decodesByDeclaredCharsetAndDefaultsToGbk() = runBlocking {
        val json = """{"result":[{"name":"分区"}]}"""
        val utf8 = BodyApi { json.toByteArray(Charsets.UTF_8).toResponseBody("application/json; charset=utf-8".toMediaType()) }
        assertEquals(json, requestForumDirectory(utf8, "https://bbs.nga.cn/"))
        assertEquals(listOf("https://bbs.nga.cn/app_api.php?__lib=home&__act=category"), utf8.urls)
        val gbk = BodyApi { json.toByteArray(charset("GBK")).toResponseBody("application/json".toMediaType()) }
        assertEquals(json, requestForumDirectory(gbk, "https://bbs.nga.cn/"))
    }

    @Test fun loadsBoardsOnceAndReportsFailure() {
        var calls = 0
        val model = ForumDirectoryViewModel({
            calls++
            """{"code":0,"result":[{"name":"分区","groups":[{"name":"组","forums":[{"id":7,"name":"版面","stid":0},{"id":8,"name":""}]}]}]}"""
        }, dispatcher)
        model.load()
        model.load()
        assertEquals(1, calls)
        assertEquals(listOf("版面"), model.state.boards.map { it.name })
        val failed = ForumDirectoryViewModel({ error("网络不可用") }, dispatcher)
        failed.load()
        assertEquals("版块目录加载失败，请检查网络后重试", failed.state.error)
        assertEquals(false, failed.state.loading)
    }
}
