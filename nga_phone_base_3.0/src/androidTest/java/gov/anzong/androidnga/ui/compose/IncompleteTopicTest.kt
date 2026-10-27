package gov.anzong.androidnga.ui.compose

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import sp.phone.http.article.ArticlePageLoader
import sp.phone.http.bean.ThreadData
import sp.phone.mvp.model.convert.ArticleConvertFactory

/** 只使用合成响应，避免将真实主题正文或账号数据纳入测试资源。 */
class IncompleteTopicTest {
    @Test fun incompleteDocumentIsRetriedBeforeNativeConversionOrDisplay() {
        val source = """{"data":{"__T":{"tid":47787244,"fid":7,"subject":"回归主题"},"__R":{"0":{"tid":47787244,"pid":1,"authorid":9,"content":"正文 [b]内容[/b]","lou":1}},"__U":{"9":{"username":"回归用户"}},"__ROWS":23,"__R__ROWS":1}}"""
        var requests = 0
        var parses = 0
        val loader = ArticlePageLoader<ThreadData>({ url, headers ->
            requests++
            assertEquals("fixture-account", headers["Cookie"])
            if (requests == 2) {
                assertTrue(url.contains("nga_client_retry="))
                assertEquals("no-cache, no-store", headers["Cache-Control"])
            }
            ((
                if (requests == 1) source.dropLast(1) else source).toResponseBody("application/json; charset=UTF-8".toMediaType()))
        }, { json -> parses++; ArticleConvertFactory.getArticleInfo(json) })
        val result = runBlocking { loader.load("https://bbs.nga.cn/read.php?tid=47787244&page=1", mapOf("Cookie" to "fixture-account")) }
        assertEquals(2,requests)
        assertEquals(1,parses)
        assertEquals(47787244,result.threadInfo.tid)
        assertEquals(23,result.__ROWS)
        assertEquals("回归用户",result.rowList.single().author)
        assertTrue(result.rowList.single().formattedHtmlData.contains("内容"))
    }

    @Test fun webpageResponseUsesNativeContentConversionAndCacheSchema() {
        val html = """<!DOCTYPE html><html><head></head><body>
            <h1 id="currentTopicName">网页兼容主题</h1>
            <script>commonui.userInfo.setAll({"9":{"uid":9,"username":"回归用户"}});
            commonui.postArg.setDefault(7,0,12,9,0,"","","","",null,0,22,1792995370,20);</script>
            <!--postliststart--><table><tr class="postrow"><td id="postcontainer0">
            <span id="postdate0">2026-10-26 14:16</span><h3 id="postsubject0"></h3>
            <span id="postcontent0">[b]网页正文[/b]<br>[img]https://img.nga.cn/a.png[/img]</span>
            </td></tr></table><script>commonui.postArg.proc(0,null,null,null,null,null,null,null,
            null,null,0,0,null,'9',1792995370,'2,0,0','10','','','7 iOS','',null,0);</script>
            <!--postlistend--></body></html>"""
        assertEquals(12, sp.phone.http.article.ArticleResponse.decode(html).getJSONObject("__T").getIntValue("tid"))
        var requests = 0
        val loader = ArticlePageLoader<ThreadData>({ _, _ ->
            requests++
            ((
                if (requests == 1) "{\"data\":" else html).toResponseBody("text/html; charset=UTF-8".toMediaType()))
        }, ArticleConvertFactory::getArticleInfo)
        val data = runBlocking { loader.load("https://bbs.nga.cn/read.php?tid=12&page=1", emptyMap()) }
        assertEquals(2, requests)
        assertEquals(12, data.threadInfo.tid)
        assertEquals("ios", data.rowList.single().fromClientModel)
        assertEquals("回归用户", data.rowList.single().author)
        assertTrue(data.rowList.single().formattedHtmlData.contains("网页正文"))
        assertTrue(data.rowList.single().imageUrls.any { it.contains("a.png") })
        val cached = ArticleConvertFactory.getArticleInfo(data.rawData)
        assertNotNull(cached)
        assertEquals(data.rowList.single().pid, cached.rowList.single().pid)
    }

    @Test fun imageMarkupAndGalleryUseCurrentHostAcrossApiAndWebFormats() {
        val decoder = gov.anzong.androidnga.core.decode.ForumImageDecoder()
        val html = decoder.decode("[img]./mon_202610/26/sample.webp[/img]" +
            "<img class=\"test\" src=\"http://img.nga.178.com/attachments/other.jpg\">", null)
        assertTrue(html.contains("https://img.nga.cn/attachments/mon_202610/26/sample.webp"))
        assertFalse(html.contains("img.nga.178.com"))
        assertEquals(listOf("https://img.nga.cn/attachments/mon_202610/26/sample.webp",
            "https://img.nga.cn/attachments/other.jpg"), decoder.imageUrls)
        val hidden = gov.anzong.androidnga.core.data.HtmlData("正文")
        hidden.setShowImage(false)
        assertTrue(decoder.decode("[img]./mon_202610/26/sample.webp[/img]", hidden).contains("ic_offline_image.png"))
        assertEquals(1, decoder.imageUrls.size)
    }

    @Test fun plainTextConversionPreservesEntitiesAndLeavesAttachmentsAndHiddenRowsRich() {
        val row = sp.phone.http.bean.ThreadRowInfo()
        row.content = "A &amp; B<br>第二行"
        assertEquals("A & B\n第二行", plainArticleText(row))
        row.alterinfo = "编辑提示"
        assertNull(plainArticleText(row))
        row.alterinfo = null
        row.content = ""
        assertNull(plainArticleText(row))
        row.content = "[img]./mon_test/a.webp[/img]"
        assertNull(plainArticleText(row))
    }
}
