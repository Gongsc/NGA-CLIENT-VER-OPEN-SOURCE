package gov.anzong.androidnga.gallery

import android.content.Intent
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test

/** 启动真实页面，用应用内置资源离线验证 Glide 加载（JPG、GIF）与加载失败提示。 */
class ImageZoomActivityTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val jpg = "file:///android_asset/xiongmao/-1324875_50d8593397258.jpg"
    private val gif = "file:///android_asset/xiongmao/-47218_5053040c1ff51.gif"
    private val missing = "file:///android_asset/xiongmao/missing.png"

    @Test fun loadsBundledImagesAndReportsFailures() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ImageZoomActivity::class.java)
            .putExtra(ImageZoomActivity.KEY_GALLERY_URLS, arrayOf(jpg, gif, missing))
            .putExtra(ImageZoomActivity.KEY_GALLERY_CUR_URL, gif)
        ActivityScenario.launch<ImageZoomActivity>(intent).use {
            compose.onNodeWithText("2 / 3").assertIsDisplayed()
            val loading = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)
            compose.waitUntil(10_000) {
                compose.onAllNodes(loading).fetchSemanticsNodes().isEmpty() &&
                    compose.onAllNodesWithText("图片加载失败").fetchSemanticsNodes().size == 1
            }
            compose.onNodeWithTag("gallery_page_1").performTouchInput { doubleClick() }
            compose.onNodeWithTag("gallery_pager").performTouchInput { swipeLeft() }
            compose.onNodeWithText("2 / 3").assertIsDisplayed()
            compose.onNodeWithTag("gallery_page_1").performTouchInput { doubleClick() }
            compose.waitForIdle()
            compose.onNodeWithTag("gallery_pager").performTouchInput { swipeLeft() }
            compose.onNodeWithText("3 / 3").assertIsDisplayed()
            compose.onNodeWithText("图片加载失败").assertIsDisplayed()
            compose.onNodeWithText("重试").assertIsDisplayed()
        }
    }
}
