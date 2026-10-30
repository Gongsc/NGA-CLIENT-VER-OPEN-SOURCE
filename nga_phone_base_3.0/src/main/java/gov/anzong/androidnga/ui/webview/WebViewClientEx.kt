package gov.anzong.androidnga.ui.webview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import gov.anzong.androidnga.activity.ArticleListActivity
import gov.anzong.androidnga.activity.TopicListActivity
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.base.util.javaTrim
import gov.anzong.androidnga.gallery.ImageZoomActivity
import gov.anzong.androidnga.util.ForumUtils

/** 正文中的论坛链接在应用内打开，图片进入图片浏览，其他地址交给系统。 */
class WebViewClientEx() : WebViewClient() {

    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context?) : this()

    private var imgUrlList: List<String>? = null

    fun setImgUrls(list: List<String>?) {
        imgUrlList = list
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        request.isForMainFrame && shouldOverrideUrlLoading(view, request.url.toString())

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
        if (url == null || url.javaTrim().startsWith("#")) return false
        // 阻止脚本、自定义协议等地址通过论坛正文启动其他应用。
        val link = ForumLink.resolve(url, view.url, ForumUtils.getAvailableDomain()) ?: return true
        val context = view.context
        val resolved = link.url.toString()
        val intent = when (link.target) {
            ForumLink.Target.ARTICLE -> Intent(context, ArticleListActivity::class.java)
                .setData(resolved.toUri())
                .putExtra("fromreplyactivity", 1)
            ForumLink.Target.FORUM -> Intent(context, TopicListActivity::class.java).setData(resolved.toUri())
            ForumLink.Target.PROFILE -> {
                AppNavigator.getInstance()
                    .build(ARouterConstants.ACTIVITY_PROFILE)
                    .withString("mode", "username")
                    .withString("username", link.url.queryParameter("username"))
                    .navigation(context)
                return true
            }
            ForumLink.Target.IMAGE -> {
                val images = ArrayList(imgUrlList.orEmpty())
                if (!images.contains(resolved)) images.add(resolved)
                Intent(context, ImageZoomActivity::class.java)
                    .putExtra(ImageZoomActivity.KEY_GALLERY_URLS, images.toTypedArray())
                    .putExtra(ImageZoomActivity.KEY_GALLERY_CUR_URL, resolved)
            }
            ForumLink.Target.EXTERNAL -> Intent(Intent.ACTION_VIEW, resolved.toUri())
        }
        try {
            context.startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
            // 用户设备可能没有可处理外部链接的应用。
        }
        return true
    }

    override fun onPageFinished(view: WebView, url: String?) {
        view.settings.blockNetworkImage = false
        super.onPageFinished(view, url)
    }
}
