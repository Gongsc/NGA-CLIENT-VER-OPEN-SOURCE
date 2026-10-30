package gov.anzong.androidnga.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import gov.anzong.androidnga.activity.ArticleCacheActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.mvvm.model.param.ArticleListParam
import gov.anzong.androidnga.mvvm.model.param.ParamKey
import java.io.File

/** 离线缓存阅读页的分页与页码标签（Compose Pager）；只用本地合成缓存，不发起请求。 */
class ArticleCacheActivityTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val tid = 2147483001
    private val cacheDir get() = File(context.filesDir, "cache/$tid")

    @Before fun createCache() {
        cacheDir.mkdirs()
        File(cacheDir, "1.json").writeText("{}")
        File(cacheDir, "2.json").writeText("{}")
        File(cacheDir, "10.json").writeText("{}")
    }

    @After fun removeCache() {
        cacheDir.deleteRecursively()
    }

    @Test fun cachedPagesBecomeTabsInPageOrderAndSelectingATabChangesPage() {
        val request = ArticleListParam().apply { tid = this@ArticleCacheActivityTest.tid; title = "离线缓存测试"; loadCache = true }
        val intent = Intent(context, ArticleCacheActivity::class.java).putExtra(ParamKey.KEY_PARAM, request)
        ActivityScenario.launch<ArticleCacheActivity>(intent).use {
            compose.onNodeWithText("离线缓存测试").assertIsDisplayed()
            compose.onNodeWithText("1").assertIsSelected()
            compose.onNodeWithText("10").assertExists()
            compose.onNodeWithText("2").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("2").assertIsSelected()
        }
    }
}
