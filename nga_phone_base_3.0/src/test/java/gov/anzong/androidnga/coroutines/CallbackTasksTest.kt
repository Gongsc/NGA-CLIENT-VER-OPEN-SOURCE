package gov.anzong.androidnga.coroutines

import gov.anzong.androidnga.http.OnHttpCallBack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor

@OptIn(ExperimentalCoroutinesApi::class)
class CallbackTasksTest {

    private val main = StandardTestDispatcher(name = "main")
    private val io = StandardTestDispatcher(main.scheduler, name = "io")

    @Before fun setUp() {
        Dispatchers.setMain(main)
        NgaDispatchers.io = io
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        NgaDispatchers.io = Dispatchers.IO
    }

    private class Recorder<T> : OnHttpCallBack<T> {
        val events = mutableListOf<String>()
        override fun onSuccess(data: T) { events += "success:$data" }
        override fun onError(text: String?) { events += "error:$text" }
    }

    private fun TestScope.pageScope() = CoroutineScope(Job() + main)

    @Test fun successRunsOnIoAndCallsBackOnce() = runTest(main) {
        val recorder = Recorder<String>()
        var dispatcher: Any? = null
        pageScope().launchWithCallback(recorder) {
            dispatcher = currentCoroutineContext()[ContinuationInterceptor]
            "响应"
        }
        advanceUntilIdle()
        assertSame(io, dispatcher)
        assertEquals(listOf("success:响应"), recorder.events)
    }

    @Test fun failurePassesThrowableMessage() = runTest(main) {
        val recorder = Recorder<String>()
        pageScope().launchWithCallback(recorder) { throw IllegalStateException("网络错误") }
        pageScope().launchWithCallback(recorder) { throw IllegalStateException() }
        advanceUntilIdle()
        assertEquals(listOf("error:网络错误", "error:null"), recorder.events)
    }

    @Test fun cancelledScopeNeverCallsBack() = runTest(main) {
        val recorder = Recorder<String>()
        val scope = pageScope()
        scope.launchWithCallback(recorder) {
            delay(1_000)
            "过期响应"
        }
        runCurrent() // 已在 IO 中开始，尚未返回
        scope.cancel()
        advanceUntilIdle()
        assertTrue(recorder.events.isEmpty())
    }

    @Test fun lambdaVariantReceivesThrowable() = runTest(main) {
        val errors = mutableListOf<Throwable>()
        val failure = IllegalArgumentException("格式错误")
        pageScope().launchWithCallback<String>({ error("不应成功") }, { errors += it }) { throw failure }
        advanceUntilIdle()
        // 调试模式下协程会复制异常以补全堆栈，比较类型与消息。
        assertEquals(1, errors.size)
        assertSame(failure.javaClass, errors.single().javaClass)
        assertEquals("格式错误", errors.single().message)
    }
}
