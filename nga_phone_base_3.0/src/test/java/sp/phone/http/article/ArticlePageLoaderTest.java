package sp.phone.http.article;

import static org.junit.Assert.*;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import io.reactivex.Observable;

import okhttp3.MediaType;
import okhttp3.ResponseBody;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArticlePageLoaderTest {
    private static final String URL =
            "https://bbs.nga.cn/read.php?tid=12&pid=34&page=2&authorid=56";
    private static final String JSON_DATA =
            "{\"data\":{\"__T\":{\"tid\":12,\"subject\":\"主题\"},\"__R\":{\"0\":{\"content\":\"正文\"}},\"__ROWS\":21,\"__R__ROWS\":1}}";
    private static final String XML_DATA =
            "<?xml version='1.0'?><root><__T><tid>12</tid><subject><![CDATA[主题 &"
                + " 标题]]></subject></__T><__R><item><content><![CDATA[正文 <br>"
                + " 内容]]></content></item></__R><__U><item><uid>56</uid><username>用户</username></item><__GROUPS><_1>会员</_1></__GROUPS></__U><__ROWS>21</__ROWS><__R__ROWS>1</__R__ROWS></root>";

    private ArticlePageLoader<JSONObject> loader(ArticlePageLoader.Fetcher fetcher) {
        return new ArticlePageLoader<>(
                fetcher, json -> JSON.parseObject(json).getJSONObject("data"));
    }

    private ResponseBody body(String source) {
        return ResponseBody.create(MediaType.parse("text/plain; charset=UTF-8"), source);
    }

    @Test
    public void validJsonNeedsOneRequest() {
        List<String> urls = new ArrayList<>();
        JSONObject result =
                loader(
                                (url, headers) -> {
                                    urls.add(url);
                                    return Observable.just(body(JSON_DATA));
                                })
                        .load(URL, null)
                        .blockingFirst();
        assertEquals(1, urls.size());
        assertEquals("主题", result.getJSONObject("__T").getString("subject"));
    }

    @Test
    public void htmlResponseRetriesXmlWithSameAccountAndParameters() {
        List<String> urls = new ArrayList<>();
        Map<String, String> original = new HashMap<>();
        original.put("Cookie", "test-account");
        JSONObject result =
                loader(
                                (url, headers) -> {
                                    urls.add(url);
                                    assertTrue(url.startsWith(URL + "&"));
                                    assertEquals("test-account", headers.get("Cookie"));
                                    assertEquals("NGA_WP_JW/(;WINDOWS)", headers.get("User-Agent"));
                                    assertEquals(
                                            headers.get("User-Agent"), headers.get("X-User-Agent"));
                                    return Observable.just(
                                            body(
                                                    url.contains("lite=xml")
                                                            ? XML_DATA
                                                            : "<html>暂不可用</html>"));
                                })
                        .load(URL, original)
                        .blockingFirst();
        assertEquals(3, urls.size());
        assertEquals(1, original.size());
        assertEquals(
                "正文 <br> 内容", result.getJSONObject("__R").getJSONObject("0").getString("content"));
        assertEquals("用户", result.getJSONObject("__U").getJSONObject("56").getString("username"));
        assertEquals(21, result.getIntValue("__ROWS"));
        assertEquals("会员", result.getJSONObject("__U").getJSONObject("__GROUPS").getString("1"));
    }

    @Test
    public void incompatibleResponsesStopAfterFourSources() {
        List<String> urls = new ArrayList<>();
        loader(
                        (url, headers) -> {
                            urls.add(url);
                            return Observable.just(body("<html>稍后重试</html>"));
                        })
                .load(URL, null)
                .test()
                .assertError(ArticlePageLoader.ResponseFormatException.class);
        assertEquals(4, urls.size());
        assertTrue(urls.get(3).contains("&__output=10&nga_client_retry="));
    }

    @Test
    public void apiBusinessErrorsDoNotRetryOtherFormats() {
        for (String response :
                new String[] {
                    "{\"error\":[\"无权限\"]}",
                    "{\"data\":{\"__MESSAGE\":{\"1\":\"请登录\"}}}",
                    "<root><__MESSAGE><item>错误</item><item>主题不存在</item></__MESSAGE></root>"
                }) {
            List<String> urls = new ArrayList<>();
            loader(
                            (url, headers) -> {
                                urls.add(url);
                                return Observable.just(body(response));
                            })
                    .load(URL, null)
                    .test()
                    .assertError(ArticlePageLoader.BusinessException.class);
            assertEquals(1, urls.size());
        }
    }

    @Test
    public void transportErrorsDoNotCauseFormatRetries() {
        List<String> urls = new ArrayList<>();
        loader(
                        (url, headers) -> {
                            urls.add(url);
                            return Observable.error(new IOException("网络不可用"));
                        })
                .load(URL, null)
                .test()
                .assertError(IOException.class);
        assertEquals(1, urls.size());
    }

    @Test
    public void charsetDefaultsToGb18030AndHonorsUtf8Header() {
        for (String charset : new String[] {"UTF-8", "GB18030"}) {
            JSONObject result =
                    loader(
                                    (url, headers) ->
                                            Observable.just(
                                                    ResponseBody.create(
                                                            MediaType.parse(
                                                                    "text/plain"
                                                                            + (charset.equals(
                                                                                            "UTF-8")
                                                                                    ? "; charset=UTF-8"
                                                                                    : "")),
                                                            JSON_DATA.getBytes(
                                                                    Charset.forName(charset)))))
                            .load(URL, null)
                            .blockingFirst();
            assertEquals("主题", result.getJSONObject("__T").getString("subject"));
        }
    }

    @Test
    public void legacyNumericContentsRetainLeadingZerosAndPlusSign() throws Exception {
        JSONObject data =
                ArticleResponse.decode(
                        "/*$js$*/{\"data\":{\"content\":+123,\"subject\":00123,\"author\":0123,\"tail\":0}}/*error"
                            + " fill content */");
        assertEquals("+123", data.getString("content"));
        assertEquals("00123", data.getString("subject"));
        assertEquals("0123", data.getString("author"));
    }

    @Test
    public void xmlExternalEntitiesAreRejected() throws Exception {
        try {
            ArticleResponse.decode(
                    "<!DOCTYPE root [<!ENTITY x SYSTEM"
                            + " 'file:///etc/hosts'>]><root><__T>&x;</__T></root>");
            fail("外部实体必须被拒绝");
        } catch (IllegalArgumentException expected) {
            assertEquals("不支持的 XML 声明", expected.getMessage());
        }
    }

    @Test
    public void nullOrEmptyDataIsReportedAsFormatError() {
        for (String source :
                new String[] {
                    "null", "", "{}", "{\"data\":null}", "{\"data\":{\"__T\":[],\"__R\":[]}}"
                }) {
            loader((url, headers) -> Observable.just(body(source)))
                    .load(URL, null)
                    .test()
                    .assertError(ArticlePageLoader.ResponseFormatException.class);
        }
    }

    @Test
    public void xmlWithoutExplicitRowCountUsesParsedReplyCount() {
        JSONObject result =
                loader(
                                (url, headers) ->
                                        Observable.just(
                                                body(
                                                        XML_DATA.replace(
                                                                "<__R__ROWS>1</__R__ROWS>", ""))))
                        .load(URL, null)
                        .blockingFirst();
        assertEquals(1, result.getIntValue("__R__ROWS"));
    }

    @Test
    public void incompleteJsonDetectionRespectsQuotedDelimiters() {
        JSONObject data = new JSONObject();
        data.put("content", "literal } ] { [ and \"quote\" and backslash \\");
        JSONObject root = new JSONObject();
        root.put("data", data);
        String source = root.toJSONString();
        assertFalse(ArticleResponse.isIncomplete(source));
        assertTrue(ArticleResponse.isIncomplete(source.substring(0, source.length() - 1)));
        assertTrue(ArticleResponse.isIncomplete("{\"data\":{\"content\":\"unfinished"));
        assertFalse(ArticleResponse.isIncomplete("{]"));
        assertFalse(ArticleResponse.isIncomplete("{\"data\":{}}/* server trailer */"));
    }

    @Test
    public void incompleteXmlOnlyAppliesToTopicDocuments() {
        assertTrue(
                ArticleResponse.isIncomplete(
                        "<?xml version='1.0'?><root><__R><item><content><![CDATA[正文"));
        assertFalse(ArticleResponse.isIncomplete("<root><__R/></root>"));
        assertFalse(ArticleResponse.isIncomplete("<root></root><!-- server trailer -->"));
        assertFalse(ArticleResponse.isIncomplete("<html>login"));
    }

    @Test
    public void truncatedJsonSwitchesSourceWithSameAccount() {
        List<String> urls = new ArrayList<>();
        Map<String, String> account = new HashMap<>();
        account.put("Cookie", "same-account");
        JSONObject result =
                loader(
                                (url, headers) -> {
                                    urls.add(url);
                                    assertEquals("same-account", headers.get("Cookie"));
                                    assertTrue(url.startsWith(URL));
                                    if (urls.size() == 1)
                                        return Observable.just(
                                                body(
                                                        JSON_DATA.substring(
                                                                0, JSON_DATA.length() - 1)));
                                    assertTrue(url.contains("nga_client_retry="));
                                    assertEquals(
                                            "no-cache, no-store", headers.get("Cache-Control"));
                                    assertEquals("no-cache", headers.get("Pragma"));
                                    return Observable.just(body(JSON_DATA));
                                })
                        .load(URL, account)
                        .blockingFirst();
        assertEquals(2, urls.size());
        assertEquals(1, account.size());
        assertEquals(21, result.getIntValue("__ROWS"));
    }

    @Test
    public void truncatedResponsesStopAtFourRequests() {
        List<String> urls = new ArrayList<>();
        loader(
                        (url, headers) -> {
                            urls.add(url);
                            return Observable.just(body("{\"data\":"));
                        })
                .load(URL, null)
                .test()
                .assertError(
                        error ->
                                error instanceof ArticlePageLoader.ResponseFormatException
                                        && ((ArticlePageLoader.ResponseFormatException) error)
                                                .isIncomplete());
        assertEquals(4, urls.size());
        assertTrue(urls.get(0).contains("__output=8"));
        assertFalse(urls.get(1).contains("__output="));
        assertTrue(urls.get(2).contains("lite=xml"));
        assertTrue(urls.get(3).contains("__output=10"));
    }

    @Test
    public void truncatedXmlContinuesToNextSource() {
        List<String> urls = new ArrayList<>();
        JSONObject result =
                loader(
                                (url, headers) -> {
                                    urls.add(url);
                                    if (urls.size() == 1)
                                        return Observable.just(body("<html>unavailable</html>"));
                                    assertTrue(url.startsWith(URL));
                                    assertEquals(
                                            "no-cache, no-store", headers.get("Cache-Control"));
                                    return Observable.just(
                                            body(urls.size() < 4 ? "<root><__R>" : XML_DATA));
                                })
                        .load(URL, null)
                        .blockingFirst();
        assertEquals(4, urls.size());
        assertEquals(21, result.getIntValue("__ROWS"));
    }

    @Test
    public void permissionErrorAfterIncompleteResponseStopsWithoutEmittingPartialRows() {
        List<String> urls = new ArrayList<>();
        loader(
                        (url, headers) -> {
                            urls.add(url);
                            return Observable.just(
                                    body(
                                            urls.size() == 1
                                                    ? JSON_DATA.substring(0, JSON_DATA.length() - 1)
                                                    : "{\"error\":[\"请先登录\"]}"));
                        })
                .load(URL, null)
                .test()
                .assertNoValues()
                .assertError(ArticlePageLoader.BusinessException.class);
        assertEquals(2, urls.size());
    }
}
