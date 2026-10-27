package gov.anzong.androidnga.coroutines

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import gov.anzong.androidnga.http.OnHttpCallBack
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/** 后台调度器；测试中替换为测试调度器。 */
object NgaDispatchers {
    @Volatile
    var io: CoroutineDispatcher = Dispatchers.IO
        @VisibleForTesting set
}

/** 不随页面取消的请求所用的作用域，对应原先未绑定生命周期的 RxJava 订阅。 */
object AppScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

/** 有页面时随页面销毁取消，没有时使用 [AppScope]。 */
fun scopeOf(owner: LifecycleOwner?): CoroutineScope = owner?.lifecycleScope ?: AppScope

/**
 * 在作用域中执行请求：[block] 在 [NgaDispatchers.io] 中执行，结果在主线程回调。
 * 作用域取消后不再回调；其他异常交给 [onError]。
 */
fun <T> CoroutineScope.launchWithCallback(
    onSuccess: (T) -> Unit,
    onError: (Throwable) -> Unit,
    block: suspend () -> T,
): Job = launch(Dispatchers.Main.immediate) {
    val result = try {
        withContext(NgaDispatchers.io) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        onError(e)
        return@launch
    }
    onSuccess(result)
}

/** 回调 Java 调用方：失败时调用 `onError(throwable.getMessage())`，与原 RxJava 订阅一致。 */
fun <T> CoroutineScope.launchWithCallback(callback: OnHttpCallBack<T>, block: suspend () -> T): Job =
    launchWithCallback({ callback.onSuccess(it) }, { callback.onError(it.message) }, block)
