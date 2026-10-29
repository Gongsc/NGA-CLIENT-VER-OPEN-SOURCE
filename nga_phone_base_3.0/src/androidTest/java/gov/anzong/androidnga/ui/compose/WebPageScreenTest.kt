package gov.anzong.androidnga.ui.compose

import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 网页浏览外壳；加载本地空白页，菜单只触发回调。 */
class WebPageScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsProgressRefreshesAndRunsMenuActions() {
        val events = mutableListOf<String>()
        var progress by mutableIntStateOf(30)
        lateinit var web: WebView
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            web = WebView(InstrumentationRegistry.getInstrumentation().targetContext).apply { loadUrl("about:blank") }
        }
        compose.setContent {
            NgaTheme {
                WebPageScreen("网页标题", progress, web, onRefresh = { events += "refresh" }, onBack = { events += "back" },
                    menu = listOf("复制链接" to { events += "copy" }))
            }
        }
        compose.onNodeWithText("网页标题").assertIsDisplayed()
        compose.onNodeWithTag("web_page").assertExists()
        compose.onNodeWithTag("web_progress").assertExists()
        progress = 100
        compose.onNodeWithTag("web_progress").assertDoesNotExist()
        compose.onNodeWithContentDescription("刷新").performClick()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("复制链接").performClick()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.runOnIdle { assertEquals(listOf("refresh", "copy", "back"), events) }
    }

    @Test fun withoutMenuItemsTheMenuButtonIsHidden() {
        compose.setContent { NgaTheme { WebPageScreen("查看内容", 100, null, {}, {}) } }
        compose.onNodeWithContentDescription("更多操作").assertDoesNotExist()
        compose.onNodeWithContentDescription("刷新").assertIsDisplayed()
    }
}
