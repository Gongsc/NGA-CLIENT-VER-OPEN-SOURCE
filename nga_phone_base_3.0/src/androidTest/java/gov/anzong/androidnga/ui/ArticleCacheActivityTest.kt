package gov.anzong.androidnga.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import gov.anzong.androidnga.R
import gov.anzong.androidnga.activity.ArticleCacheActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import sp.phone.param.ArticleListParam
import sp.phone.param.ParamKey
import java.io.File

/** 离线缓存阅读页的分页与标签（ViewPager2 + TabLayoutMediator）；只用本地合成缓存，不发起请求。 */
class ArticleCacheActivityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val tid = 2147483001
    private val cacheDir get() = File(context.filesDir, "cache/$tid")

    @Before fun createCache() {
        cacheDir.mkdirs()
        File(cacheDir, "1.json").writeText("{}")
        File(cacheDir, "2.json").writeText("{}")
    }

    @After fun removeCache() {
        cacheDir.deleteRecursively()
    }

    @Test fun cachedPagesBecomeTabsAndSelectingATabChangesPage() {
        val request = ArticleListParam().apply { tid = this@ArticleCacheActivityTest.tid; title = "离线缓存测试"; loadCache = true }
        val intent = Intent(context, ArticleCacheActivity::class.java).putExtra(ParamKey.KEY_PARAM, request)
        ActivityScenario.launch<ArticleCacheActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val tabs = activity.findViewById<TabLayout>(R.id.tabs)
                val pager = activity.findViewById<ViewPager2>(R.id.pager)
                assertEquals(2, pager.adapter?.itemCount)
                assertEquals(listOf("1", "2"), (0 until tabs.tabCount).map { tabs.getTabAt(it)?.text.toString() })
                tabs.getTabAt(1)?.select()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(1, activity.findViewById<ViewPager2>(R.id.pager).currentItem)
            }
        }
    }
}
