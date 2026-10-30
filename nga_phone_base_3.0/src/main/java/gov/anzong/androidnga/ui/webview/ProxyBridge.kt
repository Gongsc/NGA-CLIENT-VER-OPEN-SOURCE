package gov.anzong.androidnga.ui.webview

import android.webkit.JavascriptInterface
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import gov.anzong.androidnga.network.post.ForumActionClient
import gov.anzong.androidnga.network.post.NgaWriteClient.ActionResult

/**
 * 投票网页脚本的提交入口（`window.ProxyBridge.postURL`），只接受投票，见 [ForumActionClient.vote]。
 *
 * 提交在 [AppScope] 中执行，离开页面也不会中途放弃；提交中再次调用会被忽略。
 * [onSubmitting] 与 [onResult] 在主线程回调，[onResult] 的参数为可直接提示的文字。
 */
class ProxyBridge(
    private val onSubmitting: (Boolean) -> Unit,
    private val onResult: (String) -> Unit,
    private val vote: (String) -> ActionResult = { ForumActionClient.create().vote(it) },
) {
    private var submitting = false

    @JavascriptInterface
    fun postURL(query: String) {
        AppScope.launch(Dispatchers.Main.immediate) {
            if (submitting) return@launch
            submitting = true
            onSubmitting(true)
            val result = try {
                withContext(NgaDispatchers.io) { vote(query) }
            } catch (e: Exception) {
                ActionResult(false, e.message.orEmpty())
            } finally {
                submitting = false
                onSubmitting(false)
            }
            onResult(message(result))
        }
    }

    companion object {
        /** 论坛的成功提示（如“操作成功 …”）统一为“操作成功”；没有提示时按结果给出默认文字。 */
        internal fun message(result: ActionResult): String = when {
            result.message.isBlank() -> if (result.success) "操作成功" else "未知错误,请重试"
            result.message.startsWith("操作成功") -> "操作成功"
            else -> result.message
        }
    }
}
