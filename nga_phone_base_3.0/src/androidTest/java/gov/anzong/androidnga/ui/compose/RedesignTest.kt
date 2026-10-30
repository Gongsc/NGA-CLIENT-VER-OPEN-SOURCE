package gov.anzong.androidnga.ui.compose

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import gov.anzong.androidnga.common.PreferenceKey
import org.json.JSONTokener
import org.junit.*
import org.junit.Assert.*
import gov.anzong.androidnga.network.favorites.ForumFavoriteBoard
import gov.anzong.androidnga.network.favorites.ForumFavoritesSnapshot
import java.util.concurrent.atomic.AtomicReference

class RedesignTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val prefs get() = context.getSharedPreferences(PreferenceKey.PERFERENCE, Context.MODE_PRIVATE)
    private val keys = listOf(PALETTE_KEY, COMPACT_BOARDS_KEY, PreferenceKey.MATERIAL_THEME, PreferenceKey.NIGHT_MODE, PreferenceKey.KEY_NIGHT_MODE_FOLLOW_SYSTEM)
    private var backup = emptyMap<String, Any?>()
    @Before fun setup() {
        backup = keys.associateWith { prefs.all[it] }
        prefs.edit().putString(PALETTE_KEY, "GREEN").putBoolean(COMPACT_BOARDS_KEY, true)
            .putBoolean(PreferenceKey.KEY_NIGHT_MODE_FOLLOW_SYSTEM, false).putBoolean(PreferenceKey.NIGHT_MODE, false).commit()
    }
    @After fun restore() {
        val edit = prefs.edit()
        keys.forEach { key -> when (val value = backup[key]) { is String -> edit.putString(key,value); is Boolean -> edit.putBoolean(key,value); else -> edit.remove(key) } }
        edit.commit()
    }

    private fun capture(name: String) {
        val directory = java.io.File(context.filesDir, "ui-verification").apply { mkdirs() }
        java.io.File(directory, name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }

    @Test fun colorSelectionChangesWholePageAndDarkBackground() {
        compose.setContent { NgaTheme { AppearanceScreen {} } }
        val initial = compose.onNodeWithTag("appearance_screen").captureToImage().toPixelMap()[1,1].toArgb()
        compose.onNodeWithText("雾蓝").performClick()
        val blue = compose.onNodeWithTag("appearance_screen").captureToImage().toPixelMap()[1,1].toArgb()
        capture("appearance-blue.png")
        assertNotEquals(initial, blue)
        assertEquals(ngaColorScheme(AppPalette.BLUE,false).background.toArgb(), blue)
        compose.onNodeWithText("深色模式").performScrollTo().performClick()
        compose.onNodeWithText("深色", useUnmergedTree = true).performClick()
        val dark = compose.onNodeWithTag("appearance_screen").captureToImage().toPixelMap()[1,1].toArgb()
        capture("appearance-dark.png")
        assertEquals(ngaColorScheme(AppPalette.BLUE,true).background.toArgb(), dark)
        compose.onNodeWithContentDescription("恢复默认设置").performClick()
        compose.onNodeWithText("恢复").performClick()
        compose.runOnIdle { assertEquals(AppPalette.GREEN, AppearanceSettings(prefs).palette); assertTrue(AppearanceSettings(prefs).compact) }
    }

    @Test fun compactRowsAreSmallerAndDensityChoicePersists() {
        val state = HomeState().apply { categories = listOf(HomeCategory("我的收藏", true, emptyList())) }
        val names = listOf("议事厅","网事杂谈","消费电子","游戏综合","PC 游戏","手机游戏","最终幻想14","暗黑破坏神","炉石传说","守望先锋","硬件配置","软件与硬件","数码家电","影音讨论","旅游出行","读书会","摄影讨论","生活杂谈")
        val favorites = ForumFavoritesSnapshot("fixture", names.mapIndexed { index, name -> ForumFavoriteBoard(index+7,0,name) })
        val showAppearance = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent { NgaTheme {
            if (showAppearance.value) AppearanceScreen {}
            else HomeScreen(state, emptyList(), {}, {}, {}, {}, {}, onRemove = {}, favorites = favorites)
        } }
        compose.onNodeWithContentDescription("标准模式").assertDoesNotExist()
        compose.onNodeWithContentDescription("紧凑模式").assertDoesNotExist()
        compose.onNodeWithText("论坛收藏 · ${favorites.boards.size}").assertDoesNotExist()
        capture("compact-home.png")
        val compact = compose.onNodeWithTag("board:7:0").fetchSemanticsNode().boundsInRoot.height
        compose.runOnIdle { showAppearance.value = true }
        compose.onNodeWithContentDescription("标准模式").performScrollTo().performClick()
        compose.runOnIdle { showAppearance.value = false }
        capture("standard-home.png")
        val standard = compose.onNodeWithTag("board:7:0").fetchSemanticsNode().boundsInRoot.height
        assertTrue("标准列表应保留更大的图标与留白", standard > compact * 1.5f)
        compose.runOnIdle {
            assertFalse(AppearanceSettings(prefs).compact)
            assertNotEquals(0, gov.anzong.androidnga.ui.theme.ThemeManager.getInstance().webTextColor)
            showAppearance.value = true
        }
        compose.onNodeWithContentDescription("紧凑模式").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(AppearanceSettings(prefs).compact) }

    }

    @Test fun floatingPaginationSupportsJumpAndBoundaries() {
        val state = ArticleReaderState().apply { pages = 3; rows = 60 }
        compose.setContent { NgaTheme { ArticleReaderScreen(state,1,false,{},{},{},{},{}) { page, _ -> Text("正文 ${page+1}") } } }
        compose.onNodeWithContentDescription("上一页").assertIsNotEnabled()
        val footer = compose.onNodeWithTag("reader_pagination").fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithText("正文 1").fetchSemanticsNode().boundsInRoot
        assertTrue(footer.top > text.bottom)
        compose.onNodeWithContentDescription("跳转页码或楼层").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("3")
        compose.onNodeWithText("跳转", useUnmergedTree = true).performClick()
        compose.onNodeWithText("正文 3").assertIsDisplayed()
        compose.onNodeWithContentDescription("下一页").assertIsNotEnabled()
        compose.onNodeWithContentDescription("跳转页码或楼层").performClick()
        compose.onNodeWithContentDescription("回到首页").performClick()
        compose.onNodeWithText("正文 1").assertIsDisplayed()
    }

    @Test fun floatingReaderLightRightHand() = verifyFloatingReader(false, false)

    @Test fun floatingReaderDarkLeftHandWithLargeText() = verifyFloatingReader(true, true)

    private fun verifyFloatingReader(dark: Boolean, leftHand: Boolean) {
        val state = ArticleReaderState().apply { title = "主题阅读 · 离线示例"; pages = 3; rows = 60 }
        val pageState = ArticlePageState().apply {
            loading = false
            rows = (0..7).map { floor -> gov.anzong.androidnga.network.bean.ThreadRowInfo().apply {
                tid = 100; pid = floor; lou = floor; author = "示例用户 $floor"
                content = "这是一段用于检查阅读布局的离线正文。\n翻页与回复入口集中在屏幕下角，正文可以连续阅读。"
                postdate = "2026-10-27"
            } }
        }
        var replies = 0
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    density.density, if (dark) 2f else 1f),
            ) {
                NgaTheme {
                    androidx.compose.material3.MaterialTheme(colorScheme = ngaColorScheme(AppPalette.GREEN, dark)) {
                        ArticleReaderScreen(state, 1, leftHand, {}, {}, { replies++ }, {}, {}) { _, _ ->
                            ArticlePageScreen(pageState, {}, {}, {}, {}, { emptyList() }, { _, _ -> },
                                bottomContentPadding = readerActionsBottomPadding(),
                                onControlsExpandedChange = { state.controlsExpanded = it })
                        }
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("上一页").assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("回复主题").performClick()
        compose.runOnIdle { assertEquals(1, replies) }
        val controls = compose.onNodeWithTag("reader_pagination").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(if (leftHand) controls.center.x < root.center.x else controls.center.x > root.center.x)
        capture(if (dark) "reader-floating-dark-left-large.png" else "reader-floating-light.png")
        val replyExpanded = compose.onNodeWithContentDescription("回复主题").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("article_floors").performTouchInput { swipeUp() }
        compose.onNodeWithContentDescription("下一页").assertDoesNotExist()
        compose.onNodeWithText("回复", useUnmergedTree = true).assertDoesNotExist()
        val replyCollapsed = compose.onNodeWithContentDescription("回复主题").fetchSemanticsNode().boundsInRoot
        assertTrue("回复按钮应缩小", replyCollapsed.width < replyExpanded.width)
        assertEquals(replyExpanded.bottom, replyCollapsed.bottom, 1f)
        compose.onNodeWithContentDescription("跳转页码或楼层").assertIsDisplayed()
        capture(if (dark) "reader-collapsed-dark-left-large.png" else "reader-collapsed-light.png")
        compose.onNodeWithContentDescription("回复主题").performClick()
        compose.runOnIdle { assertEquals(2, replies) }
        compose.onNodeWithTag("article_floors").performTouchInput { swipeDown() }
        compose.onNodeWithContentDescription("下一页").assertIsDisplayed()
        compose.onNodeWithText("回复", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("article_floors").performScrollToIndex(7)
        compose.onNodeWithTag("article_floors").performTouchInput { swipeUp() }
        val lastFloor = compose.onNodeWithTag("floor_7").fetchSemanticsNode().boundsInRoot
        assertTrue("末尾回复应能滚动到悬浮控件上方", lastFloor.bottom <= controls.top)
        compose.onNodeWithContentDescription("跳转页码或楼层").performClick()
        compose.onNodeWithText("按楼层").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("42")
        compose.onNodeWithText("跳转", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(2, state.currentPage); assertEquals(42, state.floor) }
    }

    @Test fun embeddedHtmlFollowsThemeWithoutRemovingExplicitPostColors() {
        prefs.edit().putString(PALETTE_KEY, "BLUE").putBoolean(PreferenceKey.NIGHT_MODE,true).commit()
        val rootView = AtomicReference<android.view.View?>()
        compose.setContent { rootView.set(androidx.compose.ui.platform.LocalView.current); NgaTheme { ArticleHtml("<html><head></head><body style='background:white;color:black'><p>正文</p><div class='quote'>引用</div><span id='custom' style='color:red'>自定义文字</span></body></html>", emptyList()) } }
        var browser: WebView? = null
        compose.runOnIdle {
            fun find(view: android.view.View): WebView? {
                if(view is WebView) return view
                if(view is android.view.ViewGroup) for(i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
                return null
            }
            browser = find(rootView.get()!!.rootView)
        }
        val result = AtomicReference<String?>()
        compose.waitUntil(10000) {
            browser?.post { browser?.evaluateJavascript("""(function(){if(!document.getElementById('custom'))return '';return [getComputedStyle(document.body).backgroundColor,getComputedStyle(document.body).color,getComputedStyle(document.getElementById('custom')).color].join('|')})()""") { value ->
                val text = JSONTokener(value).nextValue() as? String
                if (!text.isNullOrBlank()) result.set(text)
            } }
            result.get() != null
        }
        assertEquals("rgb(29, 41, 54)|rgb(223, 234, 245)|rgb(255, 0, 0)", result.get())
    }
}
