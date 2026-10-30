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
import gov.anzong.androidnga.mvvm.model.param.ArticleListParam
import gov.anzong.androidnga.mvvm.model.param.ParamKey

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
                fun findReader(fragment: androidx.fragment.app.Fragment): gov.anzong.androidnga.ui.fragment.ArticleListFragment? {
                    if (fragment is gov.anzong.androidnga.ui.fragment.ArticleListFragment) return fragment
                    return fragment.childFragmentManager.fragments.firstNotNullOfOrNull(::findReader)
                }
                val fragment = activity.supportFragmentManager.fragments.firstNotNullOf(::findReader)
                // 换成按网络加载、且总是返回无法解析的响应，不发起真实请求
                fragment.viewModel.request.loadCache = false
                fragment.viewModel.pageLoader = { throw gov.anzong.androidnga.network.article.ArticlePageLoader.ResponseFormatException() }
                fragment.loadPage()
                org.junit.Assert.assertFalse(activity.isFinishing)
            }
            compose.onNodeWithText("主题加载异常测试").assertIsDisplayed()
            compose.onNodeWithText("主题数据暂时无法解析，请稍后重试。").assertIsDisplayed()
            compose.onNodeWithContentDescription("重新加载").assertIsDisplayed()
            scenario.onActivity { org.junit.Assert.assertFalse(it.isFinishing) }
        }
    }

    @Test fun realReaderScrollCollapsesControlsAcrossFragmentBoundary() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val request = ArticleListParam().apply { tid = 2147483000; page = 1; title = "滚动收起测试"; loadCache = true }
        val intent = Intent(context, ArticleListActivity::class.java).putExtra(ParamKey.KEY_PARAM, request)
        ActivityScenario.launch<ArticleListActivity>(intent).use { scenario ->
            compose.waitUntil(10000) { compose.onAllNodesWithText("读取缓存失败！").fetchSemanticsNodes().isNotEmpty() }
            scenario.onActivity { activity ->
                fun findReader(fragment: androidx.fragment.app.Fragment): gov.anzong.androidnga.ui.fragment.ArticleListFragment? {
                    if (fragment is gov.anzong.androidnga.ui.fragment.ArticleListFragment) return fragment
                    return fragment.childFragmentManager.fragments.firstNotNullOfOrNull(::findReader)
                }
                val fragment = activity.supportFragmentManager.fragments.firstNotNullOf(::findReader)
                fragment.setData(gov.anzong.androidnga.network.bean.ThreadData().apply {
                    __ROWS = 20
                    rowList = (0..19).map { floor -> gov.anzong.androidnga.network.bean.ThreadRowInfo().apply {
                        tid = request.tid; pid = floor; lou = floor; author = "离线示例 $floor"
                        content = "此处是离线正文，用于验证真实分页宿主中的上下滑动收起与展开。".repeat(4)
                    } }
                })
                fragment.hideLoadingView()
            }
            compose.onNodeWithText("回复", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithTag("article_floors").performTouchInput { swipeUp() }
            compose.onNodeWithContentDescription("上一页").assertDoesNotExist()
            compose.onNodeWithText("回复", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithContentDescription("回复主题").assertIsDisplayed()
            compose.onNodeWithContentDescription("跳转页码或楼层").assertIsDisplayed()
            compose.onNodeWithTag("article_floors").performTouchInput { swipeDown() }
            compose.onNodeWithContentDescription("上一页").assertIsDisplayed()
            compose.onNodeWithText("回复", useUnmergedTree = true).assertIsDisplayed()
        }
    }

}
