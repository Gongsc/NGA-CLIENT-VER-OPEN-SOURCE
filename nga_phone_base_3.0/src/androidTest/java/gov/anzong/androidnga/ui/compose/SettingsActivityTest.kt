package gov.anzong.androidnga.ui.compose

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import gov.anzong.androidnga.activity.AppearanceActivity
import org.junit.Rule
import org.junit.Test

class SettingsActivityTest {
    @get:Rule val compose=createEmptyComposeRule()
    @Test fun realAppearanceHostOpensThemedReadingPageAndReturns() {
        ActivityScenario.launch(AppearanceActivity::class.java).use {
            compose.onNodeWithTag("appearance_screen").assertIsDisplayed()
            compose.onNodeWithText("字体与阅读大小").performScrollTo().performClick()
            compose.onNodeWithTag("reading_preview").assertIsDisplayed()
            compose.onNodeWithContentDescription("返回").performClick()
            compose.onNodeWithTag("appearance_screen").assertIsDisplayed()
        }
    }
}
