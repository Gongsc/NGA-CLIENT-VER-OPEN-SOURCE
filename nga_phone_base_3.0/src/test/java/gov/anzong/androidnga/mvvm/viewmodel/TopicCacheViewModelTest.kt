package gov.anzong.androidnga.mvvm.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.mvp.model.entity.TopicListInfo
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TopicCacheViewModelTest {
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val main = UnconfinedTestDispatcher()
    @Before fun setMain() = Dispatchers.setMain(main)
    @After fun resetMain() = Dispatchers.resetMain()

    private fun topic(tid: Int) = ThreadPageInfo().apply { setTid(tid) }

    private val TopicCacheViewModel.noticeText get() = notice.value?.let { (if (it.success) "✓" else "✗") + it.text }

    @Test fun cacheIsReadOnceUntilReloaded() {
        var reads = 0
        val vm = TopicCacheViewModel(loadCache = { reads++; TopicListInfo().apply { addThreadPage(topic(reads)) } }, worker = main)
        vm.start(); vm.start()
        assertEquals(1, reads)
        assertEquals(false, vm.loading.value)
        vm.reload()
        assertEquals(2, vm.topics.value!!.threadPageList[0].tid)
    }

    @Test fun readFailureIsReported() {
        val vm = TopicCacheViewModel(loadCache = { throw IOException("没有缓存目录") }, worker = main)
        vm.start()
        assertEquals("✗读取缓存失败！", vm.noticeText)
        assertEquals(false, vm.loading.value)
        vm.noticeShown()
        assertNull(vm.notice.value)
    }

    @Test fun deleteRemovesTopicOrReportsFailure() {
        val deleted = mutableListOf<Int>()
        val vm = TopicCacheViewModel(deleteCache = { tid -> deleted += tid; tid == 1 }, worker = main)
        val info = topic(1)
        vm.delete(info)
        assertSame(info, vm.removedTopic.value)
        assertEquals("✓删除成功！", vm.noticeText)
        vm.delete(topic(2))
        assertEquals("✗删除失败！", vm.noticeText)
        assertSame(info, vm.removedTopic.value)
        assertEquals(listOf(1, 2), deleted)
    }

    @Test fun exportReportsResult() {
        var fail = false
        val vm = TopicCacheViewModel(exportCache = { if (fail) throw IOException("打包失败") }, worker = main)
        vm.exportFile()
        assertEquals("✓已导出至下载目录的 NGA 文件夹", vm.noticeText)
        fail = true
        vm.exportFile()
        assertEquals("✗导出失败", vm.noticeText)
    }
}
