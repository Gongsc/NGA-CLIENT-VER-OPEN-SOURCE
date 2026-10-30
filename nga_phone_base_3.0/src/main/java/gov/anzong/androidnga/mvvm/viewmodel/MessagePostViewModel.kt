package gov.anzong.androidnga.mvvm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.network.post.MessagePostClient
import gov.anzong.androidnga.mvvm.model.param.MessagePostParam

/**
 * 发送或回复短消息：同一时间只提交一次。
 *
 * 提交在 [AppScope] 中执行，离开页面也不会中途放弃（写操作已发出后取消只会让结果不明）；
 * 结果保存在 ViewModel 中，页面重建后仍能收到。
 */
class MessagePostViewModel(
    private val param: MessagePostParam,
    private val submit: suspend (MessagePostParam) -> String = { MessagePostClient.create().send(it) },
    private val scope: CoroutineScope = AppScope,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
) : ViewModel() {

    /** 由 Fragment 的 arguments（"param"）创建。 */
    constructor(state: SavedStateHandle) : this(requireNotNull(state.get<MessagePostParam>("param")) { "缺少短消息参数" })

    /** 提交结果；[message] 为服务器返回的提示，失败时为错误原因。 */
    class Result(@JvmField val success: Boolean, @JvmField val message: String)

    private val _sending = MutableLiveData(false)
    private val _result = MutableLiveData<Result?>()

    val sending: LiveData<Boolean> get() = _sending
    /** 未处理的提交结果，处理后调用 [resultShown]。 */
    val result: LiveData<Result?> get() = _result

    /** 新消息需要填写收件人；回复沿用原会话。 */
    val isNewMessage get() = param.action == "new"

    val initialRecipient: String? get() = param.recipient

    /** 开始提交；已有提交进行中时返回 false。 */
    fun send(title: String, recipient: String, body: String): Boolean {
        if (_sending.value == true) return false
        _sending.value = true
        param.recipient = recipient
        param.postSubject = title
        param.postContent = body
        scope.launch {
            val result = try {
                Result(true, withContext(worker) { submit(param) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result(false, e.message ?: "发送失败，请稍后重试")
            }
            _result.value = result
            _sending.value = false
        }
        return true
    }

    fun resultShown() {
        _result.value = null
    }
}
