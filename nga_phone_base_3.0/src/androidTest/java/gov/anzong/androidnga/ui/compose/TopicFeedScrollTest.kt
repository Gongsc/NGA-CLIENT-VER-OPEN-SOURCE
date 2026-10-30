package gov.anzong.androidnga.ui.compose

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import gov.anzong.androidnga.common.PreferenceKey
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.settings.PhoneConfiguration
import java.io.File

class TopicFeedScrollTest {
    @get:Rule val compose = createComposeRule()

    @Test fun postButtonFollowsScrollInLightRightHandMode() = verifyPostButton(false)
    @Test fun postButtonFollowsScrollInDarkLeftHandModeWithLargeText() = verifyPostButton(true)

    private fun verifyPostButton(dark: Boolean) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences(PreferenceKey.PERFERENCE, Context.MODE_PRIVATE)
        val previous = prefs.all[PreferenceKey.LEFT_HAND]
        val config = PhoneConfiguration.getInstance()
        val state = TopicFeedState().apply {
            title = "主题列表 · 离线示例"
            canPost = true
            hasMore = false
            // 标题与回复数取自固定示例，覆盖长标题换行与热帖胶囊，同时用于 README 预览截图。
            val titles = listOf("示例主题 0：阅读与交流", "新版本更新内容汇总与常见问题解答，欢迎补充", "分享一下最近拍的游戏截图",
                "周末适合放松的单机游戏有哪些推荐", "装机讨论：预算五千元如何在显卡与处理器之间取舍", "长期更新：新手入门资料整理")
            val replies = listOf(12, 86, 5, 57, 23, 140)
            rows = (0..29).map {
                TopicRow("sample_$it", AnnotatedString(titles.getOrElse(it) { _ -> "示例主题 $it：阅读与交流" }),
                    "示例作者 ${it % 4 + 1}", "示例回复者", replies.getOrElse(it) { _ -> it }, null)
            }
        }
        var posts = 0
        try {
            prefs.edit().putBoolean(PreferenceKey.LEFT_HAND, dark).commit()
            config.onSharedPreferenceChanged(prefs, PreferenceKey.LEFT_HAND)
            compose.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (dark) 2f else 1f)) {
                    NgaTheme {
                        MaterialTheme(colorScheme = ngaColorScheme(AppPalette.GREEN, dark)) {
                            TopicFeedScreen(state, {}, {}, {}, {}, {}, { posts++ }, {})
                        }
                    }
                }
            }
            val button = compose.onNodeWithContentDescription("发表主题")
            val expanded = button.assertIsDisplayed().assertHeightIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
            val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
            assertTrue(if (dark) expanded.center.x < root.center.x else expanded.center.x > root.center.x)
            capture(context, "topic-post-expanded-${if (dark) "dark-left-large" else "light"}.png")
            compose.onNodeWithTag("topic_feed_list").performTouchInput { swipeUp() }
            compose.onNodeWithText("发表主题", useUnmergedTree = true).assertDoesNotExist()
            val collapsed = button.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(collapsed.width < expanded.width)
            assertEquals(expanded.bottom, collapsed.bottom, 1f)
            capture(context, "topic-post-collapsed-${if (dark) "dark-left-large" else "light"}.png")
            button.performClick()
            compose.runOnIdle { assertEquals(1, posts) }
            compose.onNodeWithTag("topic_feed_list").performTouchInput { swipeDown() }
            compose.onNodeWithText("发表主题", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithTag("topic_feed_list").performTouchInput { swipeUp() }
            compose.onNodeWithText("发表主题", useUnmergedTree = true).assertDoesNotExist()
            compose.runOnIdle { state.refreshGeneration++ }
            compose.onNodeWithText("发表主题", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithText("示例主题 0：阅读与交流").assertIsDisplayed()
            compose.runOnIdle { state.canPost = false }
            button.assertDoesNotExist()
        } finally {
            val edit = prefs.edit()
            if (previous is Boolean) edit.putBoolean(PreferenceKey.LEFT_HAND, previous) else edit.remove(PreferenceKey.LEFT_HAND)
            edit.commit()
            config.onSharedPreferenceChanged(prefs, PreferenceKey.LEFT_HAND)
        }
    }

    private fun capture(context: Context, name: String) {
        val dir = File(context.filesDir, "ui-verification").apply { mkdirs() }
        File(dir, name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
