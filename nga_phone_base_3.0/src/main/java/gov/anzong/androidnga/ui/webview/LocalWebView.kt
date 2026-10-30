package gov.anzong.androidnga.ui.webview

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.webkit.DownloadListener
import android.webkit.JavascriptInterface
import gov.anzong.androidnga.common.util.LogUtils
import gov.anzong.androidnga.common.view.WebViewEx
import gov.anzong.androidnga.settings.PhoneConfiguration

/**
 * 显示正文的 WebView：链接交给 [WebViewClientEx]，下载交给浏览器，相同内容不重复加载。
 * @author Justwen
 */
open class LocalWebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    WebViewEx(context, attrs), DownloadListener {

    val webViewClientEx = WebViewClientEx()

    private val emotionSize = PhoneConfiguration.getInstance().emoticonSize.toString() + "px"

    private var contentData: String? = null

    init {
        setDownloadListener(this)
        try {
            webViewClient = webViewClientEx
            settings.javaScriptEnabled = true
            addJavascriptInterface(this, "action")
            settings.textZoom = PhoneConfiguration.getInstance().webViewTextZoom
            settings.blockNetworkImage = true
            isFocusableInTouchMode = false
            isFocusable = false
            isLongClickable = false
            setBackgroundColor(Color.TRANSPARENT)
        } catch (e: Exception) {
            // 某些机型的WebView不支持以上方法的调用
        }
        isVerticalScrollBarEnabled = false
    }

    override fun onDownloadStart(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
    ) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        context.startActivity(intent)
    }

    /** 复用正文容器时允许同一文档重新绑定，避免中断加载后被旧缓存跳过。 */
    fun clearContentCache() {
        contentData = null
    }

    override fun loadDataWithBaseURL(
        baseUrl: String?,
        data: String,
        mimeType: String?,
        encoding: String?,
        historyUrl: String?,
    ) {
        if (data == contentData) {
            LogUtils.d("Data is not changed, ignore this update")
            return
        }
        contentData = data
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    @JavascriptInterface
    fun getEmotionSize(): String = emotionSize
}
