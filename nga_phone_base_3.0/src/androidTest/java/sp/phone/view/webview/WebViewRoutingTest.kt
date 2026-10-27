package sp.phone.view.webview

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import gov.anzong.androidnga.activity.ArticleListActivity
import org.junit.Assert.*
import org.junit.Test
import gov.anzong.androidnga.base.json.NgaJson
import sp.phone.http.article.ArticlePageLoader
import sp.phone.mvp.model.convert.ArticleConvertFactory
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

class WebViewRoutingTest {
    private fun withWebView(block: (WebView, MutableList<Intent>) -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val intents = mutableListOf<Intent>()
            val context = object : ContextWrapper(ApplicationProvider.getApplicationContext<Context>()) {
                override fun startActivity(intent: Intent) { intents += intent }
            }
            val webView = WebView(context)
            try { block(webView, intents) } finally { webView.destroy() }
        }
    }

    @Test fun topicLinkDispatchesExplicitNativeIntentAndRetainsParameters() = withWebView { webView, intents ->
        val client = WebViewClientEx()
        for (url in listOf("/read.php?tid=12&page=3#pid45", "https://nga.178.com/read.php?pid=45", "//ngabbs.com/read.php?tid=12")) {
            assertTrue(client.shouldOverrideUrlLoading(webView, url))
            assertEquals(ArticleListActivity::class.java.name, intents.last().component!!.className)
            assertNull(intents.last().action)
        }
        assertEquals("3", intents.first().data!!.getQueryParameter("page"))
        assertEquals("pid45", intents.first().data!!.fragment)
    }

    @Test fun externalWebLinkStillUsesBrowserOnUserNavigation() = withWebView { webView, intents ->
        WebViewClientEx().shouldOverrideUrlLoading(webView, "https://developer.android.com/compose")
        assertEquals(Intent.ACTION_VIEW, intents.single().action)
    }

    @Test fun subframesDoNotStartActivities() = withWebView { webView, intents ->
        val request = object : WebResourceRequest {
            override fun getUrl() = Uri.parse("https://ngabbs.com/read.php?tid=12")
            override fun isForMainFrame() = false
            override fun isRedirect() = false
            override fun hasGesture() = false
            override fun getMethod() = "GET"
            override fun getRequestHeaders() = emptyMap<String, String>()
        }
        assertFalse(WebViewClientEx().shouldOverrideUrlLoading(webView, request))
        assertTrue(intents.isEmpty())
    }

    @Test fun scriptsAndAnchorsDoNotLaunchApplications() = withWebView { webView, intents ->
        val client = WebViewClientEx()
        assertTrue(client.shouldOverrideUrlLoading(webView, "javascript:alert(1)"))
        assertFalse(client.shouldOverrideUrlLoading(webView, "#pid12"))
        assertTrue(intents.isEmpty())
    }

    @Test fun xmlCompatibilityResponseBuildsNativeArticleRows() {
        val xml = """<root><__T><tid>12</tid><fid>7</fid><subject>兼容主题</subject></__T><__R><item><tid>12</tid><pid>34</pid><authorid>56</authorid><content><![CDATA[原生正文 [b]内容[/b]]]></content><postdate>2026-10-26</postdate><lou>1</lou></item></__R><__U><item><uid>56</uid><username>兼容用户</username></item></__U><__ROWS>21</__ROWS><__R__ROWS>1</__R__ROWS></root>"""
        val result = ArticlePageLoader({ _, _ -> (xml.toResponseBody("text/xml; charset=UTF-8".toMediaType())) }, ArticleConvertFactory::getArticleInfo)
            .let { loader -> runBlocking { loader.load("https://bbs.nga.cn/read.php?tid=12&page=2", emptyMap()) } }
        assertEquals(21, result.__ROWS)
        assertEquals(12, result.threadInfo.tid)
        assertEquals("兼容用户", result.rowList.single().author)
        assertTrue(result.rowList.single().formattedHtmlData.contains("内容"))
        assertTrue(NgaJson.parseObject(result.rawData).containsKey("data"))
        val invalid = NgaJson.parseObject(result.rawData)
        invalid.getJSONObject("data")["__R__ROWS"] = "invalid"
        assertNull(ArticleConvertFactory.getArticleInfo(invalid.toJSONString()))
    }
    @Test fun configuredReadUserAgentSurvivesSharedNetworkInterceptors() {
        val client = sp.phone.http.retrofit.RetrofitHelper.getInstance().createOkHttpClientBuilder()
            .addInterceptor { chain ->
                assertEquals("NGA_WP_JW/(;WINDOWS)", chain.request().header("User-Agent"))
                assertEquals("NGA_WP_JW/(;WINDOWS)", chain.request().header("X-User-Agent"))
                assertEquals("fixture-account", chain.request().header("Cookie"))
                okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200).message("OK").body("fixture".toResponseBody("text/plain".toMediaType())).build()
            }.build()
        val request = okhttp3.Request.Builder().url("https://bbs.nga.cn/read.php?tid=12")
            .header("User-Agent", "NGA_WP_JW/(;WINDOWS)").header("X-User-Agent", "NGA_WP_JW/(;WINDOWS)")
            .header("Cookie", "fixture-account").build()
        client.newCall(request).execute().use { assertEquals("fixture", it.body!!.string()) }
    }

}
