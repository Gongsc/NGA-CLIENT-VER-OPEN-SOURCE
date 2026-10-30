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
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.network.post.ForumActionClient
import gov.anzong.androidnga.network.post.NgaWriteClient
import gov.anzong.androidnga.mvvm.model.param.SignPostParam
import gov.anzong.androidnga.task.SignPostTask
import gov.anzong.androidnga.task.requestProfile

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
    /** 上传图片并设为头像，参数为图片的 data URL，返回论坛提示；失败时抛出带原因的异常。 */
    private val avatarUploadRequest: suspend (String) -> String = { image ->
        ForumActionClient.create().uploadAvatar(UserManagerImpl.getInstance().userId.orEmpty(), image)
    },
    private val scope: CoroutineScope = AppScope,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
    /** 读取本人资料中的最新头像；修改头像成功后用它更新首页底栏与“我的”页面的账号头像。 */
    private val ownAvatar: suspend () -> String? = {
        UserManagerImpl.getInstance().userId?.takeIf { it.isNotEmpty() }?.let { requestProfile("uid=$it").avatarUrl }
    },
    private val saveOwnAvatar: (String) -> Unit = { url -> UserManagerImpl.getInstance().let { it.updateAvatarUrl(it.userId, url) } },
) : ViewModel() {

    /** 提交结果；[value] 为提交的签名或头像地址，成功时返回给上一页。 */
    class Result(val success: Boolean, val message: String, val value: String)

    var sending by mutableStateOf(false); private set
    /** 未处理的提交结果，处理后调用 [resultShown]。 */
    var result by mutableStateOf<Result?>(null); private set

    /** 开始提交签名；已有提交进行中时返回 false。 */
    fun submitSignature(sign: String) = submit(sign) { Result(true, signRequest(sign), sign) }

    /** 开始提交头像地址；已有提交进行中时返回 false。 */
    fun submitAvatar(url: String) = submit(url, avatar = true) {
        val response = avatarRequest(url)
        Result(response.success, if (response.success) "操作成功" else response.message, url)
    }

    /** 读取并上传图片后设为头像；[image] 在后台线程读取图片并返回 data URL。已有提交进行中时返回 false。 */
    fun uploadAvatar(image: () -> String) = submit("", avatar = true) { Result(true, avatarUploadRequest(image()), "") }

    fun resultShown() {
        result = null
    }

    private fun submit(value: String, avatar: Boolean = false, request: suspend () -> Result): Boolean {
        if (sending) return false
        sending = true
        scope.launch {
            val outcome = try {
                withContext(worker) { request() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result(false, e.message ?: "提交失败，请稍后重试", value)
            }
            // 先写回账号头像再公布结果，返回首页时即显示新头像；读取失败不影响修改结果。
            if (outcome.success && avatar) {
                try {
                    withContext(worker) { ownAvatar() }?.takeIf { it.isNotBlank() }?.let(saveOwnAvatar)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // 忽略
                }
            }
            result = outcome
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
