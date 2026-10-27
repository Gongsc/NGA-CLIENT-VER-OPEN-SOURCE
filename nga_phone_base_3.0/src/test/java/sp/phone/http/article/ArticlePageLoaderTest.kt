package sp.phone.http.article

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonObject
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.nio.charset.Charset

class ArticlePageLoaderTest {

    private fun loader(fetch: suspend (String, Map<String, String>) -> ResponseBody) =
        ArticlePageLoader(fetch) { json -> NgaJson.parseObject(json).getJSONObject("data") }

    private fun body(source: String) = source.toResponseBody("text/plain; charset=UTF-8".toMediaType())

    private fun <T> ArticlePageLoader<T>.loadNow(url: String = URL, headers: Map<String, String>? = null): T =
        runBlocking { load(url, headers) }

    private inline fun <reified E : Throwable> ArticlePageLoader<*>.failure(url: String = URL): E {
        val error = try {
            loadNow(url)
            null
        } catch (e: Throwable) {
            e
        }
        if (error !is E) throw AssertionError("应抛出 ${E::class.java.simpleName}，实际为 $error", error)
        return error
    }

    @Test
    fun validJsonNeedsOneRequest() {
        val urls = mutableListOf<String>()
        val result = loader { url, _ -> urls += url; body(JSON_DATA) }.loadNow()
        assertEquals(1, urls.size)
        assertEquals("主题", result!!.getJSONObject("__T").getString("subject"))
    }

    @Test
    fun htmlResponseRetriesXmlWithSameAccountAndParameters() {
        val urls = mutableListOf<String>()
        val original = hashMapOf("Cookie" to "test-account")
        val result = loader { url, headers ->
            urls += url
            assertTrue(url.startsWith("$URL&"))
            assertEquals("test-account", headers["Cookie"])
            assertEquals("NGA_WP_JW/(;WINDOWS)", headers["User-Agent"])
            assertEquals(headers["User-Agent"], headers["X-User-Agent"])
            body(if (url.contains("lite=xml")) XML_DATA else "<html>暂不可用</html>")
        }.loadNow(headers = original)!!
        assertEquals(3, urls.size)
        assertEquals(1, original.size)
        assertEquals("正文 <br> 内容", result.getJSONObject("__R").getJSONObject("0").getString("content"))
        assertEquals("用户", result.getJSONObject("__U").getJSONObject("56").getString("username"))
        assertEquals(21, result.getIntValue("__ROWS"))
        assertEquals("会员", result.getJSONObject("__U").getJSONObject("__GROUPS").getString("1"))
    }

    @Test
    fun incompatibleResponsesStopAfterFourSources() {
        val urls = mutableListOf<String>()
        loader { url, _ -> urls += url; body("<html>稍后重试</html>") }
            .failure<ArticlePageLoader.ResponseFormatException>()
        assertEquals(4, urls.size)
        assertTrue(urls[3].contains("&__output=10&nga_client_retry="))
    }

    @Test
    fun apiBusinessErrorsDoNotRetryOtherFormats() {
        for (response in listOf(
            "{\"error\":[\"无权限\"]}",
            "{\"data\":{\"__MESSAGE\":{\"1\":\"请登录\"}}}",
            "<root><__MESSAGE><item>错误</item><item>主题不存在</item></__MESSAGE></root>",
        )) {
            val urls = mutableListOf<String>()
            loader { url, _ -> urls += url; body(response) }.failure<ArticlePageLoader.BusinessException>()
            assertEquals(1, urls.size)
        }
    }

    @Test
    fun transportErrorsDoNotCauseFormatRetries() {
        val urls = mutableListOf<String>()
        loader { url, _ -> urls += url; throw IOException("网络不可用") }.failure<IOException>()
        assertEquals(1, urls.size)
    }

    @Test
    fun charsetDefaultsToGb18030AndHonorsUtf8Header() {
        for (charset in listOf("UTF-8", "GB18030")) {
            val result = loader { _, _ ->
                JSON_DATA.toByteArray(Charset.forName(charset))
                    .toResponseBody(("text/plain" + if (charset == "UTF-8") "; charset=UTF-8" else "").toMediaType())
            }.loadNow()!!
            assertEquals("主题", result.getJSONObject("__T").getString("subject"))
        }
    }

