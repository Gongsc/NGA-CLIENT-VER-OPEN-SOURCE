package gov.anzong.androidnga.ui.compose

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import gov.anzong.androidnga.ui.webview.LocalWebView
import kotlin.math.ceil

/** 可由 LazyColumn 复用的正文容器，绑定回调始终对应当前楼层。 */
class ArticleContentWebView(context: Context) : LocalWebView(context) {
    private var document: String? = null
    private var heightChanged: ((Float) -> Unit)? = null
    @Volatile private var generation = 0
    private var lastHeightPixels = -1

    init {
        if (android.os.Build.VERSION.SDK_INT >= 33) settings.isAlgorithmicDarkeningAllowed = false
        settings.textZoom = 100
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        settings.minimumFontSize = 1
        settings.minimumLogicalFontSize = 1
        addJavascriptInterface(SizeBridge(), "composeSize")
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {
                if (progress == 100) installSizeObserver()
            }
        }
    }

    fun bind(html: String, images: List<String>, report: (Float) -> Unit) {
        heightChanged = report
        webViewClientEx.setImgUrls(images)
        if (document != html) {
            generation++
            lastHeightPixels = -1
            document = html
            loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        }
    }

    fun resetForReuse() {
        generation++
        heightChanged = null
        document = null
        lastHeightPixels = -1
        stopLoading()
        clearContentCache()
        webViewClientEx.setImgUrls(emptyList())
    }

    fun release() {
        resetForReuse()
        removeJavascriptInterface("composeSize")
        webChromeClient = null
        destroy()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && w != oldw) evaluateJavascript("window.__ngaScheduleSize && window.__ngaScheduleSize();", null)
    }

    private fun installSizeObserver() {
        val token = generation
        evaluateJavascript("""
            (function(){
                if(window.__ngaSizeToken === $token)return;
                window.__ngaSizeToken=$token;
                if(window.__ngaSizeObserver)window.__ngaSizeObserver.disconnect();
                var pending=false,lastHeight=-1,lastWidth=-1;
                function schedule(){
                    if(pending)return;pending=true;
                    setTimeout(function(){requestAnimationFrame(function(){
                        pending=false;
                        if(!document.body)return;
                        var h=Math.ceil(document.body.getBoundingClientRect().height+16),w=window.innerWidth;
                        if(h===lastHeight&&w===lastWidth)return;
                        lastHeight=h;lastWidth=w;
                        window.composeSize.reportHeight($token,h,w);
                    });},32);
                }
                window.__ngaScheduleSize=schedule;
                if(window.ResizeObserver){window.__ngaSizeObserver=new ResizeObserver(schedule);window.__ngaSizeObserver.observe(document.body);}
                document.addEventListener('load',schedule,true);
                window.addEventListener('resize',schedule);
                schedule();
            })();
        """.trimIndent(), null)
    }

    private inner class SizeBridge {
        @JavascriptInterface fun reportHeight(token: Int, cssHeight: Double, viewport: Double) {
            if (!cssHeight.isFinite() || cssHeight <= 0 || !viewport.isFinite() || viewport <= 0) return
            post {
                if (token != generation || width <= 0) return@post
                val pixels = ceil(cssHeight * width / viewport).toInt().coerceAtLeast(1)
                if (pixels == lastHeightPixels) return@post
                lastHeightPixels = pixels
                heightChanged?.invoke((pixels / resources.displayMetrics.density).coerceIn(1f, 100000f))
            }
        }
    }
}
