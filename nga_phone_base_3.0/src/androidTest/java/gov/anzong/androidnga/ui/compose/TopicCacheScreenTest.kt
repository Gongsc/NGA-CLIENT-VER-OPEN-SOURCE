package gov.anzong.androidnga.ui.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import sp.phone.mvp.model.entity.ThreadPageInfo

/** 我的缓存列表；只使用合成主题，删除、导入、导出只触发回调。 */
class TopicCacheScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun topic(tid: Int) = ThreadPageInfo().apply { this.tid = tid; subject = "缓存$tid"; author = "作者"; replies = tid }

    @Test fun opensConfirmsDeleteAndUsesMenu() {
        val events = mutableListOf<String>()
        compose.setContent {
            NgaTheme {
                TopicCacheScreen(listOf(topic(1), topic(2)), false, { events += "open:${it.tid}" }, { events += "delete:${it.tid}" },
                    { events += "import" }, { events += "export" }, {})
            }
        }
        compose.onNodeWithText("缓存1").performClick()
        compose.onNodeWithContentDescription("删除缓存缓存2").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("缓存2").performTouchInput { longClick() }
        compose.onNodeWithText("删除").performClick()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("导出缓存").performClick()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("导入缓存").performClick()
        compose.runOnIdle { assertEquals(listOf("open:1", "delete:2", "export", "import"), events) }
    }

    @Test fun emptyCacheExplainsHowToCacheAndDisablesExport() {
        compose.setContent { NgaTheme { TopicCacheScreen(emptyList(), false, {}, {}, {}, {}, {}) } }
        compose.onNodeWithText("暂无缓存的帖子", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("导出缓存").assertIsNotEnabled()
    }
}
