package gov.anzong.androidnga.mvvm.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import gov.anzong.androidnga.R
import gov.anzong.androidnga.coroutines.PendingResponse
import gov.anzong.androidnga.mvvm.model.articleUrl
import gov.anzong.androidnga.mvvm.model.quoteBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import sp.phone.common.UserManager
import sp.phone.http.bean.ThreadData
import sp.phone.http.bean.ThreadRowInfo
import sp.phone.param.ArticleListParam
import java.io.IOException
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleListViewModelTest {
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val main = UnconfinedTestDispatcher()
    @Before fun setMain() = Dispatchers.setMain(main)
    @After fun resetMain() = Dispatchers.resetMain()

    private fun data(raw: String = "raw") = ThreadData().apply { rawData = raw }

    private fun request(cache: Boolean = false) = ArticleListParam().apply { tid = 12; page = 0; loadCache = cache }

    private val notices = mutableListOf<String>()

    private fun viewModel(request: ArticleListParam = request(),
                          page: suspend (ArticleListParam) -> ThreadData = { data() },
                          cache: (ArticleListParam) -> ThreadData = { error("不应读取缓存") },
                          write: (ArticleListParam, String?) -> Unit = { _, _ -> },
                          users: UserManager = unusedUsers()) =
        ArticleListViewModel(request, page, cache, write, { users }, main) { message, success ->
            notices += (if (success) "✓" else "✗") + message
        }

    @Test fun startLoadsOnceAndPageIsAtLeastOne() {
        var calls = 0
        val vm = viewModel(page = { calls++; data() })
        assertEquals(1, vm.request.page)
        vm.start(); vm.start()
        assertEquals(1, calls)
        assertEquals("raw", vm.data.value?.rawData)
        assertEquals(false, vm.loading.value)
    }

    @Test fun reloadIsIgnoredWhileLoading() {
        val pending = PendingResponse<ThreadData>()
        var calls = 0
        val vm = viewModel(page = { calls++; pending.await() })
        vm.load(); vm.load()
        assertEquals(1, calls)
        assertEquals(true, vm.loading.value)
        pending.complete(data())
        assertEquals(false, vm.loading.value)
    }

    @Test fun failureKeepsLastContentAndClearsOnRetry() {
        var fail = false
        val vm = viewModel(page = { if (fail) throw IOException("主题数据暂时无法解析，请稍后重试。") else data("第一次") })
        vm.load()
        fail = true
        vm.load()
        assertEquals("主题数据暂时无法解析，请稍后重试。", vm.error.value)
        assertEquals("第一次", vm.data.value?.rawData)
        fail = false
        vm.load()
        assertNull(vm.error.value)
    }

    @Test fun cachedPageReadsCacheAndReportsGenericFailure() {
        val vm = viewModel(request = request(cache = true), page = { error("不应请求网络") }, cache = { throw IOException("文件不存在") })
        vm.start()
        assertEquals("读取缓存失败！", vm.error.value)
    }

    @Test fun saveCacheNeedsLoadedContentAndTopicInfo() {
        val written = mutableListOf<String?>()
        var fail = false
        val request = request()
        val vm = viewModel(request = request, write = { _, raw -> if (fail) throw IOException() else written += raw })
        vm.saveCache()
        assertTrue(notices.isEmpty())
        vm.load()
        vm.saveCache()
        assertEquals(listOf("✗缓存失败！"), notices)
        request.topicInfo = "{}"
        vm.saveCache()
        fail = true
        vm.saveCache()
        assertEquals(listOf("raw"), written)
        assertEquals(listOf("✗缓存失败！", "✓缓存成功！", "✗缓存失败！"), notices)
    }

    @Test fun blacklistTogglesAndSkipsAnonymousRows() {
        val list = mutableMapOf<String, String>()
        val users = object : UserManager by unusedUsers() {
            override fun addToBlackList(authorName: String, authorId: String) { list[authorId] = authorName }
            override fun removeFromBlackList(authorId: String) { list.remove(authorId) }
        }
        val vm = viewModel(users = users)
        val row = ThreadRowInfo().apply { authorid = 5; author = "甲" }
        assertEquals(R.string.add_to_blacklist_success, vm.toggleBlacklist(row))
        assertEquals(mapOf("5" to "甲"), list)
        assertTrue(row.get_isInBlackList())
        assertEquals(R.string.remove_from_blacklist_success, vm.toggleBlacklist(row))
        assertTrue(list.isEmpty())
        val anonymous = ThreadRowInfo().apply { authorid = 6; author = "乙"; setISANONYMOUS(true) }
        assertEquals(R.string.cannot_add_to_blacklist_cause_anony, vm.toggleBlacklist(anonymous))
        assertTrue(list.isEmpty())
    }

    @Test fun articleUrls() {
        val domain = "https://bbs.example.test"
        assertEquals("$domain/read.php?page=1&tid=12", articleUrl(domain, ArticleListParam().apply { tid = 12 }))
        assertEquals("$domain/read.php?page=3&tid=12&pid=99&authorid=5",
            articleUrl(domain, ArticleListParam().apply { tid = 12; page = 3; pid = 99; authorId = 5 }))
    }

    @Test fun quoteStripsNestedQuotesAndNamesAuthor() {
        val param = ArticleListParam().apply { tid = 12; page = 2 }
        val row = ThreadRowInfo().apply {
            pid = 99; tid = 12; authorid = 5; author = "甲"; postdate = "2026-10-28 12:00"; lou = 21
            content = "[quote]旧引用[/quote][b]Reply to [pid=1,12,1]Reply[/pid] Post by 乙[/b] 正文 "
        }
        val trim: (String) -> String = { it.trim() }
        assertEquals("[quote][pid=99,12,2]Reply[/pid] [b]Post by [uid=5]甲[/uid] (2026-10-28 12:00):[/b]\n正文[/quote]\n",
            quoteBlock(param, row, trim))
        row.setISANONYMOUS(true)
        assertTrue(quoteBlock(param, row, trim).contains("[uid=-1]甲[/uid][color=gray](21楼)[/color]"))
        param.tid = 0
        row.tid = 34
        assertTrue(quoteBlock(param, row, trim).startsWith("[quote][pid=99,34,2]"))
    }

    private companion object {
        fun unusedUsers(): UserManager = UserManager::class.java.cast(
            Proxy.newProxyInstance(UserManager::class.java.classLoader, arrayOf(UserManager::class.java)) { _, method, _ ->
                throw UnsupportedOperationException(method.name)
            })
    }
}
