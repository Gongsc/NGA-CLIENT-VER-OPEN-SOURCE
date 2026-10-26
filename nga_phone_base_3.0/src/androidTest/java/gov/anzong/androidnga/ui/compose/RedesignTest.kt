package gov.anzong.androidnga.ui.compose

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
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
import sp.phone.http.favorites.ForumFavoriteBoard
import sp.phone.http.favorites.ForumFavoritesSnapshot
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
        compose.setContent { NgaTheme { HomeScreen(state, emptyList(), {}, {}, {}, {}, {}, onRemove = {}, favorites = favorites) } }
        capture("compact-home.png")
        val compact = compose.onNodeWithTag("board:7:0").fetchSemanticsNode().boundsInRoot.height
        compose.onNodeWithContentDescription("标准模式").performClick()
        capture("standard-home.png")
        val standard = compose.onNodeWithTag("board:7:0").fetchSemanticsNode().boundsInRoot.height
        assertTrue("标准列表应保留更大的图标与留白", standard > compact * 1.5f)
        compose.runOnIdle {
            assertFalse(AppearanceSettings(prefs).compact)
            assertNotEquals(0, sp.phone.theme.ThemeManager.getInstance().webTextColor)
        }
        compose.onNodeWithContentDescription("紧凑模式").performClick()
        compose.runOnIdle { assertTrue(AppearanceSettings(prefs).compact) }
    }

    @Test fun paginationStaysBelowContentAndSupportsJumpAndBoundaries() {
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
        compose.onNodeWithContentDescription("回到首页").performClick()
        compose.onNodeWithText("正文 1").assertIsDisplayed()
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
