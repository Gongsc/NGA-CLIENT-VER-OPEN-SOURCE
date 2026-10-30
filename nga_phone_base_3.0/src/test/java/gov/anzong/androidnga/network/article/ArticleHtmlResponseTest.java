package gov.anzong.androidnga.network.article;

import static org.junit.Assert.*;

import gov.anzong.androidnga.base.json.NgaJsonObject;

import org.junit.Test;

public class ArticleHtmlResponseTest {
    static final String HTML =
            """
<!DOCTYPE html><html><head></head><body>
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
34,0,null,'9',1792995370,'2,1,0','18','','','7 iOS (a,b)\\\'x','',null,0);</script>
<!--postlistend--></body></html>
""";

    @Test
    public void webpageNormalizesToSameSchemaWithoutExecutingScripts() throws Exception {
        NgaJsonObject data = ArticleResponse.decode(HTML);
        assertEquals(12, data.getJSONObject("__T").getIntValue("tid"));
        assertEquals("主题 & 标题", data.getJSONObject("__T").getString("subject"));
        assertEquals(43, data.getIntValue("__ROWS"));
        NgaJsonObject row = data.getJSONObject("__R").getJSONObject("0");
        assertEquals(20, row.getIntValue("lou"));
        assertEquals(34, row.getIntValue("pid"));
        assertEquals(2, row.getIntValue("score"));
        assertEquals("7 iOS (a,b)'x", row.getString("from_client"));
        assertTrue(row.getString("content").contains("[b]内容[/b]"));
        assertTrue(row.getString("content").contains("a.png"));
        assertFalse(row.getString("content").contains("bad()"));
        assertEquals("测试用户", data.getJSONObject("__U").getJSONObject("9").getString("username"));
    }

    @Test
    public void attachmentsAndAlertsArePreservedAndAuthorPaginationUsesPageMetadata()
            throws Exception {
        String source =
                HTML.replace(
                                "</td>",
                                "<script>ubbcode.attach.load($('postattach20'),"
                                    + " [{url:'a/test.png',type:'img',thumb:'1',name:'a,b'}]);</script></td>")
                        .replace(
                                "<!--postlistend-->",
                                "<script>commonui.loadAlertInfo('编辑说明','alertc20');var"
                                    + " __PAGE={0:'/read.php?tid=12',1:1,2:1,3:20};</script><!--postlistend-->");
        NgaJsonObject data = ArticleResponse.decode(source);
        NgaJsonObject row = data.getJSONObject("__R").getJSONObject("0");
        assertEquals(
                "a/test.png",
                row.getJSONObject("attachs").getJSONObject("0").getString("attachurl"));
        assertEquals("编辑说明", row.getString("alterinfo"));
        assertEquals(1, data.getIntValue("__ROWS"));
    }

    @Test
    public void incompleteOrMismatchedHtmlIsRejected() {
        for (String source :
                new String[] {
                    HTML.replace("<!--postlistend-->", ""),
                    HTML.replace("postcontainer20'", "postcontainer21'"),
                    HTML.substring(0, HTML.indexOf("<!--postlistend-->")),
                    HTML.replace("$('postcontainer20')", "$('postcontainer21')")
                            .replace("20,$", "21,$")
                }) {
            try {
                ArticleHtmlResponse.decode(source);
                fail("不能将部分楼层当成完整页");
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    @Test
    public void webpageHotRepliesAreSeparateFromNormalFloors() throws Exception {
        String source =
                HTML.replace("postcontainer20", "postcontainer0")
                        .replace("postdate20", "postdate0")
                        .replace("postsubject20", "postsubject0")
                        .replace("postcontent20", "postcontent0")
                        .replace("proc(20,", "proc(0,")
                        .replace("34,0,null", "0,0,null")
                        .replace(
                                "</td>",
                                "<span id='hightlight_for_0'><span"
                                    + " id='postcommentsubject__99'></span><span"
                                    + " id='postcomment__99'>热点正文 [b]加粗[/b]</span><span"
                                    + " id='commentInfo__99'>2026-10-26</span></span></td>")
                        .replace(
                                "<!--postlistend-->",
                                "<script>commonui.postArg.proc('__99',null,null,null,null,null,null,null,null,null,99,0,null,'9',1792995370,'18,0,0','18','','','7"
                                    + " iOS','',null,0);</script><!--postlistend-->");
        NgaJsonObject data = ArticleResponse.decode(source);
        assertEquals(1, data.getIntValue("__R__ROWS"));
        NgaJsonObject hot =
                data.getJSONObject("__R")
                        .getJSONObject("0")
                        .getJSONObject("hotreply")
                        .getJSONObject("0");
        assertEquals(99, hot.getIntValue("pid"));
        assertEquals(-1, hot.getIntValue("lou"));
        assertEquals(18, hot.getIntValue("score"));
        assertEquals("热点正文 [b]加粗[/b]", hot.getString("content"));
    }
}
