package gov.anzong.androidnga.ui.webview

import android.content.Context
import android.webkit.WebView
import gov.anzong.androidnga.ui.compose.isNgaLoginUrl

/** 为官方登录页的嵌套表单提供确定的可视高度。 */
class LoginWebView(context: Context) : WebView(context) {
    fun fitLoginFrame() {
        if (!isNgaLoginUrl(url.orEmpty())) return
        evaluateJavascript(FIT_LOGIN_FRAME, null)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width > 0 && height > 0) post { fitLoginFrame() }
    }

    private companion object {
        // 部分 WebView 中，官方页面的百分比高度和 vh 会解析为 0；使用实际视口高度。
        val FIT_LOGIN_FRAME = """
            (function() {
                function fit() {
                    var frame = document.getElementById('iff');
                    var height = window.innerHeight;
                    if (!frame || !document.body || height <= 0) return;
                    document.documentElement.style.setProperty('height', height + 'px', 'important');
                    document.body.style.setProperty('height', height + 'px', 'important');
                    frame.style.setProperty('height', height + 'px', 'important');
                    frame.style.setProperty('min-height', height + 'px', 'important');
                }
                if (!window.__ngaLoginFit) {
                    window.__ngaLoginFit = fit;
                    window.addEventListener('resize', fit);
                }
                window.__ngaLoginFit();
            })();
        """.trimIndent()
    }
}
