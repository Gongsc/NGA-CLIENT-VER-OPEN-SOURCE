package gov.anzong.androidnga.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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

    @Test fun serialWorkRunsInSubmissionOrderAndSurvivesFailures() {
        NgaDispatchers.io = Dispatchers.IO
        val done = java.util.concurrent.CountDownLatch(1)
        val events = java.util.Collections.synchronizedList(mutableListOf<Int>())
        for (i in 1..50) BackgroundTask.runSerially {
            if (i == 10) throw IllegalStateException("写入失败")
            events += i
        }
        BackgroundTask.runSerially { done.countDown() }
        org.junit.Assert.assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS))
        assertEquals((1..50).filter { it != 10 }, events.toList())
    }
}
