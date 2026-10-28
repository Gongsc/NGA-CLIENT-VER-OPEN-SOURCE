package gov.anzong.androidnga.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import gov.anzong.androidnga.mvvm.viewmodel.SettingsListsViewModel.Account
import gov.anzong.androidnga.mvvm.viewmodel.SettingsListsViewModel.BlockedUser
import gov.anzong.androidnga.mvvm.viewmodel.SettingsListsViewModel.Keyword
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 设置中的关键词、黑名单与账号页面；只使用本地合成数据与回调，不修改真实账号。 */
class SettingsListScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun keywordsAddRejectDuplicatesToggleAndConfirmRemoval() {
        var keywords by mutableStateOf(listOf(Keyword("广告", true)))
        val removed = mutableListOf<Int>()
        var toggled = -1
        compose.setContent {
            NgaTheme {
                KeywordFilterScreen(keywords, { text ->
                    val t = text.trim()
                    if (keywords.any { it.text == t }) false else { keywords = keywords + Keyword(t, true); true }
                }, { toggled = it }, { removed += it }, {})
            }
        }
        compose.onNodeWithText("添加").assertIsNotEnabled()
        compose.onNodeWithText("新关键词").performTextInput("广告")
        compose.onNodeWithText("添加").performClick()
        compose.onNodeWithText("这个关键词已经添加过了").assertIsDisplayed()
        compose.onNodeWithText("新关键词").performTextClearance()
        compose.onNodeWithText("新关键词").performTextInput("剧透")
        compose.onNodeWithText("添加").performClick()
        compose.onNodeWithText("剧透").assertIsDisplayed()
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.runOnIdle { assertEquals(0, toggled) }
        compose.onNodeWithContentDescription("删除关键词剧透").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(emptyList<Int>(), removed) }
        compose.onNodeWithContentDescription("删除关键词剧透").performClick()
        compose.onNodeWithText("删除").performClick()
        compose.runOnIdle { assertEquals(listOf(1), removed) }
    }

    @Test fun blacklistUnblocksOpensProfileAndConfirmsClear() {
        val users = listOf(BlockedUser("1", "甲"), BlockedUser("2", "乙"))
        val events = mutableListOf<String>()
        compose.setContent {
            NgaTheme { BlacklistScreen(users, { events += "open:${it.uid}" }, { events += "unblock:${it.uid}" }, { events += "clear" }, {}) }
        }
        compose.onAllNodesWithText("移除").onLast().performClick()
        compose.onNodeWithText("甲").performClick()
        compose.onNodeWithText("清空").performClick()
        compose.onNodeWithText("是否移除全部 2 个用户？").assertIsDisplayed()
        compose.onAllNodesWithText("清空").onLast().performClick()
        compose.runOnIdle { assertEquals(listOf("unblock:2", "open:1", "clear"), events) }
    }

    @Test fun emptyBlacklistDisablesClear() {
        compose.setContent { NgaTheme { BlacklistScreen(emptyList(), {}, {}, {}, {}) } }
        compose.onNodeWithText("清空").assertIsNotEnabled()
        compose.onNodeWithText("黑名单为空", substring = true).assertIsDisplayed()
    }

    @Test fun accountsActivateMoveAndConfirmRemoval() {
        val accounts = listOf(Account("1", "甲", null, true), Account("2", "乙", null, false))
        val events = mutableListOf<String>()
        compose.setContent {
            NgaTheme {
                AccountsScreen(accounts, { null }, { events += "activate:$it" }, { from, to -> events += "move:$from>$to" },
                    { events += "open:${it.uid}" }, { events += "remove:$it" }, { events += "add" }, {})
            }
        }
        compose.onNodeWithText("UID 1 · 当前账号").assertIsDisplayed()
        compose.onNodeWithText("乙").performClick()
        compose.onNodeWithContentDescription("乙的操作").performClick()
        compose.onNodeWithText("下移").assertDoesNotExist()
        compose.onNodeWithText("上移").performClick()
        compose.onNodeWithContentDescription("甲的操作").performClick()
        compose.onNodeWithText("移除账号").performClick()
        compose.onNodeWithText("移除").performClick()
        compose.onNodeWithContentDescription("添加账号").performClick()
        compose.runOnIdle { assertEquals(listOf("activate:1", "move:1>0", "remove:0", "add"), events) }
    }
}
