package gov.anzong.androidnga.ui.compose

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.network.bean.ThreadRowInfo

/** Exercises UI callbacks only; no authenticated or server mutation requests are made. */
class MigratedScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun editorRequiresContentAndExplicitSubmissionConfirmation() {
        val state = PostEditorState().apply { preparing = false }
        var submissions = 0
        compose.setContent { NgaTheme { PostEditorScreen(state, "new", {}, { submissions++ }, {}, {}, {}) } }
        compose.onNodeWithText("提交").assertIsNotEnabled()
        compose.onNodeWithText("标题").performTextInput("界面测试标题")
        compose.onNodeWithText("正文").performTextInput("此内容仅用于本地界面测试。")
        compose.onNodeWithText("提交").performClick()
        compose.onNodeWithText("确认提交").assertIsDisplayed()
        assertEquals(0, submissions)
        compose.onAllNodesWithText("提交").onLast().performClick()
        compose.runOnIdle { assertEquals(1, submissions) }
    }

    @Test fun editorProtectsUnsubmittedChangesOnExit() {
        val state = PostEditorState().apply { preparing = false }
        var exits = 0
        compose.setContent { NgaTheme { PostEditorScreen(state, "reply", { exits++ }, {}, {}, {}, {}) } }
        compose.onNodeWithText("正文").performTextInput("尚未提交的本地内容")
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("放弃编辑？").assertIsDisplayed()
        compose.onNodeWithText("继续编辑").performClick()
        compose.runOnIdle { assertEquals(0, exits) }
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("放弃编辑", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test fun readerChangesPageWithoutMakingNetworkRequests() {
        val state = ArticleReaderState().apply { pages = 3 }
        compose.setContent {
            NgaTheme { ArticleReaderScreen(state, 1, false, {}, {}, {}, {}, {}) { page, _ -> Text("本地正文页 ${page + 1}") } }
        }
        compose.onNodeWithContentDescription("下一页").performClick()
        compose.onNodeWithText("本地正文页 2").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, state.currentPage) }
    }

    @Test fun emoticonPickerInsertsSiteCodes() {
        val selected = mutableListOf<String>()
        compose.setContent { NgaTheme { EmoticonPicker { selected += it } } }
        // 表情网格只有 320dp 高，需先滚动到目标表情
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("茶"))
        compose.onNodeWithContentDescription("茶").performClick()
        // 切换分类用语义点击，避免刚滚动完的网格吞掉触摸
        compose.onNodeWithText("企鹅").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        compose.onNodeWithText("企鹅").assertIsSelected()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription("吃瓜"))
        compose.onNodeWithContentDescription("吃瓜").performClick()
        compose.runOnIdle { assertEquals(listOf("[s:ac:茶]", "[s:pg:吃瓜]"), selected) }
        compose.onNodeWithText("熊猫").assertDoesNotExist()
    }

    @Test fun articleRowExposesQuoteAction() {
        val row = ThreadRowInfo().apply { author = "测试作者"; content = "离线正文内容"; tid = 100; pid = 200; lou = 1; score = 42 }
        val state = ArticlePageState().apply { rows = listOf(row); loading = false }
        var quoted = 0
        compose.setContent { NgaTheme { ArticlePageScreen(state, {}, { quoted++ }, {}, {}, { emptyList() }, { _, _ -> }) } }
        compose.onNodeWithText("离线正文内容").assertIsDisplayed()
        // 赞同数以图标加数字显示，无障碍读作“赞同 42”
        compose.onNode(hasContentDescription("赞同") and hasText("42")).assertIsDisplayed()
        compose.onNodeWithText("赞同", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("引用回复").performClick()
        compose.runOnIdle { assertEquals(1, quoted) }
    }
}
