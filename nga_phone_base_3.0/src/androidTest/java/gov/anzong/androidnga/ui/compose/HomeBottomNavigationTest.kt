package gov.anzong.androidnga.ui.compose

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.mvvm.viewmodel.HomeInboxViewModel
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo

class HomeBottomNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fourDestinationsReplaceDrawerAndMessageTabsGateLogin() {
        val inbox = HomeInboxViewModel()
        var login = 0
        compose.setContent { NgaTheme {
            HomeScreen(HomeState(), emptyList(), {}, {}, {}, {}, {}, onRemove = {},
                historyContent = { Text("本地浏览记录") },
                messagesContent = { controls -> HomeMessagesScreen(null, inbox, 0, { login++ }, {}, {}, {}, {}, controls) })
        } }
        compose.onNodeWithContentDescription("打开导航").assertDoesNotExist()
        for (index in 0..3) compose.onNodeWithTag("home_tab_$index").assertIsDisplayed()
        compose.onNodeWithTag("home_tab_0").assertIsSelected()
        compose.onNodeWithTag("home_tab_1").performClick().assertIsSelected()
        compose.onNodeWithTag("home_tab_0").assertIsNotSelected()
        compose.onAllNodes(isSelectable() and isSelected()).filter(hasAnyAncestor(hasTestTag("home_bottom_navigation"))).assertCountEquals(1)
        compose.onNodeWithText("本地浏览记录").assertIsDisplayed()
        compose.onNodeWithTag("home_tab_2").performClick()
        compose.onNodeWithText("回复通知").assertIsDisplayed()
        compose.onNodeWithText("短消息").performClick().assertIsSelected()
        compose.onNodeWithText("登录 NGA 账号").performClick()
        compose.runOnIdle { assertEquals(1, login) }
        compose.onNodeWithTag("home_tab_3").performClick()
        compose.onNodeWithText("未登录").assertIsDisplayed()
    }

    @Test fun historyScrollHidesNavigationAndReverseScrollRestoresIt() {
        val topics = (1..40).map { i -> ThreadPageInfo().apply { tid = i; subject = "离线测试主题 $i"; author = "测试用户"; page = 2 } }
        compose.setContent { NgaTheme {
            HomeScreen(HomeState(), emptyList(), {}, {}, {}, {}, {}, onRemove = {},
                historyContent = { controls -> HomeHistoryScreen(topics, {}, {}, {}, controls) })
        } }
        compose.onNodeWithTag("home_tab_1").performClick()
        // 底栏浮在列表上方，默认 swipeUp() 从列表底边起手会落在底栏上而非列表；从列表可见区域内起手。
        compose.onNodeWithTag("home_history").performTouchInput { swipeUp(startY = centerY, endY = top) }
        compose.waitForIdle()
        compose.onNodeWithTag("home_bottom_navigation").assertDoesNotExist()
        compose.onNodeWithTag("home_history").performTouchInput { swipeDown() }
        compose.waitForIdle()
        compose.onNodeWithTag("home_bottom_navigation").assertIsDisplayed()
    }

    @Test fun scrollControlsIgnoreFlingAndSmallDirectionChanges() {
        lateinit var connection: NestedScrollConnection
        val changes = mutableListOf<Boolean>()
        compose.setContent { connection = rememberScrollControlsConnection({ true }) { changes += it } }
        compose.runOnIdle {
            connection.onPostScroll(Offset(0f, -1000f), Offset.Zero, NestedScrollSource.SideEffect)
            assertTrue(changes.isEmpty())
            connection.onPostScroll(Offset(0f, -1f), Offset.Zero, NestedScrollSource.UserInput)
            assertTrue(changes.isEmpty())
            connection.onPostScroll(Offset(0f, -1000f), Offset.Zero, NestedScrollSource.UserInput)
            assertEquals(listOf(false), changes)
            connection.onPostScroll(Offset(0f, 1000f), Offset.Zero, NestedScrollSource.UserInput)
            assertEquals(listOf(false, true), changes)
        }
    }
}
