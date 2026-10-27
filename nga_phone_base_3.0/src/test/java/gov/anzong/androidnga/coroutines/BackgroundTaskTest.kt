package gov.anzong.androidnga.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundTaskTest {

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

    @Test fun workRunsInBackgroundThenResultIsDelivered() = runTest(main) {
        val events = mutableListOf<String>()
        BackgroundTask.run({ events += "work"; "响应" }, { events += "result:$it" })
        // 与 AsyncTask.execute 一样立即返回，不在调用方同步执行
        assertEquals(emptyList<String>(), events)
        advanceUntilIdle()
        assertEquals(listOf("work", "result:响应"), events)
    }

    @Test fun nullResultIsDelivered() = runTest(main) {
        var delivered = false
        var value: String? = "未回调"
        BackgroundTask.run<String?>({ null }, { delivered = true; value = it })
        advanceUntilIdle()
        assertEquals(true, delivered)
        assertEquals(null, value)
    }

    @Test fun tasksDoNotWaitForEachOther() = runTest(main) {
        val events = mutableListOf<String>()
        BackgroundTask.run({ events += "work1"; 1 }, { events += "result1" })
        BackgroundTask.run({ events += "work2"; 2 }, { events += "result2" })
        runCurrent()
        assertEquals(listOf("work1", "work2", "result1", "result2"), events)
    }
}
