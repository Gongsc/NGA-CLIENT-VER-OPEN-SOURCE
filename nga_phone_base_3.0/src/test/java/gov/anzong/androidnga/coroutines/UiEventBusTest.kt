package gov.anzong.androidnga.coroutines

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import gov.anzong.androidnga.util.ThrottledClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UiEventBusTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private class Page : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.CREATED }
        override val lifecycle: Lifecycle get() = registry
    }

    @Test fun eventsReachLiveSubscribersOnlyWithoutReplay() {
        UiEventBus.post(UiEvent(UiEvent.EVENT_SHOW_TOPIC_LIST, "订阅前"))
        val first = Page()
        val second = Page()
        val a = mutableListOf<Any?>()
        val b = mutableListOf<Any?>()
        UiEventBus.observe(first) { a += it.obj }
        UiEventBus.observe(second) { b += it.obj }
        UiEventBus.post(UiEvent(UiEvent.EVENT_SHOW_TOPIC_LIST, "版块"))
        first.registry.currentState = Lifecycle.State.DESTROYED
        UiEventBus.post(UiEvent(UiEvent.EVENT_LOGIN_AUTH_CODE_UPDATE, "验证码"))
        assertEquals(listOf<Any?>("版块"), a)
        assertEquals(listOf<Any?>("版块", "验证码"), b)
        second.registry.currentState = Lifecycle.State.DESTROYED
    }

    @Test fun throttledClicksPassFirstThenIgnoreForOneSecond() {
        var now = 10_000L
        var clicks = 0
        val listener = ThrottledClickListener({ clicks++ }, 1000) { now }
        listener.onClick(null)
        now += 999
        listener.onClick(null)
        assertEquals(1, clicks)
        now += 1
        listener.onClick(null)
        assertEquals(2, clicks)
        listener.onClick(null)
        assertEquals(2, clicks)
    }
}
