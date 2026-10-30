package gov.anzong.androidnga.mvvm.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import gov.anzong.androidnga.coroutines.PendingResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.mvvm.model.param.MessagePostParam
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MessagePostViewModelTest {
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val main = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + main)

    private fun viewModel(param: MessagePostParam, submit: suspend (MessagePostParam) -> String) =
        MessagePostViewModel(param, submit, scope, main)

    @Test fun secondSendWhileSubmittingIsRejected() {
        val pending = PendingResponse<String>()
        val sent = mutableListOf<MessagePostParam>()
        val vm = viewModel(MessagePostParam("new", 0, "")) { sent += it; pending.await() }
        assertTrue(vm.send("标题", "收件人", "正文内容"))
        assertFalse(vm.send("标题", "收件人", "正文内容"))
        assertEquals(1, sent.size)
        assertEquals(true, vm.sending.value)
        assertNull(vm.result.value)

        pending.complete("发送完毕")
        val result = vm.result.value!!
        assertTrue(result.success)
        assertEquals("发送完毕", result.message)
        assertEquals(false, vm.sending.value)
        vm.resultShown()
        assertNull(vm.result.value)
    }

    @Test fun formFieldsAreCopiedIntoTheRequest() {
        var sent: MessagePostParam? = null
        val vm = viewModel(MessagePostParam("reply", 9, "原标题")) { sent = it; "ok" }
        assertFalse(vm.isNewMessage)
        vm.send("回复标题", "甲", "回复正文")
        assertEquals("reply", sent!!.action)
        assertEquals(9, sent!!.mid)
        assertEquals("回复标题", sent!!.postSubject)
        assertEquals("甲", sent!!.recipient)
        assertEquals("回复正文", sent!!.postContent)
    }

    @Test fun failureReportsReasonAndAllowsRetry() {
        var fail = true
        val vm = viewModel(MessagePostParam("new", 0, "")) { if (fail) throw IOException("网络错误") else "ok" }
        vm.send("", "甲", "正文内容")
        assertFalse(vm.result.value!!.success)
        assertEquals("网络错误", vm.result.value!!.message)
        assertEquals(false, vm.sending.value)
        fail = false
        assertTrue(vm.send("", "甲", "正文内容"))
        assertTrue(vm.result.value!!.success)
    }

    @Test fun failureWithoutMessageUsesGenericReason() {
        val vm = viewModel(MessagePostParam("new", 0, "")) { throw IOException() }
        vm.send("", "甲", "正文内容")
        assertEquals("发送失败，请稍后重试", vm.result.value!!.message)
    }

    @Test fun paramComesFromFragmentArguments() {
        val param = MessagePostParam("new", 0, "").apply { recipient = "乙" }
        val vm = MessagePostViewModel(SavedStateHandle(mapOf("param" to param)))
        assertTrue(vm.isNewMessage)
        assertEquals("乙", vm.initialRecipient)
    }
}
