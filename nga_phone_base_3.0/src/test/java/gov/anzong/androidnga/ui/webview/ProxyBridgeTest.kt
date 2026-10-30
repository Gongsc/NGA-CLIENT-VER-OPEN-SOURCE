package gov.anzong.androidnga.ui.webview

import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import gov.anzong.androidnga.network.post.NgaWriteClient.ActionResult

/** 投票提交：进度回调、结果文字与提交中忽略重复调用；不访问真实论坛。 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProxyBridgeTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        NgaDispatchers.io = dispatcher
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        NgaDispatchers.io = Dispatchers.IO
    }

    @Test fun votesOnceWhileSubmittingAndReportsResult() {
        val worker = StandardTestDispatcher(dispatcher.scheduler)
        NgaDispatchers.io = worker
        val events = mutableListOf<String>()
        val queries = mutableListOf<String>()
        val bridge = ProxyBridge({ events += "submitting:$it" }, { events += "result:$it" }) { query ->
            queries += query
            ActionResult(true, "操作成功 已投票")
        }
        bridge.postURL("__lib=vote&__act=vote&tid=1&voteid=2")
        bridge.postURL("__lib=vote&__act=vote&tid=1&voteid=3")
        assertEquals(listOf("submitting:true"), events)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("__lib=vote&__act=vote&tid=1&voteid=2"), queries)
        assertEquals(listOf("submitting:true", "submitting:false", "result:操作成功"), events)
    }

    @Test fun failureAndExceptionShowReason() {
        val events = mutableListOf<String>()
        ProxyBridge({}, { events += it }) { ActionResult(false, "你已经投过票了") }.postURL("q")
        ProxyBridge({}, { events += it }) { throw IllegalStateException("网络不可用") }.postURL("q")
        ProxyBridge({}, { events += it }) { ActionResult(false, "") }.postURL("q")
        assertEquals(listOf("你已经投过票了", "网络不可用", "未知错误,请重试"), events)
    }
}
