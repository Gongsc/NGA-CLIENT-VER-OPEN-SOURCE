package gov.anzong.androidnga.mvvm.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import sp.phone.common.UserManagerImpl
import sp.phone.http.post.ForumActionClient
import sp.phone.http.post.NgaWriteClient
import sp.phone.param.SignPostParam
import sp.phone.task.SignPostTask

/**
 * 修改签名与头像：同一时间只提交一次。
 *
 * 提交在 [AppScope] 中执行，离开页面也不会中途放弃；结果保存在 ViewModel 中，页面重建后仍能收到。
 */
class AccountEditViewModel(
    private val signRequest: suspend (String) -> String = { sign ->
        SignPostTask().submit(SignPostParam().apply { uid = UserManagerImpl.getInstance().userId; this.sign = sign })
    },
    private val avatarRequest: suspend (String) -> NgaWriteClient.ActionResult = { ForumActionClient.create().setAvatar(it) },
    private val scope: CoroutineScope = AppScope,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
) : ViewModel() {

    /** 提交结果；[value] 为提交的签名或头像地址，成功时返回给上一页。 */
    class Result(val success: Boolean, val message: String, val value: String)

    var sending by mutableStateOf(false); private set
    /** 未处理的提交结果，处理后调用 [resultShown]。 */
    var result by mutableStateOf<Result?>(null); private set

    /** 开始提交签名；已有提交进行中时返回 false。 */
    fun submitSignature(sign: String) = submit(sign) { Result(true, signRequest(sign), sign) }

    /** 开始提交头像地址；已有提交进行中时返回 false。 */
    fun submitAvatar(url: String) = submit(url) {
        val response = avatarRequest(url)
        Result(response.success, if (response.success) "操作成功" else response.message, url)
    }

    fun resultShown() {
        result = null
    }

    private fun submit(value: String, request: suspend () -> Result): Boolean {
        if (sending) return false
        sending = true
        scope.launch {
            result = try {
                withContext(worker) { request() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result(false, e.message ?: "提交失败，请稍后重试", value)
            }
            sending = false
        }
        return true
    }

    companion object {
        /** 头像只接受 http(s) 图片地址。 */
        @JvmStatic
        fun isAvatarUrl(url: String) = url.trim().let { it.startsWith("http://") || it.startsWith("https://") } && url.trim().length > 8
    }
}
