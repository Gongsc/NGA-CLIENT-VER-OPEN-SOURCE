package gov.anzong.androidnga.ui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** 修改签名与头像页面；只触发本地回调，不提交到论坛。 */
class AccountEditScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun signatureRejectsEmptyAndSubmitsEditedText() {
        val submitted = mutableListOf<String>()
        compose.setContent { NgaTheme { SignatureEditScreen("旧签名", false, { submitted += it }, {}) } }
        compose.onNodeWithText("旧签名").performTextClearance()
        compose.onNodeWithContentDescription("提交").performClick()
        compose.onNodeWithText("请输入内容").assertIsDisplayed()
        compose.onNodeWithText("签名").performTextInput("新签名")
        compose.onNodeWithContentDescription("提交").performClick()
        compose.runOnIdle { assertEquals(listOf("新签名"), submitted) }
    }

    @Test fun signatureSendingHidesSubmit() {
        compose.setContent { NgaTheme { SignatureEditScreen("旧签名", true, {}, {}) } }
        compose.onNodeWithContentDescription("提交").assertDoesNotExist()
        compose.onNodeWithText("旧签名").assertIsNotEnabled()
    }

    @Test fun avatarValidatesAddressPreviewsAndSubmits() {
        val events = mutableListOf<String>()
        var url by mutableStateOf("")
        compose.setContent {
            NgaTheme { AvatarEditScreen(url, { url = it }, false, { events += "submit:$url" }, {}) }
        }
        compose.onNodeWithText("提交").performClick()
        compose.onNodeWithText("请输入正确的头像 URL 地址").assertIsDisplayed()
        compose.onNodeWithContentDescription("头像预览").assertDoesNotExist()
        compose.onNodeWithText("从相册选择并上传").assertDoesNotExist()
        compose.onNodeWithText("设置头像", substring = true).assertIsDisplayed()
        compose.onNodeWithText("头像地址").performTextInput("https://img.nga.cn/avatar.png")
        compose.onNodeWithContentDescription("头像预览").assertExists()
        compose.onNodeWithText("提交").performClick()
        compose.runOnIdle { assertEquals(listOf("submit:https://img.nga.cn/avatar.png"), events) }
    }
}
