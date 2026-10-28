package gov.anzong.androidnga.coroutines

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.function.Consumer

/**
 * 页面间事件，替代原 RxBus。只投递给已订阅的页面，不保留历史事件；订阅随页面销毁结束。
 */
object UiEventBus {

    private val events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 64)

    val flow: SharedFlow<UiEvent> = events.asSharedFlow()

    @JvmStatic
    fun post(event: UiEvent) {
        events.tryEmit(event)
    }

    /** 在主线程接收事件，直到 [owner] 销毁。 */
    @JvmStatic
    fun observe(owner: LifecycleOwner, consumer: Consumer<UiEvent>) {
        owner.lifecycleScope.launch { events.collect { consumer.accept(it) } }
    }
}
