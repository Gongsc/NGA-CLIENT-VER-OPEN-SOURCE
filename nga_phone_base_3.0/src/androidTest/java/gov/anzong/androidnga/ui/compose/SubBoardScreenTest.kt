package gov.anzong.androidnga.ui.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import gov.anzong.androidnga.mvvm.viewmodel.SubBoardViewModel.Row
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.mvvm.model.entity.SubBoard

/** 子版块页；只使用本地合成数据，开关只触发回调，不请求论坛。 */
class SubBoardScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun row(fid: Int, visible: Boolean, type: Int = 0, pending: Boolean = false, description: String? = null) =
        Row(SubBoard().apply { this.fid = fid; name = "版块$fid"; this.type = type; this.description = description }, visible, pending)

    @Test fun opensTogglesAndHidesSwitchWhereNotAllowed() {
        val events = mutableListOf<String>()
        val rows = listOf(row(1, true, description = "说明一"), row(2, false), row(3, true, type = -1), row(4, true, pending = true))
        compose.setContent {
            NgaTheme { SubBoardScreen("父版块 - 子版块", rows, { events += "open:${it.board.fid}" }, { events += "toggle:$it" }, {}) }
        }
        compose.onNodeWithText("父版块 - 子版块").assertIsDisplayed()
        compose.onNodeWithText("说明一").assertIsDisplayed()
        compose.onNodeWithContentDescription("在父版块显示版块1").assertIsOn()
        compose.onNodeWithContentDescription("在父版块显示版块2").assertIsOff().performClick()
        compose.onNodeWithContentDescription("在父版块显示版块3").assertDoesNotExist()
        compose.onNodeWithContentDescription("在父版块显示版块4").assertDoesNotExist()
        compose.onNodeWithText("版块3").performClick()
        compose.runOnIdle { assertEquals(listOf("toggle:1", "open:3"), events) }
    }

    @Test fun emptyListShowsHint() {
        compose.setContent { NgaTheme { SubBoardScreen("父版块 - 子版块", emptyList(), {}, {}, {}) } }
        compose.onNodeWithText("这个版块没有子版块").assertIsDisplayed()
    }
}