    @Test
    fun legacyNumericContentsRetainLeadingZerosAndPlusSign() {
        val data = ArticleResponse.decode(
            "/*\$js\$*/{\"data\":{\"content\":+123,\"subject\":00123,\"author\":0123,\"tail\":0}}/*error fill content */")
        assertEquals("+123", data.getString("content"))
        assertEquals("00123", data.getString("subject"))
        assertEquals("0123", data.getString("author"))
    }

    @Test
    fun xmlExternalEntitiesAreRejected() {
        try {
            ArticleResponse.decode("<!DOCTYPE root [<!ENTITY x SYSTEM 'file:///etc/hosts'>]><root><__T>&x;</__T></root>")
            fail("外部实体必须被拒绝")
        } catch (expected: IllegalArgumentException) {
            assertEquals("不支持的 XML 声明", expected.message)
        }
    }

    @Test
    fun nullOrEmptyDataIsReportedAsFormatError() {
        for (source in listOf("null", "", "{}", "{\"data\":null}", "{\"data\":{\"__T\":[],\"__R\":[]}}")) {
            loader { _, _ -> body(source) }.failure<ArticlePageLoader.ResponseFormatException>()
        }
    }

    @Test
    fun xmlWithoutExplicitRowCountUsesParsedReplyCount() {
        val result = loader { _, _ -> body(XML_DATA.replace("<__R__ROWS>1</__R__ROWS>", "")) }.loadNow()!!
        assertEquals(1, result.getIntValue("__R__ROWS"))
    }

    @Test
    fun incompleteJsonDetectionRespectsQuotedDelimiters() {
        val data = NgaJsonObject()
        data["content"] = "literal } ] { [ and \"quote\" and backslash \\"
        val root = NgaJsonObject()
        root["data"] = data
        val source = root.toJSONString()
        assertFalse(ArticleResponse.isIncomplete(source))
        assertTrue(ArticleResponse.isIncomplete(source.substring(0, source.length - 1)))
        assertTrue(ArticleResponse.isIncomplete("{\"data\":{\"content\":\"unfinished"))
        assertFalse(ArticleResponse.isIncomplete("{]"))
        assertFalse(ArticleResponse.isIncomplete("{\"data\":{}}/* server trailer */"))
    }

    @Test
    fun incompleteXmlOnlyAppliesToTopicDocuments() {
        assertTrue(ArticleResponse.isIncomplete("<?xml version='1.0'?><root><__R><item><content><![CDATA[正文"))
        assertFalse(ArticleResponse.isIncomplete("<root><__R/></root>"))
        assertFalse(ArticleResponse.isIncomplete("<root></root><!-- server trailer -->"))
        assertFalse(ArticleResponse.isIncomplete("<html>login"))
    }

    @Test
    fun truncatedJsonSwitchesSourceWithSameAccount() {
        val urls = mutableListOf<String>()
        val account = hashMapOf("Cookie" to "same-account")
        val result = loader { url, headers ->
            urls += url
            assertEquals("same-account", headers["Cookie"])
            assertTrue(url.startsWith(URL))
            if (urls.size == 1) return@loader body(JSON_DATA.substring(0, JSON_DATA.length - 1))
            assertTrue(url.contains("nga_client_retry="))
            assertEquals("no-cache, no-store", headers["Cache-Control"])
            assertEquals("no-cache", headers["Pragma"])
            body(JSON_DATA)
        }.loadNow(headers = account)!!
        assertEquals(2, urls.size)
        assertEquals(1, account.size)
        assertEquals(21, result.getIntValue("__ROWS"))
    }

    @Test
    fun truncatedResponsesStopAtFourRequests() {
        val urls = mutableListOf<String>()
        val error = loader { url, _ -> urls += url; body("{\"data\":") }
            .failure<ArticlePageLoader.ResponseFormatException>()
        assertTrue(error.isIncomplete)
        assertEquals(4, urls.size)
        assertTrue(urls[0].contains("__output=8"))
        assertFalse(urls[1].contains("__output="))
        assertTrue(urls[2].contains("lite=xml"))
        assertTrue(urls[3].contains("__output=10"))
    }

