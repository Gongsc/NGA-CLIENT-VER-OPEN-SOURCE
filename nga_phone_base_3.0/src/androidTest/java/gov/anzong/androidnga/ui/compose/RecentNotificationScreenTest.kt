package gov.anzong.androidnga.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.network.ApiConstants
import gov.anzong.androidnga.mvvm.model.entity.RecentReplyInfo

/** 回复通知页；只使用本地合成数据，清空只触发回调，不请求论坛。 */
class RecentNotificationScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun reply(pid: String, type: Int) = RecentReplyInfo(type, "用户$pid", "主题$pid", pid, "100")

    @Test fun showsTypesMarksUnreadOpensAndConfirmsClear() {
        val replies = listOf(reply("1", ApiConstants.NGA_NOTIFICATION_TYPE_REPLY_AT), reply("2", ApiConstants.NGA_NOTIFICATION_TYPE_TOPIC_REPLY))
        val events = mutableListOf<String>()
        compose.setContent {
            NgaTheme {
                RecentNotificationScreen(true, replies, false, null, { it.pidStr == "1" }, { events += "refresh" },
                    { events += "open:${it.pidStr}" }, { events += "clear" }, {}, {})
            }
        }
        compose.onNodeWithText("在回复中@了你").assertIsDisplayed()
        compose.onNodeWithText("回复了你的主题").assertIsDisplayed()
        compose.onAllNodesWithText("未读").assertCountEquals(1)
        compose.onNodeWithText("主题2").performClick()
        compose.onNodeWithText("清空").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("清空").performClick()
        compose.onAllNodesWithText("清空").onLast().performClick()
        compose.runOnIdle { assertEquals(listOf("open:2", "clear"), events) }
    }

    @Test fun emptyAndErrorStates() {
        var error by androidx.compose.runtime.mutableStateOf<String?>(null)
        var retried = 0
        compose.setContent {
            NgaTheme { RecentNotificationScreen(true, emptyList(), false, error, { false }, { retried++ }, {}, {}, {}, {}) }
        }
        compose.onNodeWithText("暂无回复通知").assertIsDisplayed()
        compose.onNodeWithText("清空").assertIsNotEnabled()
        error = "网络错误"
        compose.onNodeWithText("网络错误").assertIsDisplayed()
        compose.onNodeWithText("重试").performClick()
        compose.runOnIdle { assertEquals(1, retried) }
    }

    @Test fun loggedOutShowsLogin() {
        var login = 0
        compose.setContent { NgaTheme { RecentNotificationScreen(false, emptyList(), false, null, { false }, {}, {}, {}, { login++ }, {}) } }
        compose.onNodeWithText("登录 NGA 账号").performClick()
        compose.runOnIdle { assertEquals(1, login) }
    }
}
