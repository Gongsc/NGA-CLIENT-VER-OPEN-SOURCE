package sp.phone.ui.fragment.dialog

import android.annotation.SuppressLint
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import gov.anzong.androidnga.base.util.ToastUtils
import sp.phone.common.PhoneConfiguration
import sp.phone.proxy.ProxyBridge
import gov.anzong.androidnga.ui.compose.NgaDialogLayout
import sp.phone.view.webview.WebViewClientEx

/**
 * 楼层中的投票/投注；arguments 中的 "html" 为投票网页。
 * 网页脚本经 [ProxyBridge] 提交投票，提交中在网页上方显示进度，结果以提示显示；脚本的 alert/confirm 以 Compose 对话框显示。
 */
class VoteDialogFragment : BaseDialogFragment() {

    private class JsPrompt(val message: String, val result: JsResult, val confirm: Boolean)

    private var prompt by mutableStateOf<JsPrompt?>(null)
    private var submitting by mutableStateOf(false)

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    override fun DialogContent() {
        NgaDialogLayout("投票/投注", buttons = { TextButton(onClick = { dismiss() }) { Text("关闭") } }) {
            if (submitting) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("正在提交…", style = MaterialTheme.typography.bodyMedium)
            }
            AndroidView(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 480.dp), factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(0)
                    isLongClickable = false
                    setOnLongClickListener { true }
                    settings.defaultFontSize = PhoneConfiguration.getInstance().webSize
                    settings.javaScriptEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    addJavascriptInterface(ProxyBridge({ submitting = it }, ToastUtils::show), "ProxyBridge")
                    webViewClient = WebViewClientEx(requireActivity())
                    webChromeClient = object : WebChromeClient() {
                        override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
                            prompt = JsPrompt(message.orEmpty(), result, confirm = false)
                            return true
                        }

                        override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
                            prompt = JsPrompt(message.orEmpty(), result, confirm = true)
                            return true
                        }
                    }
                    loadDataWithBaseURL(null, arguments?.getString("html").orEmpty(), "text/html", "utf-8", null)
                }
            }, onRelease = { it.destroy() })
        }
        prompt?.let { current ->
            fun answer(confirmed: Boolean) {
                prompt = null
                if (confirmed) current.result.confirm() else current.result.cancel()
            }
            AlertDialog(onDismissRequest = { answer(!current.confirm) }, text = { Text(current.message) },
                confirmButton = { TextButton(onClick = { answer(true) }) { Text("确定") } },
                dismissButton = if (current.confirm) ({ TextButton(onClick = { answer(false) }) { Text("取消") } }) else null)
        }
    }

    override fun onDestroyView() {
        prompt?.result?.cancel()
        prompt = null
        super.onDestroyView()
    }
}
