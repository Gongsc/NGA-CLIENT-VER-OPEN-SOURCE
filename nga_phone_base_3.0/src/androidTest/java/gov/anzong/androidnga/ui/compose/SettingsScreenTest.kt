package gov.anzong.androidnga.ui.compose

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import gov.anzong.androidnga.common.PreferenceKey
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule val compose=createComposeRule()
    private val prefs=ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("settings_screen_test",Context.MODE_PRIVATE)

    @Test fun settingsUsesCurrentPaletteWithoutDuplicateAppearanceEntry() {
        val scheme=mutableStateOf(ngaColorScheme(AppPalette.BLUE,false))
        compose.setContent {MaterialTheme(colorScheme=scheme.value){SettingsScreen(prefs,{},{},{})}}
        for(palette in AppPalette.entries) for(dark in listOf(false,true)) {
            val expected=ngaColorScheme(palette,dark)
            compose.runOnIdle{scheme.value=expected}
            val image=compose.onNodeWithTag("settings_screen").captureToImage().toPixelMap()
            assertEquals(expected.background,image[2,2])
        }
        compose.onNodeWithText("外观与布局").assertDoesNotExist()
        compose.onNodeWithText("关于").assertDoesNotExist()
        compose.onNodeWithText("字体与阅读大小").assertDoesNotExist()
    }

    @Test fun togglesPersistAndNotificationSoundDependsOnReminder() {
        prefs.edit().clear().commit()
        compose.setContent {NgaTheme{SettingsScreen(prefs,{},{},{})}}
        compose.onNodeWithText("左手模式").performClick()
        compose.runOnIdle {assertTrue(prefs.getBoolean(PreferenceKey.LEFT_HAND,false))}
        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("回复提醒"))
        compose.onNodeWithText("回复提醒").performClick()
        compose.onNode(hasText("提醒声音") and isToggleable()).assertIsNotEnabled()
        compose.runOnIdle {assertFalse(prefs.getBoolean(PreferenceKey.ENABLE_NOTIFIACTION,true))}
        compose.onNodeWithText("回复提醒").performClick()
        compose.onNode(hasText("提醒声音") and isToggleable()).assertIsEnabled()
    }

    @Test fun selectionsPersistAndCacheClearingNeedsConfirmation() {
        prefs.edit().clear().commit()
        var clear=0
        var destination=""
        compose.setContent {NgaTheme {SettingsScreen(prefs,{}, {destination=it},{clear++})}}
        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("帖子图片"))
        compose.onNodeWithText("帖子图片").performClick()
        compose.onNodeWithText("始终关闭").performClick()
        compose.runOnIdle {assertEquals("1",prefs.getString(PreferenceKey.KEY_LOAD_IMAGE_STRATEGY,null))}
        compose.onNodeWithTag("settings_list").performScrollToNode(hasText("清除缓存"))
        compose.onNodeWithText("清除缓存").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle {assertEquals(0,clear)}
        compose.onNodeWithText("清除缓存").performClick()
        compose.onNodeWithText("清除",useUnmergedTree=true).performClick()
        compose.runOnIdle {assertEquals(1,clear)}
    }

    @Test fun profileGroupsActionsAndAppEntriesAppearOnlyOnce() {
        val actions=listOf(gov.anzong.androidnga.R.id.menu_post to "我的主题",gov.anzong.androidnga.R.id.menu_msg to "短消息",gov.anzong.androidnga.R.id.menu_appearance to "外观与布局",gov.anzong.androidnga.R.id.menu_setting to "设置",gov.anzong.androidnga.R.id.menu_about to "关于").map{HomeAction(it.first,it.second)}
        val state=HomeState().apply{account=HomeAccount("42","测试账号")}
        compose.setContent {NgaTheme{HomeScreen(state,actions,{},{},{},{},{},onRemove={})}}
        compose.onNodeWithTag("home_tab_3").performClick()
        compose.onNodeWithText("测试账号").assertIsDisplayed()
        compose.onNodeWithText("UID 42").assertIsDisplayed()
        compose.onNodeWithText("账号").assertIsDisplayed()
        compose.onNodeWithText("消息").assertIsDisplayed()
        compose.onNodeWithTag("home_navigation").performScrollToNode(hasText("关于"))
        compose.onNodeWithText("应用").assertIsDisplayed()
        compose.onAllNodesWithText("外观与布局").assertCountEquals(1)
        compose.onAllNodesWithText("设置").assertCountEquals(1)
        compose.onAllNodesWithText("关于").assertCountEquals(1)
    }

    @Test fun loggedOutProfileFoldsAccountEntriesIntoLogin() {
        val actions=listOf(gov.anzong.androidnga.R.id.menu_post to "我的主题",gov.anzong.androidnga.R.id.menu_msg to "短消息",gov.anzong.androidnga.R.id.menu_download to "我的缓存").map{HomeAction(it.first,it.second)}
        val clicked=mutableListOf<Int>()
        compose.setContent {NgaTheme{HomeScreen(HomeState(),actions,{clicked+=it},{},{},{},{},onRemove={})}}
        compose.onNodeWithTag("home_tab_3").performClick()
        compose.onNodeWithText("未登录").assertIsDisplayed()
        compose.onNodeWithText("我的缓存").assertIsDisplayed()
        compose.onAllNodesWithText("我的主题").assertCountEquals(0)
        compose.onAllNodesWithText("短消息").assertCountEquals(0)
        compose.onNodeWithText("登录 NGA 账号").performClick()
        compose.runOnIdle {assertEquals(listOf(gov.anzong.androidnga.R.id.menu_login),clicked)}
    }
}
