package gov.anzong.androidnga.mvvm.viewmodel

import gov.anzong.androidnga.coroutines.PendingResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Test
import sp.phone.http.post.NgaWriteClient
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AccountEditViewModelTest {
    private val main = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + main)

    private fun viewModel(sign: suspend (String) -> String = { "修改成功！" },
                          avatar: suspend (String) -> NgaWriteClient.ActionResult = { NgaWriteClient.ActionResult(true, "") }) =
        AccountEditViewModel(sign, avatar, scope, main)

    @Test fun signatureSubmitsOnceAndReturnsTheNewSignature() {
        val pending = PendingResponse<String>()
        val sent = mutableListOf<String>()
        val vm = viewModel(sign = { sent += it; pending.await() })
        assertTrue(vm.submitSignature("新签名"))
        assertFalse(vm.submitSignature("新签名"))
        assertTrue(vm.sending)
        pending.complete("修改成功！")
        assertFalse(vm.sending)
        assertEquals(listOf("新签名"), sent)
        val result = vm.result!!
        assertTrue(result.success)
        assertEquals("修改成功！", result.message)
        assertEquals("新签名", result.value)
        vm.resultShown()
        assertNull(vm.result)
    }

    @Test fun failuresKeepTheReasonAndAllowRetry() {
        val vm = viewModel(sign = { throw IOException("签名过长") },
            avatar = { NgaWriteClient.ActionResult(false, "地址无效") })
        vm.submitSignature("很长的签名")
        assertEquals("签名过长", vm.result!!.message)
        assertFalse(vm.result!!.success)
        vm.resultShown()
        assertTrue(vm.submitAvatar("https://example.com/a.png"))
        assertEquals("地址无效", vm.result!!.message)
        assertFalse(vm.result!!.success)
    }

    @Test fun avatarSuccessUsesUnifiedMessage() {
        val vm = viewModel()
        vm.submitAvatar("https://example.com/a.png")
        assertEquals("操作成功", vm.result!!.message)
        assertEquals("https://example.com/a.png", vm.result!!.value)
    }

    @Test fun avatarAddressMustBeHttp() {
        assertTrue(AccountEditViewModel.isAvatarUrl("https://img.nga.cn/a.png"))
        assertTrue(AccountEditViewModel.isAvatarUrl(" http://a.cn/b "))
        assertFalse(AccountEditViewModel.isAvatarUrl("ftp://a.cn/b"))
        assertFalse(AccountEditViewModel.isAvatarUrl("https://"))
        assertFalse(AccountEditViewModel.isAvatarUrl(""))
    }
}
