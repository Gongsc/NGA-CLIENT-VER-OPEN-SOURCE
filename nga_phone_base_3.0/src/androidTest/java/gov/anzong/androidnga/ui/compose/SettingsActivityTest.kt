package gov.anzong.androidnga.ui.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import gov.anzong.androidnga.activity.SettingsActivity
import org.junit.Rule
import org.junit.Test

class SettingsActivityTest {
    @get:Rule val compose=createEmptyComposeRule()
    @Test fun realSettingsHostOpensThemedReadingPageAndReturns() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            compose.onNodeWithTag("settings_screen").assertIsDisplayed()
            compose.onNodeWithText("字体与阅读大小").performClick()
            compose.onNodeWithText("正文预览：普通回复与富文本使用相同的基础字号。").assertIsDisplayed()
            compose.onNodeWithContentDescription("返回").performClick()
            compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        }
    }
}
