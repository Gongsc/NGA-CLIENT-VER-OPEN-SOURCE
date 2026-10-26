package gov.anzong.androidnga.ui.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import gov.anzong.androidnga.activity.LoginActivity
import gov.anzong.androidnga.activity.PostActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class ActivityHostTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun replyEditorPreservesDraftAcrossActivityRecreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, PostActivity::class.java).putExtra("action", "reply")
            .putExtra("tid", "2147483000").putExtra("prefix", "引用前缀\n")
        ActivityScenario.launch<PostActivity>(intent).use { scenario ->
            compose.onNodeWithText("回复主题").assertIsDisplayed()
            compose.onNodeWithText("正文").performTextInput("本地追加内容")
            scenario.recreate()
            compose.onNodeWithText("引用前缀\n本地追加内容").assertExists()
            val screenshots = File(context.filesDir, "ui-verification").apply { mkdirs() }
            File(screenshots, "reply-editor.png").outputStream().use { output ->
                compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
    }

    @Test fun closingLoginDoesNotReportAuthenticationSuccess() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, LoginActivity::class.java)
        ActivityScenario.launchActivityForResult<LoginActivity>(intent).use { scenario ->
            compose.onNodeWithText("账号登录").assertIsDisplayed()
            compose.onNodeWithText("完成登录").assertIsDisplayed()
            scenario.onActivity { it.finish() }
            assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
        }
    }
}
