package gov.anzong.androidnga.mvvm.viewmodel

import gov.anzong.androidnga.coroutines.PendingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import sp.phone.http.bean.ProfileData
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val main = UnconfinedTestDispatcher()
    @Before fun setMain() = Dispatchers.setMain(main)
    @After fun resetMain() = Dispatchers.resetMain()

    private fun profile(sign: String) = ProfileData().apply { uid = "1"; userName = "甲"; this.sign = sign }

    @Test fun queryPrefersUidAndEncodesUserNamesAsGbk() {
        assertEquals("uid=42", ProfileViewModel.profileQuery("42", "甲"))
        assertEquals("uid=7", ProfileViewModel.profileQuery(null, "UID7"))
        assertEquals("username=%BC%D7", ProfileViewModel.profileQuery(null, "甲"))
        assertEquals("username=UIDabc", ProfileViewModel.profileQuery("", "UIDabc"))
        assertNull(ProfileViewModel.profileQuery(null, " "))
    }

    @Test fun moneyAndFameFormatting() {
        assertEquals(Triple(12L, 34L, 56L), ProfileViewModel.money("123456"))
        assertEquals(Triple(0L, 0L, 0L), ProfileViewModel.money("abc"))
        assertEquals("1.5", ProfileViewModel.fame("15"))
        assertEquals("未知", ProfileViewModel.fame("未知"))
    }

    @Test fun startLoadsOnceAndSignatureCanBeUpdated() {
        val pending = PendingResponse<ProfileData>()
        val queries = mutableListOf<String>()
        val vm = ProfileViewModel("uid=1", { queries += it; pending.await() }, main)
        vm.start(); vm.start()
        assertTrue(vm.loading)
        pending.complete(profile("旧签名"))
        assertFalse(vm.loading)
        assertEquals(listOf("uid=1"), queries)
        assertEquals("旧签名", vm.signature)
        vm.updateSignature("新签名")
        assertEquals("新签名", vm.signature)
        assertEquals("新签名", vm.profile!!.sign)
    }

    @Test fun failureKeepsReasonAndRefreshRetries() {
        var fail = true
        val vm = ProfileViewModel("uid=1", { if (fail) throw IOException("网络错误") else profile("") }, main)
        vm.start()
        assertEquals("网络错误", vm.error)
        assertNull(vm.profile)
        fail = false
        vm.refresh()
        assertNull(vm.error)
        assertNotNull(vm.profile)
    }

    @Test fun missingParametersReportAnError() {
        val vm = ProfileViewModel(null, { error("不应请求") }, main)
        vm.start()
        assertEquals("缺少用户参数", vm.error)
        assertFalse(vm.loading)
    }
}
