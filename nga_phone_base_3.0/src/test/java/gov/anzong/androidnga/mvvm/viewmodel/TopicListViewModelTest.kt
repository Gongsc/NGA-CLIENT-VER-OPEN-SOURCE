package gov.anzong.androidnga.mvvm.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import gov.anzong.androidnga.coroutines.PendingResponse
import gov.anzong.androidnga.mvvm.model.topicListUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.mvvm.model.BoardStore
import gov.anzong.androidnga.mvvm.model.entity.Board
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.mvvm.model.entity.TopicListInfo
import gov.anzong.androidnga.mvvm.model.param.TopicListParam
import java.io.IOException
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class TopicListViewModelTest {
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val main = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + main)
    @Before fun setMain() = Dispatchers.setMain(main)
    @After fun resetMain() = Dispatchers.resetMain()

    private fun topic(tid: Int, replies: Int = 0, postDate: Int = 0) = ThreadPageInfo().apply {
        this.tid = tid; this.replies = replies; this.postDate = postDate
    }

    private fun page(vararg topics: ThreadPageInfo, now: Int = 0) = TopicListInfo().apply {
        threadPageList = topics.toMutableList()
        curTime = now
    }

    private class Bookmarks : BoardStore by unusedBoards() {
        val keys = mutableSetOf<Pair<Int, Int>>()
        override fun isBookmark(fid: Int, stid: Int) = (fid to stid) in keys
        override fun addBookmark(board: Board) { keys += board.fid to board.stid }
        override fun removeBookmark(fid: Int, stid: Int) { keys -= fid to stid }
    }

    private fun viewModel(param: TopicListParam = TopicListParam().apply { fid = 7 },
                          loadPage: suspend (Int, TopicListParam) -> TopicListInfo = { p, _ -> page(topic(p)) },
                          removeFavorite: suspend (ThreadPageInfo) -> Boolean = { true },
                          boards: BoardStore = Bookmarks(), notices: MutableList<String> = mutableListOf()) =
        TopicListViewModel(param, loadPage, removeFavorite, boards, scope, main) { notices += it }

    @Test fun startLoadsFirstPageOnceAndNextPagesAppend() {
        val requested = mutableListOf<Int>()
        val vm = viewModel(loadPage = { p, _ -> requested += p; page(topic(p)) })
        vm.start(); vm.start()
        vm.loadNextPage(2)
        assertEquals(listOf(1, 2), requested)
        assertEquals(1, vm.firstPage.value!!.threadPageList[0].tid)
        assertEquals(2, vm.nextPage.value!!.threadPageList[0].tid)
        assertEquals(false, vm.refreshing.value)
    }

    @Test fun refreshCancelsPendingNextPage() {
        val pending = PendingResponse<TopicListInfo>()
        val vm = viewModel(loadPage = { p, _ -> if (p == 2) pending.await() else page(topic(p)) })
        vm.start(); vm.loadNextPage(2)
        assertEquals(true, vm.refreshing.value)
        vm.loadNextPage(2)
        vm.refresh()
        assertTrue(pending.cancelled)
        pending.complete(page(topic(2)))
        assertNull(vm.nextPage.value)
        assertEquals(false, vm.refreshing.value)
    }

    @Test fun failureIsReportedOnce() {
        val vm = viewModel(loadPage = { _, _ -> throw IOException("访客不能直接访问") })
        vm.start()
        assertEquals("访客不能直接访问", vm.error.value)
        assertNull(vm.firstPage.value)
        vm.errorShown()
        assertNull(vm.error.value)
    }

    @Test fun hotTopicsAreFilteredSortedTrimmedAndPaged() {
        val day = 24 * 60 * 60
        val now = 10 * day
        // 5 页共 60 个 24 小时内的主题和 1 个过期主题，另有 1 个跨页重复
        val pages = (1..5).map { p ->
            val topics = (1..12).map { i -> topic(p * 100 + i, replies = p * 100 + i, postDate = now - 60) }
            page(*(topics + listOf(topic(1, replies = 9999, postDate = now - day - 1), topic(101, replies = 101, postDate = now))).toTypedArray(), now = now)
        }
        val requested = mutableListOf<Int>()
        val vm = viewModel(param = TopicListParam().apply { fid = 7; twentyfour = 1 },
            loadPage = { p, _ -> requested += p; pages[p - 1] })
        vm.start()
        assertEquals(listOf(1, 2, 3, 4, 5), requested)
        val first = vm.firstPage.value!!.threadPageList
        assertEquals(20, first.size)
        assertEquals(512, first[0].tid)
        assertFalse(first.any { it.tid == 1 })

        vm.loadNextPage(2)
        val second = vm.nextPage.value!!
        vm.loadNextPage(3)
        val third = vm.nextPage.value!!
        assertNotSame(second, third)
        assertEquals(20, second.threadPageList.size)
        assertEquals(10, third.threadPageList.size)
        vm.loadNextPage(4)
        assertTrue(vm.nextPage.value!!.threadPageList.isEmpty())
        val all = first + second.threadPageList + third.threadPageList
        assertEquals(TopicListViewModel.HOT_TOPIC_COUNT, all.map { it.tid }.distinct().size)
        assertEquals(all.sortedByDescending { it.replies }, all)
        assertEquals(listOf(1, 2, 3, 4, 5), requested)
    }

    @Test fun removingFavoriteNotifiesOrReportsFailure() {
        var ok = true
        val notices = mutableListOf<String>()
        val vm = viewModel(removeFavorite = { if (ok) true else throw IOException() }, notices = notices)
        val info = topic(5)
        vm.removeFavorite(info)
        assertSame(info, vm.removedTopic.value)
        assertEquals(listOf("操作成功！"), notices)
        ok = false
        vm.removeFavorite(topic(6))
        assertEquals("操作失败!", vm.error.value)
        assertSame(info, vm.removedTopic.value)
    }

    @Test fun boardBookmarkUsesRequestBoard() {
        val vm = viewModel(param = TopicListParam().apply { fid = 7; stid = 3 })
        assertFalse(vm.isBookmarkBoard)
        vm.addBookmarkBoard(Board(7, 3, "合集"))
        assertTrue(vm.isBookmarkBoard)
        vm.removeBookmarkBoard()
        assertFalse(vm.isBookmarkBoard)
    }

    @Test fun topicListUrls() {
        val domain = "https://bbs.example.test"
        assertEquals("$domain/thread.php?fid=7&page=2&lite=js&noprefix",
            topicListUrl(domain, 2, TopicListParam().apply { fid = 7 }))
        assertEquals("$domain/thread.php?stid=3&page=1&lite=js&noprefix&recommend=1&order_by=postdatedesc&user=1",
            topicListUrl(domain, 1, TopicListParam().apply { fid = 7; stid = 3; recommend = 1 }))
        assertEquals("$domain/thread.php?authorid=5&searchpost=1&fid=7&page=1&lite=js&noprefix",
            topicListUrl(domain, 1, TopicListParam().apply { fid = 7; authorId = 5; searchPost = 1 }))
        assertEquals("$domain/thread.php?author=%C4%E3%BA%C3&searchpost=1&page=1&lite=js&noprefix",
            topicListUrl(domain, 1, TopicListParam().apply { author = "你好&searchpost=1" }))
        assertEquals("$domain/thread.php?favor=1&page=1&lite=js&noprefix",
            topicListUrl(domain, 1, TopicListParam().apply { favor = 1 }))
    }

    private companion object {
        fun unusedBoards(): BoardStore = BoardStore::class.java.cast(
            Proxy.newProxyInstance(BoardStore::class.java.classLoader, arrayOf(BoardStore::class.java)) { _, method, _ ->
                throw UnsupportedOperationException(method.name)
            })
    }
}
