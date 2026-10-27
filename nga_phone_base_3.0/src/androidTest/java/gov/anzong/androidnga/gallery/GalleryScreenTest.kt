package gov.anzong.androidnga.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class GalleryScreenTest {
    @get:Rule val compose = createComposeRule()

    private val urls = listOf("https://img.nga.cn/a.jpg", "https://img.nga.cn/b.png", "https://img.nga.cn/c.gif")
    private val states = mutableMapOf<String, GalleryZoomState>()
    private val downloaded = mutableListOf<String>()
    private val shared = mutableListOf<String>()
    private var downloadAll = 0

    private fun show(initialPage: Int = 0) = compose.setContent {
        MaterialTheme {
            GalleryScreen(urls, initialPage, onBack = {}, onDownload = { downloaded += it }, onShare = { shared += it },
                onDownloadAll = { downloadAll++ }) { url, state ->
                states[url] = state
                Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) { Text(url) }
            }
        }
    }

    @Test fun startsAtRequestedImageAndSwipesBetweenPages() {
        show(initialPage = 1)
        compose.onNodeWithText("2 / 3").assertIsDisplayed()
        compose.onNodeWithTag("gallery_pager").performTouchInput { swipeLeft() }
        compose.onNodeWithText("3 / 3").assertIsDisplayed()
        compose.onNodeWithContentDescription("下载保存").performClick()
        compose.onNodeWithContentDescription("分享").performClick()
        assertEquals(listOf(urls[2]), downloaded)
        assertEquals(listOf(urls[2]), shared)
    }

    @Test fun doubleTapZoomsLocksPagingAndRestores() {
        show()
        compose.onNodeWithTag("gallery_page_0").performTouchInput { doubleClick() }
        compose.waitForIdle()
        assertEquals(GalleryZoomState.DOUBLE_TAP_SCALE, states.getValue(urls[0]).scale, 0.01f)
        compose.onNodeWithTag("gallery_pager").performTouchInput { swipeLeft() }
        compose.onNodeWithText("1 / 3").assertIsDisplayed()
        assertTrue(states.getValue(urls[0]).isZoomed)
        compose.onNodeWithTag("gallery_page_0").performTouchInput { doubleClick() }
        compose.waitForIdle()
        assertFalse(states.getValue(urls[0]).isZoomed)
        compose.onNodeWithTag("gallery_pager").performTouchInput { swipeLeft() }
        compose.onNodeWithText("2 / 3").assertIsDisplayed()
    }

    @Test fun pinchZoomsAndLeavingPageRestores() {
        show()
        compose.onNodeWithTag("gallery_page_0").performTouchInput {
            pinch(center - Offset(40f, 0f), center - Offset(300f, 0f),
                center + Offset(40f, 0f), center + Offset(300f, 0f))
        }
        compose.waitForIdle()
        val state = states.getValue(urls[0])
        assertTrue("双指放大后应大于 1 倍：${state.scale}", state.scale > 2f)
        compose.onNodeWithTag("gallery_page_0").performTouchInput {
            pinch(center - Offset(300f, 0f), center - Offset(10f, 0f),
                center + Offset(300f, 0f), center + Offset(10f, 0f))
        }
        compose.waitForIdle()
        assertFalse(state.isZoomed)
    }

    @Test fun downloadAllAsksForConfirmation() {
        show()
        compose.onNodeWithContentDescription("更多").performClick()
        compose.onNodeWithText("下载全部").performClick()
        compose.onNodeWithText("取消").performClick()
        assertEquals(0, downloadAll)
        compose.onNodeWithContentDescription("更多").performClick()
        compose.onNodeWithText("下载全部").performClick()
        compose.onNodeWithText("确定").performClick()
        assertEquals(1, downloadAll)
    }
}
