package gov.anzong.androidnga.network.article

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArticleHtmlResponseTest {

    @Test
    fun webpageNormalizesToSameSchemaWithoutExecutingScripts() {
        val data = ArticleResponse.decode(HTML)
        assertEquals(12, data.getJSONObject("__T")!!.getIntValue("tid"))
        assertEquals("主题 & 标题", data.getJSONObject("__T")!!.getString("subject"))
        assertEquals(43, data.getIntValue("__ROWS"))
        val row = data.getJSONObject("__R")!!.getJSONObject("0")!!
        assertEquals(20, row.getIntValue("lou"))
        assertEquals(34, row.getIntValue("pid"))
        assertEquals(2, row.getIntValue("score"))
        assertEquals("7 iOS (a,b)'x", row.getString("from_client"))
        val content = row.getString("content")!!
        assertTrue(content.contains("[b]内容[/b]"))
        assertTrue(content.contains("a.png"))
        assertFalse(content.contains("bad()"))
        assertEquals("测试用户", data.getJSONObject("__U")!!.getJSONObject("9")!!.getString("username"))
    }

    @Test
    fun attachmentsAndAlertsArePreservedAndAuthorPaginationUsesPageMetadata() {
        val data = ArticleResponse.decode(DecodeGoldenInputs.HTML_ATTACHMENTS)
        val row = data.getJSONObject("__R")!!.getJSONObject("0")!!
        assertEquals("a/test.png", row.getJSONObject("attachs")!!.getJSONObject("0")!!.getString("attachurl"))
        assertEquals("编辑说明", row.getString("alterinfo"))
        assertEquals(1, data.getIntValue("__ROWS"))
    }

    @Test
    fun incompleteOrMismatchedHtmlIsRejected() {
        val sources = listOf(
            HTML.replace("<!--postlistend-->", ""),
            HTML.replace("postcontainer20'", "postcontainer21'"),
            HTML.substring(0, HTML.indexOf("<!--postlistend-->")),
            HTML.replace("\$('postcontainer20')", "\$('postcontainer21')").replace("20,\$", "21,\$"),
        )
        for (source in sources) {
            try {
                ArticleHtmlResponse.decode(source)
                fail("不能将部分楼层当成完整页")
            } catch (expected: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun webpageHotRepliesAreSeparateFromNormalFloors() {
        val data = ArticleResponse.decode(DecodeGoldenInputs.HTML_HOT_REPLY)
        assertEquals(1, data.getIntValue("__R__ROWS"))
        val hot = data.getJSONObject("__R")!!.getJSONObject("0")!!.getJSONObject("hotreply")!!.getJSONObject("0")!!
        assertEquals(99, hot.getIntValue("pid"))
        assertEquals(-1, hot.getIntValue("lou"))
        assertEquals(18, hot.getIntValue("score"))
        assertEquals("热点正文 [b]加粗[/b]", hot.getString("content"))
    }

    companion object {

        /** 合成的论坛网页：一层楼、作者信息与启动脚本。 */
        const val HTML = """<!DOCTYPE html><html><head></head><body>
<h1 id='currentTopicName'>主题 &amp; 标题</h1>
<script>
commonui.userInfo.setAll({"9":{"uid":9,"username":"测试用户","avatar":null}});
commonui.postArg.setDefault(-7,0,12,9,0,"","","","",null,4096,42,1792999440,20);
</script>
<!--postliststart-->
<table><tr class='postrow'><td id='postcontainer20'>
<span id='postdate20'>2026-10-26 14:16</span><h3 id='postsubject20'></h3>
<span id='postcontent20'>正文 [b]内容[/b]<br><span>括号 (a,b) &amp; 引号</span>
<img src='https://img.nga.cn/a.png' onerror='bad()'><script>bad()</script></span>
</td></tr></table>
<script>commonui.postArg.proc(20,$('postcontainer20'),$('postsubject20'),$('postcontent20'),
$('postsign20'),$('posterinfo20'),$('postInfo20'),$('postBtnPos20'),null,null,
34,0,null,'9',1792995370,'2,1,0','18','','','7 iOS (a,b)\'x','',null,0);</script>
<!--postlistend--></body></html>
"""
    }
}
