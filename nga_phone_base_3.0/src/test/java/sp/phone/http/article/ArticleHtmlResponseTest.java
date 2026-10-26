package sp.phone.http.article;

import static org.junit.Assert.*;

import com.alibaba.fastjson.JSONObject;

import io.reactivex.Observable;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        JSONObject data = ArticleResponse.decode(HTML);
        assertEquals(12, data.getJSONObject("__T").getIntValue("tid"));
        assertEquals("主题 & 标题", data.getJSONObject("__T").getString("subject"));
        assertEquals(43, data.getIntValue("__ROWS"));
        JSONObject row = data.getJSONObject("__R").getJSONObject("0");
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
        JSONObject data = ArticleResponse.decode(source);
        JSONObject row = data.getJSONObject("__R").getJSONObject("0");
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
    public void truncatedApiUsesWebpageWithAccountAndFiltersPreserved() {
        List<String> urls = new ArrayList<>();
        ArticlePageLoader<JSONObject> loader =
                new ArticlePageLoader<>(
                        (url, headers) -> {
                            urls.add(url);
                            assertEquals("fixture-account", headers.get("Cookie"));
                            assertTrue(url.contains("authorid=9&page=2"));
                            return Observable.just(
                                    ResponseBody.create(
                                            MediaType.parse("text/plain; charset=UTF-8"),
                                            urls.size() == 1 ? "{\"data\":" : HTML));
                        },
                        json -> com.alibaba.fastjson.JSON.parseObject(json).getJSONObject("data"));
        JSONObject data =
                loader.load(
                                "https://bbs.nga.cn/read.php?tid=12&authorid=9&page=2",
                                Map.of("Cookie", "fixture-account"))
                        .blockingFirst();
        assertEquals(2, urls.size());
        assertFalse(urls.get(1).contains("__output="));
        assertEquals(1, data.getIntValue("__R__ROWS"));
    }

    @Test
    public void webpageBusinessErrorsStopRequests() {
        new ArticlePageLoader<>(
                        (url, headers) ->
                                Observable.just(
                                        ResponseBody.create(
                                                MediaType.parse("text/html; charset=UTF-8"),
                                                "<!DOCTYPE"
                                                    + " html><html><body><!--msginfostart--><b>请先登录</b><!--msginfoend--></body></html>")),
                        json -> json)
                .load("https://bbs.nga.cn/read.php?tid=12", null)
                .test()
                .assertError(
                        e ->
                                e instanceof ArticlePageLoader.BusinessException
                                        && e.getMessage().equals("请先登录"));
    }

    @Test
    public void topicIdentityAndRowCountMustMatchBeforeConversion() {
        for (String response :
                new String[] {
                    HTML.replace(",0,12,9,", ",0,13,9,"),
                    "{\"data\":{\"__T\":{\"tid\":12},\"__R\":{},\"__ROWS\":1,\"__R__ROWS\":2}}"
                }) {
            new ArticlePageLoader<>(
                            (url, headers) ->
                                    Observable.just(
                                            ResponseBody.create(
                                                    MediaType.parse("text/html; charset=UTF-8"),
                                                    response)),
                            json -> {
                                fail("不应转换无效数据");
                                return json;
                            })
                    .load("https://bbs.nga.cn/read.php?tid=12", null)
                    .test()
                    .assertError(ArticlePageLoader.ResponseFormatException.class);
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
        JSONObject data = ArticleResponse.decode(source);
        assertEquals(1, data.getIntValue("__R__ROWS"));
        JSONObject hot =
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
