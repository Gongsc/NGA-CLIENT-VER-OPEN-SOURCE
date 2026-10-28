package gov.anzong.androidnga.mvvm.viewmodel

import gov.anzong.androidnga.coroutines.PendingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import sp.phone.http.bean.MessageListInfo
import sp.phone.http.bean.MessageThreadPageInfo
import sp.phone.mvp.model.entity.RecentReplyInfo

@OptIn(ExperimentalCoroutinesApi::class)
class HomeInboxViewModelTest {
    private val main = UnconfinedTestDispatcher()
    @Before fun setMain() = Dispatchers.setMain(main)
    @After fun resetMain() = Dispatchers.resetMain()
    private fun page(number: Int, next: Int, vararg mids: Int) = MessageListInfo().apply {
        set__currentPage(number); set__nextPage(next)
        messageEntryList = mids.map { mid -> MessageThreadPageInfo().apply { this.mid = mid } }
    }

    @Test fun accountSwitchCancelsOldRequestsAndClearsData() {
        var account = "a"
        val pending = PendingResponse<MessageListInfo>()
        val vm = HomeInboxViewModel({ account }, { "cookie-$account" }, { emptyList() },
            { cookie, _ -> if (cookie == "cookie-a") pending.await() else page(1, 0, 2) },
            main)
        vm.selectAccount("a"); vm.loadMessages()
        account = "b"; vm.selectAccount("b")
        assertTrue(pending.cancelled)
        assertTrue(vm.messages.isEmpty())
        pending.complete(page(1, 0, 1))
        vm.loadMessages()
        assertEquals(listOf(2), vm.messages.map { it.mid })
        account = ""; vm.selectAccount("")
        assertTrue(vm.messages.isEmpty())
        assertEquals(0, vm.nextPage)
    }

    @Test fun paginationDeduplicatesAndFailedRefreshPreservesRows() {
        var fail = false
        val vm = HomeInboxViewModel({ "a" }, { "cookie" }, { emptyList() },
            { _, number -> if (fail) throw IllegalStateException("离线") else if (number == 1) page(1, 2, 1, 2) else page(2, 0, 2, 3) },
            main)
        vm.selectAccount("a"); vm.loadMessages(); vm.loadMessages(more = true)
        assertEquals(listOf(1, 2, 3), vm.messages.map { it.mid })
        assertEquals(0, vm.nextPage)
        fail = true; vm.loadMessages(force = true)
        assertEquals("离线", vm.messageError)
        assertEquals(3, vm.messages.size)
        assertFalse(vm.messagesLoading)
        fail = false; vm.retryMessages()
        assertNull(vm.messageError)
        assertEquals(listOf(1, 2), vm.messages.map { it.mid })
    }

    @Test fun unreadRepliesTriggerOnlyOneFollowupAndMergeDuplicates() {
        var calls = 0
        var read = 0
        fun reply(pid: String, unread: Boolean) = RecentReplyInfo(1, "示例", "主题", pid, "100").apply { isUnread = unread }
        val vm = HomeInboxViewModel({ "a" }, { "cookie" }, {
            calls++
            if (calls == 1) listOf(reply("1", true)) else listOf(reply("1", false), reply("2", false))
        }, { _, _ -> page(1, 0) }, main)
        vm.selectAccount("a"); vm.loadReplies(onRead = { read++ })
        assertEquals(2, calls)
        assertEquals(2, vm.replies.size)
        assertEquals(1, read)
        vm.loadReplies()
        assertEquals(2, calls)
    }

    @Test fun renewedSessionForSameUserInvalidatesCachedMessages() {
        var cookie = "old"
        var calls = 0
        val vm = HomeInboxViewModel({ "a" }, { cookie }, { emptyList() },
            { _, _ -> calls++; page(1, 0, calls) }, main)
        vm.selectAccount("a"); vm.loadMessages()
        cookie = "new"; vm.selectAccount("a")
        assertTrue(vm.messages.isEmpty())
        vm.loadMessages()
        assertEquals(listOf(2), vm.messages.map { it.mid })
    }

    @Test fun replyResponseWithoutNotificationsIsEmptyNotError() {
        val prefix = "window.script_muti_get_var_store="
        assertNull(homeReplyError(prefix + "{\"data\":{},\"time\":1}"))
        assertNull(homeReplyError("{\"data\":{\"0\":{\"0\":[]}}}"))
        assertTrue(sp.phone.mvp.model.convert.ForumNotificationFactory.buildRecentReplyList(prefix + "{\"data\":{}}").isEmpty())
        val reply = "{\"0\":1,\"1\":\"7\",\"2\":\"示例\",\"5\":\"主题\",\"6\":\"100\",\"7\":\"200\",\"9\":\"1\"}"
        assertEquals(1, sp.phone.mvp.model.convert.ForumNotificationFactory.buildRecentReplyList("{\"data\":{\"0\":{\"0\":[$reply]}}}").size)
    }

    @Test fun replyResponseWithServerErrorFails() {
        assertEquals("你需要登录", homeReplyError("{\"error\":{\"0\":\"你需要登录\"}}"))
        assertNotNull(homeReplyError(""))
    }

    @Test fun repliesAreShownNewestFirst() {
        fun reply(pid: String, time: String?) = RecentReplyInfo(1, "示例", "主题", pid, "100").apply { timeStamp = time }
        val vm = HomeInboxViewModel({ "a" }, { "cookie" }, {
            listOf(reply("1", "100"), reply("2", null), reply("3", "300"), reply("4", "200"))
        }, { _, _ -> page(1, 0) }, main)
        vm.selectAccount("a"); vm.loadReplies()
        assertEquals(listOf("3", "4", "1", "2"), vm.replies.map { it.pidStr })
    }

    @Test fun clearingRepliesCallsServerOnceAndDropsPendingLoad() {
        var cleared = 0
        val pending = CompletableDeferred<List<RecentReplyInfo>>()
        val vm = HomeInboxViewModel({ "a" }, { "cookie" }, { pending.await() }, { _, _ -> page(1, 0) }, main, { cleared++ })
        vm.clearReplies()
        assertEquals(0, cleared)
        vm.selectAccount("a"); vm.loadReplies()
        assertTrue(vm.repliesLoading)
        vm.clearReplies()
        assertEquals(1, cleared)
        assertFalse(vm.repliesLoading)
        pending.complete(listOf(RecentReplyInfo(1, "示例", "主题", "1", "100")))
        assertTrue(vm.replies.isEmpty())
        vm.loadReplies()
        assertTrue(vm.replies.isEmpty())
    }
}
