package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class ArticleHtmlTest {
    @get:Rule val compose = createComposeRule()

    @Test fun htmlBodyReportsHeightWithoutClippingLongContent() {
        val html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head><body>" +
            (1..30).joinToString("") { "<p>本地正文第 $it 行</p>" } + "</body></html>"
        compose.setContent { NgaTheme { Box(Modifier.width(240.dp)) { ArticleHtml(html, emptyList()) } } }
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onNodeWithTag("article_html").fetchSemanticsNode().boundsInRoot.height > 300
        }
        compose.onNodeWithTag("article_html").assertHeightIsAtLeast(96.dp)
    }

    @Test fun recycledWebViewReportsHeightToCurrentFloorOnly() {
        val first = java.util.concurrent.atomic.AtomicReference(0f)
        val second = java.util.concurrent.atomic.AtomicReference(0f)
        val phase = androidx.compose.runtime.mutableIntStateOf(0)
        lateinit var browser: ArticleContentWebView
        val longHtml = "<html><body>" + (1..40).joinToString("") { "<p>长正文第 $it 行</p>" } + "</body></html>"
        compose.setContent {
            val document = if (phase.intValue == 0) longHtml else "<html><body>短正文</body></html>"
            val target = if (phase.intValue == 0) first else second
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.width(240.dp).height(500.dp),
                factory = { ArticleContentWebView(it).also { view -> browser = view } },
                update = { it.bind(document, emptyList()) { value -> target.set(value) } },
                onRelease = { it.release() })
        }
        compose.waitUntil(10000) { first.get() > 100 }
        val firstHeight = first.get()
        compose.runOnIdle { browser.resetForReuse(); phase.intValue = 1 }
        compose.waitUntil(10000) { second.get() > 0 }
        org.junit.Assert.assertTrue(second.get() < firstHeight)
        org.junit.Assert.assertEquals(firstHeight, first.get())
    }

    @Test fun scrollingReusesContentViewsInsteadOfRecreatingEveryFloor() {
        val root = java.util.concurrent.atomic.AtomicReference<android.view.View>()
        val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<ArticleContentWebView, Boolean>())
        compose.setContent {
            root.set(androidx.compose.ui.platform.LocalView.current)
            NgaTheme {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.width(240.dp).then(Modifier.testTag("scroll_reader"))) {
                    items(40, key = { it }, contentType = { "floor" }) { floor ->
                        ArticleHtml("<html><body><p>楼层 $floor</p><p>正文</p></body></html>", emptyList())
                    }
                }
            }
        }
        fun collect(view: android.view.View) {
            if(view is ArticleContentWebView) seen.add(view)
            if(view is android.view.ViewGroup) for(i in 0 until view.childCount) collect(view.getChildAt(i))
        }
        for(index in (0..36 step 4) + (36 downTo 0 step 4)) {
            compose.onNodeWithTag("scroll_reader").performScrollToIndex(index)
            compose.runOnIdle { collect(root.get()) }
        }
        org.junit.Assert.assertTrue(seen.isNotEmpty())
        org.junit.Assert.assertTrue("滚动创建了 ${seen.size} 个正文容器", seen.size < 40)
    }
}
