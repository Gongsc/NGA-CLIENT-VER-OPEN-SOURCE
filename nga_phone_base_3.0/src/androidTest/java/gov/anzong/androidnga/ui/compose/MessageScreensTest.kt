package gov.anzong.androidnga.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import sp.phone.http.bean.MessageArticlePageInfo
import sp.phone.http.bean.MessageThreadPageInfo

/** 短消息列表、会话与写信页面；只使用本地合成数据与回调，不发送短消息。 */
class MessageScreensTest {
    @get:Rule val compose = createComposeRule()

    private fun thread(mid: Int) = MessageThreadPageInfo().apply {
        this.mid = mid; subject = "会话$mid"; from_username = "用户$mid"; posts = mid; lastTime = "2026-10-29"
    }

    private fun entry(lou: Int, author: String) = MessageArticlePageInfo().apply {
        this.lou = lou; this.author = author; time = "10:0$lou"; formated_html_data = "<html><body>消息$lou</body></html>"
    }

    @Test fun listOpensComposesSwitchesAccountAndRetries() {
        val events = mutableListOf<String>()
        var error by mutableStateOf<String?>(null)
        compose.setContent {
            NgaTheme {
                MessageListScreen(listOf("甲", "乙"), 0, listOf(thread(1), thread(2)), false, error, false,
                    { events += "switch:$it" }, { events += "refresh" }, { events += "retry" }, { events += "more" },
                    { events += "open:${it.mid}" }, { events += "compose" }, {}, {})
            }
        }
        compose.onNodeWithText("会话2").performClick()
        compose.onNodeWithText("写短消息", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("切换账号").performClick()
        compose.onNodeWithText("乙").performClick()
        error = "网络错误"
        compose.onNodeWithText("重试").performClick()
        compose.runOnIdle { assertEquals(listOf("open:2", "compose", "switch:1", "retry"), events) }
    }

    @Test fun listWithoutAccountAsksToLogin() {
        var login = false
        compose.setContent {
            NgaTheme { MessageListScreen(emptyList(), 0, emptyList(), false, null, false, {}, {}, {}, {}, {}, {}, { login = true }, {}) }
        }
        compose.onNodeWithText("写短消息", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("登录 NGA 账号").performClick()
        compose.runOnIdle { assertEquals(true, login) }
    }

    @Test fun detailShowsEntriesMarksLastPageAndReplies() {
        var replied = false
        val muted = entry(1, "乙").apply { mute_time = "100" }
        compose.setContent {
            NgaTheme {
                MessageDetailScreen("会话标题", listOf(entry(0, "甲"), muted), false, null, false, {}, {}, { replied = true }, {})
            }
        }
        compose.onNodeWithText("会话标题").assertIsDisplayed()
        compose.onNodeWithText("乙(传说)").assertIsDisplayed()
        compose.onAllNodesWithTag("article_html").assertCountEquals(2)
        compose.onNodeWithText("已加载到短消息内容的最后一页").assertExists()
        compose.onNodeWithContentDescription("回复").performClick()
        compose.runOnIdle { assertEquals(true, replied) }
    }

    @Test fun postRequiresRecipientAndBodyBeforeSending() {
        val sent = mutableListOf<Triple<String, String, String>>()
        compose.setContent {
            NgaTheme { MessagePostScreen(true, null, false, { t, r, b -> sent += Triple(t, r, b) }, {}) }
        }
        compose.onNodeWithContentDescription("发送").performClick()
        compose.onNodeWithText("请输入收件人").assertIsDisplayed()
        compose.onNodeWithText("请输入内容").assertIsDisplayed()
        compose.onNodeWithText("收件人").performTextInput("甲")
        compose.onNodeWithText("标题").performTextInput("你好")
        compose.onNodeWithText("内容").performTextInput("正文")
        compose.onNodeWithContentDescription("发送").performClick()
        compose.runOnIdle { assertEquals(listOf(Triple("你好", "甲", "正文")), sent) }
    }

    @Test fun replyHidesRecipientAndSendingDisablesSubmit() {
        compose.setContent { NgaTheme { MessagePostScreen(false, "甲", true, { _, _, _ -> }, {}) } }
        compose.onNodeWithText("回短消息").assertIsDisplayed()
        compose.onNodeWithText("收件人").assertDoesNotExist()
        compose.onNodeWithContentDescription("发送").assertDoesNotExist()
    }
}
