package gov.anzong.androidnga.coroutines

import kotlinx.coroutines.CompletableDeferred
import kotlin.coroutines.cancellation.CancellationException

/** 测试用的未返回响应：由测试决定何时返回，并记录等待方是否已被取消。 */
class PendingResponse<T> {
    private val result = CompletableDeferred<T>()
    var cancelled = false
        private set

    suspend fun await(): T = try {
        result.await()
    } catch (e: CancellationException) {
        cancelled = true
        throw e
    }

    fun complete(value: T) {
        result.complete(value)
    }
}
