package gov.anzong.androidnga.ui.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import sp.phone.http.bean.AdminForumsData
import sp.phone.http.bean.ProfileData
import sp.phone.http.bean.ReputationData

/** 用户资料页；只使用本地合成资料，菜单与链接只触发回调。 */
class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    private val profile = ProfileData().apply {
        uid = "42"; userName = "测试用户"; memberGroup = "学徒"; postCount = "10"; money = "123456"; frame = "15"
        reputationEntryList = listOf(ReputationData("某版块", "8"))
        adminForums = listOf(AdminForumsData("7", "测试版块"))
    }

    @Test fun showsDetailsMenuAndForumLinksForOtherUsers() {
        val events = mutableListOf<String>()
        compose.setContent {
            NgaTheme {
                ProfileScreen(profile, null, false, null, currentUser = false, blocked = true, onAction = { events += it.name },
                    onEditSignature = { events += "sign" }, onOpenForum = { events += "forum:${it.fid}" }, onRefresh = {}, onBack = {})
            }
        }
        compose.onNodeWithText("用户ID：42").assertIsDisplayed()
        compose.onNodeWithText("已激活").assertIsDisplayed()
        compose.onNodeWithText("12 金 34 银 56 铜").assertIsDisplayed()
        compose.onNodeWithText("1.5").assertExists()
        compose.onNodeWithText("修改签名").assertDoesNotExist()
        compose.onNodeWithText("测试版块").performScrollTo().performClick()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("修改头像").assertDoesNotExist()
        compose.onNodeWithText("取消屏蔽此人").performClick()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("发送短消息").performClick()
        compose.runOnIdle { assertEquals(listOf("forum:7", "ToggleBlock", "SendMessage"), events) }
    }

    @Test fun currentUserCanEditSignatureAndAvatar() {
        val events = mutableListOf<String>()
        compose.setContent {
            NgaTheme {
                ProfileScreen(profile, "<html><body>签名内容</body></html>", false, null, currentUser = true, blocked = false,
                    onAction = { events += it.name }, onEditSignature = { events += "sign" }, onOpenForum = {}, onRefresh = {}, onBack = {})
            }
        }
        compose.onNodeWithTag("article_html").assertExists()
        compose.onNodeWithText("修改签名").performScrollTo().performClick()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("发送短消息").assertDoesNotExist()
        compose.onNodeWithText("修改头像").performClick()
        compose.runOnIdle { assertEquals(listOf("sign", "ModifyAvatar"), events) }
    }

    @Test fun errorBeforeLoadOffersRetryAndOnlyLinkActions() {
        var retried = false
        compose.setContent {
            NgaTheme { ProfileScreen(null, null, false, "网络错误", false, false, {}, {}, {}, { retried = true }, {}) }
        }
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("复制链接").assertIsDisplayed()
        compose.onNodeWithText("搜索发帖").assertDoesNotExist()
        compose.runOnIdle { assertEquals(true, retried) }
    }
}
