package gov.anzong.androidnga.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.WebPageScreen
import gov.anzong.androidnga.util.FunctionUtils

/** 应用内打开网页；extras 为 "url" 与可选的 "title"（未提供时显示网页标题）。返回键先回退网页历史。 */
class WebViewActivity : BaseActivity() {

    private lateinit var webView: WebView
    private var pageTitle by mutableStateOf("")
    private var loadProgress by mutableIntStateOf(100)

    override fun usesComposeContent() = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val fixedTitle = intent.getStringExtra("title")?.takeIf { it.isNotBlank() }
        pageTitle = fixedTitle.orEmpty()
        webView = WebView(this).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    view.loadUrl(request.url.toString())
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) { loadProgress = newProgress }
                override fun onReceivedTitle(view: WebView, title: String?) {
                    if (fixedTitle == null && !title.isNullOrBlank()) pageTitle = title
                }
            }
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.loadWithOverviewMode = true
            settings.textZoom = 100
            settings.setSupportZoom(true)
            settings.useWideViewPort = true
        }
        if (savedInstanceState?.let { webView.restoreState(it) } == null) intent.getStringExtra("url")?.let(webView::loadUrl)
        setContent {
            NgaTheme {
                WebPageScreen(pageTitle, loadProgress, webView, onRefresh = webView::reload, onBack = ::finish, menu = listOf(
                    "复制链接" to { FunctionUtils.copyToClipboard(this, currentUrl()) },
                    "通过浏览器打开" to { FunctionUtils.openUrlByDefaultBrowser(this, currentUrl()) },
                ))
            }
        }
    }

    private fun currentUrl() = webView.url ?: intent.getStringExtra("url").orEmpty()

    override fun handleBackNavigation(): Boolean {
        if (!webView.canGoBack()) return false
        webView.goBack()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
