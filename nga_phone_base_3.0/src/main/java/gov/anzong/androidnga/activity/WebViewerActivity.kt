package gov.anzong.androidnga.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.WebPageScreen
import sp.phone.view.webview.WebViewClientEx

/** 查看文档或图片等内容；extras 中的 "path" 为要打开的地址（如关于页中的说明文档）。 */
class WebViewerActivity : BaseActivity() {

    private lateinit var webView: WebView
    private var loadProgress by mutableIntStateOf(100)

    override fun usesComposeContent() = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("path").orEmpty()
        webView = WebView(this).apply {
            webViewClient = WebViewClientEx(this@WebViewerActivity)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) { loadProgress = newProgress }
            }
            settings.setSupportZoom(true)
            settings.javaScriptEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.loadWithOverviewMode = true
        }
        webView.loadUrl(path)
        setContent {
            NgaTheme { WebPageScreen("查看内容", loadProgress, webView, onRefresh = { webView.loadUrl(path) }, onBack = ::finish) }
        }
    }

    override fun onStop() {
        webView.stopLoading()
        super.onStop()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
