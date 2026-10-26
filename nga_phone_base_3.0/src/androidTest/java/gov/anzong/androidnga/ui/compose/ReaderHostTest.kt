package gov.anzong.androidnga.ui.compose

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.activity.ArticleListActivity
import sp.phone.param.ArticleListParam
import sp.phone.param.ParamKey

class ReaderHostTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun realReaderHostHandlesMissingCacheWithoutNetworkRequests() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val request = ArticleListParam().apply { tid = 2147483000; page = 1; title = "离线详情测试"; loadCache = true }
        val intent = Intent(context, ArticleListActivity::class.java).putExtra(ParamKey.KEY_PARAM, request)
        ActivityScenario.launch<ArticleListActivity>(intent).use {
            compose.onNodeWithText("离线详情测试").assertIsDisplayed()
            compose.waitUntil(timeoutMillis = 10000) { compose.onAllNodesWithText("读取缓存失败！").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("重新加载").assertIsDisplayed()
            compose.onNodeWithContentDescription("跳转页码或楼层").assertIsDisplayed()
            compose.onNodeWithContentDescription("帖子操作").performClick()
            compose.onNodeWithText("复制链接").assertExists()
        }
    }
    @Test fun responseFormatFailureKeepsReaderOpenWithRetryAction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val request = ArticleListParam().apply { tid = 2147483000; page = 1; title = "主题加载异常测试"; loadCache = true }
        val intent = Intent(context, ArticleListActivity::class.java).putExtra(ParamKey.KEY_PARAM, request)
        ActivityScenario.launch<ArticleListActivity>(intent).use { scenario ->
            compose.waitUntil(timeoutMillis = 10000) { compose.onAllNodesWithText("读取缓存失败！").fetchSemanticsNodes().isNotEmpty() }
            scenario.onActivity { activity ->
                fun findReader(fragment: androidx.fragment.app.Fragment): sp.phone.ui.fragment.ArticleListFragment? {
                    if (fragment is sp.phone.ui.fragment.ArticleListFragment) return fragment
                    return fragment.childFragmentManager.fragments.firstNotNullOfOrNull(::findReader)
                }
                val fragment = activity.supportFragmentManager.fragments.firstNotNullOf(::findReader)
                val presenter = object : sp.phone.mvp.presenter.ArticleListPresenter(request) {
                    override fun onCreateModel() = object : sp.phone.mvp.model.ArticleListModel() {
                        override fun loadPage(param: ArticleListParam, headers: MutableMap<String, String>?, callback: gov.anzong.androidnga.http.OnHttpCallBack<sp.phone.http.bean.ThreadData>) {
                            val error = sp.phone.http.article.ArticlePageLoader.ResponseFormatException()
                            callback.onError(error.message, error)
                        }
                    }
                }
                presenter.attachView(fragment)
                presenter.loadPage(request)
                org.junit.Assert.assertFalse(activity.isFinishing)
            }
            compose.onNodeWithText("主题加载异常测试").assertIsDisplayed()
            compose.onNodeWithText("主题数据暂时无法解析，请稍后重试。").assertIsDisplayed()
            compose.onNodeWithContentDescription("重新加载").assertIsDisplayed()
            scenario.onActivity { org.junit.Assert.assertFalse(it.isFinishing) }
        }
    }

}
