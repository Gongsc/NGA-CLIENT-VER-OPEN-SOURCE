package gov.anzong.androidnga.ui.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 字体与阅读大小：预览、分组调整与恢复默认；只通过回调保存，不写入设置。 */
class ReadingSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun buttonsAdjustPreviewAndSaveAndResetRestoresDefaults() {
        val saved = mutableListOf<ReadingSizes>()
        compose.setContent {
            NgaTheme { ReadingSettingsContent(ReadingSizes(70, 18, 104, 150), 24, { saved += it }, {}) }
        }
        compose.onNodeWithTag("reading_preview").assertIsDisplayed()
        compose.onNodeWithContentDescription("头像预览").assertExists()
        listOf("主题列表", "帖子正文", "楼层").forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("18sp").assertExists()
        compose.onNodeWithContentDescription("增大标题字号").performClick()
        compose.onNodeWithText("19sp").assertExists()
        compose.onNodeWithContentDescription("减小正文大小").performClick()
        compose.onNodeWithText("65%").assertExists()
        compose.onNodeWithText("32dp").performScrollTo().assertExists()
        compose.runOnIdle { assertEquals(listOf(ReadingSizes(70, 19, 104, 150), ReadingSizes(65, 19, 104, 150)), saved) }
        compose.onNodeWithContentDescription("恢复默认大小").performClick()
        compose.onNodeWithText("恢复").performClick()
        compose.runOnIdle { assertEquals(ReadingSizes.Default, saved.last()) }
        compose.onNodeWithText("${ReadingSizes.Default.title}sp").assertExists()
    }

    @Test fun outOfRangeValuesAreClampedAndLimitsDisableButtons() {
        compose.setContent { NgaTheme { ReadingSettingsContent(ReadingSizes(1, 30, 1, 999), 24, {}, {}) } }
        compose.onNodeWithText("40%").assertExists()
        compose.onNodeWithText("25sp").assertExists()
        compose.onNodeWithText("200px").assertExists()
        compose.onNodeWithContentDescription("减小正文大小").assertIsNotEnabled()
        compose.onNodeWithContentDescription("增大标题字号").assertIsNotEnabled()
    }
}