    @Test
    fun truncatedXmlContinuesToNextSource() {
        val urls = mutableListOf<String>()
        val result = loader { url, headers ->
            urls += url
            if (urls.size == 1) return@loader body("<html>unavailable</html>")
            assertTrue(url.startsWith(URL))
            assertEquals("no-cache, no-store", headers["Cache-Control"])
            body(if (urls.size < 4) "<root><__R>" else XML_DATA)
        }.loadNow()!!
        assertEquals(4, urls.size)
        assertEquals(21, result.getIntValue("__ROWS"))
    }

    @Test
    fun permissionErrorAfterIncompleteResponseStopsWithoutEmittingPartialRows() {
        val urls = mutableListOf<String>()
        loader { url, _ ->
            urls += url
            body(if (urls.size == 1) JSON_DATA.substring(0, JSON_DATA.length - 1) else "{\"error\":[\"请先登录\"]}")
        }.failure<ArticlePageLoader.BusinessException>()
        assertEquals(2, urls.size)
    }

    // ---- 网页来源（自 ArticleHtmlResponseTest 迁入） ----

    @Test
    fun truncatedApiUsesWebpageWithAccountAndFiltersPreserved() {
        val urls = mutableListOf<String>()
        val data = loader { url, headers ->
            urls += url
            assertEquals("fixture-account", headers["Cookie"])
            assertTrue(url.contains("authorid=9&page=2"))
            body(if (urls.size == 1) "{\"data\":" else ArticleHtmlResponseTest.HTML)
        }.loadNow("https://bbs.nga.cn/read.php?tid=12&authorid=9&page=2", mapOf("Cookie" to "fixture-account"))!!
        assertEquals(2, urls.size)
        assertFalse(urls[1].contains("__output="))
        assertEquals(1, data.getIntValue("__R__ROWS"))
    }

    @Test
    fun webpageBusinessErrorsStopRequests() {
        val error = ArticlePageLoader({ _, _ ->
            ("<!DOCTYPE html><html><body><!--msginfostart--><b>请先登录</b><!--msginfoend--></body></html>")
                .toResponseBody("text/html; charset=UTF-8".toMediaType())
        }) { json -> json }.failure<ArticlePageLoader.BusinessException>("https://bbs.nga.cn/read.php?tid=12")
        assertEquals("请先登录", error.message)
    }

    @Test
    fun topicIdentityAndRowCountMustMatchBeforeConversion() {
        for (response in listOf(
            ArticleHtmlResponseTest.HTML.replace(",0,12,9,", ",0,13,9,"),
            "{\"data\":{\"__T\":{\"tid\":12},\"__R\":{},\"__ROWS\":1,\"__R__ROWS\":2}}",
        )) {
            ArticlePageLoader({ _, _ -> response.toResponseBody("text/html; charset=UTF-8".toMediaType()) }) { json ->
                fail("不应转换无效数据")
                json
            }.failure<ArticlePageLoader.ResponseFormatException>("https://bbs.nga.cn/read.php?tid=12")
        }
    }

    companion object {
        private const val URL = "https://bbs.nga.cn/read.php?tid=12&pid=34&page=2&authorid=56"
        private const val JSON_DATA =
            "{\"data\":{\"__T\":{\"tid\":12,\"subject\":\"主题\"},\"__R\":{\"0\":{\"content\":\"正文\"}},\"__ROWS\":21,\"__R__ROWS\":1}}"
        private const val XML_DATA =
            "<?xml version='1.0'?><root><__T><tid>12</tid><subject><![CDATA[主题 & 标题]]></subject></__T><__R><item><content><![CDATA[正文 <br> 内容]]></content></item></__R><__U><item><uid>56</uid><username>用户</username></item><__GROUPS><_1>会员</_1></__GROUPS></__U><__ROWS>21</__ROWS><__R__ROWS>1</__R__ROWS></root>"
    }
}
