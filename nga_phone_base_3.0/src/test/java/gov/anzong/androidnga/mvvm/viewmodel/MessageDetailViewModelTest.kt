package gov.anzong.androidnga.mvvm.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import gov.anzong.androidnga.coroutines.PendingResponse
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
import sp.phone.http.bean.MessageArticlePageInfo
import sp.phone.http.bean.MessageDetailInfo

@OptIn(ExperimentalCoroutinesApi::class)
class MessageDetailViewModelTest {
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val main = UnconfinedTestDispatcher()
    @Before fun setMain() = Dispatchers.setMain(main)
    @After fun resetMain() = Dispatchers.resetMain()

    private fun page(number: Int, next: Int, title: String = "标题") = MessageDetailInfo().apply {
        set__currentPage(number); set__nextPage(next); set_Title(title); set_Alluser("甲 乙")
        messageEntryList = listOf(MessageArticlePageInfo())
    }

    private val MessageDetailViewModel.pageNumbers get() = pages.value.orEmpty().map { it.get__currentPage() }

    @Test fun pagesAccumulateUntilLastPageAndRefreshStartsOver() {
        val requested = mutableListOf<Pair<Int, Int>>()
        val vm = MessageDetailViewModel(7, { number, mid -> requested += number to mid; page(number, if (number < 2) number + 1 else 0) }, main)
        vm.refresh(); vm.loadNextPage(); vm.loadNextPage()
        assertEquals(listOf(1 to 7, 2 to 7), requested)
        assertEquals(listOf(1, 2), vm.pageNumbers)
        assertFalse(vm.hasNextPage)
        assertEquals(false, vm.loading.value)
        assertEquals("甲 乙", vm.recipient)
        vm.refresh()
        assertEquals(listOf(1), vm.pageNumbers)
    }

    @Test fun refreshCancelsPendingNextPage() {
        val pending = PendingResponse<MessageDetailInfo>()
        val vm = MessageDetailViewModel(1, { number, _ -> if (number == 2) pending.await() else page(1, 2) }, main)
        vm.refresh(); vm.loadNextPage()
        assertEquals(true, vm.loading.value)
        vm.refresh()
        assertTrue(pending.cancelled)
        pending.complete(page(2, 0))
        assertEquals(listOf(1), vm.pageNumbers)
        assertEquals(false, vm.loading.value)
    }

    @Test fun failureKeepsLoadedPagesAndReportsErrorOnce() {
        var fail = false
        val vm = MessageDetailViewModel(1, { number, _ -> if (fail) throw IllegalStateException("没有权限") else page(number, 2) }, main)
        vm.refresh()
        fail = true; vm.loadNextPage()
        assertEquals("没有权限", vm.error.value)
        assertEquals(listOf(1), vm.pageNumbers)
        assertEquals(false, vm.loading.value)
        vm.errorShown()
        assertNull(vm.error.value)
    }

    @Test fun midComesFromFragmentArguments() {
        assertEquals(42, MessageDetailViewModel(SavedStateHandle(mapOf("mid" to 42))).mid)
        val empty = MessageDetailViewModel(SavedStateHandle())
        assertEquals(0, empty.mid)
        assertFalse(empty.isLoaded)
        assertTrue(empty.hasNextPage)
        assertNull(empty.title)
    }
}
