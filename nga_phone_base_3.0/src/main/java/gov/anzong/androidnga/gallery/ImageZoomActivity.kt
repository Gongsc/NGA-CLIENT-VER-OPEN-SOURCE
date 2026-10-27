package gov.anzong.androidnga.gallery

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.content.FileProvider
import gov.anzong.androidnga.BuildConfig
import gov.anzong.androidnga.R
import gov.anzong.androidnga.activity.BaseActivity
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.ui.compose.NgaTheme
import java.io.File

/** 图片浏览页，界面见 [GalleryScreen]。 */
class ImageZoomActivity : BaseActivity() {

    private val saveTask by lazy { SaveImageTask() }

    /** 已下载到本地的图片，分享时复用。 */
    private val downloads = mutableMapOf<String, File>()

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val (urls, initialPage) = galleryArguments(
            intent.getStringArrayExtra(KEY_GALLERY_URLS), intent.getStringExtra(KEY_GALLERY_CUR_URL))
        if (urls.isEmpty()) {
            finish()
            return
        }
        setContent {
            NgaTheme {
                GalleryScreen(
                    urls = urls,
                    initialPage = initialPage,
                    onBack = ::finish,
                    onDownload = { save(it) },
                    onShare = { url -> downloads[url]?.let(::share) ?: save(url) { share(it) } },
                    onDownloadAll = { save(*urls.toTypedArray()) },
                )
            }
        }
    }

    private fun save(vararg urls: String, then: (File) -> Unit = {}) {
        saveTask.execute({ result ->
            downloads[result.url] = result.file
            then(result.file)
        }, *urls)
    }

    private fun share(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file)
            val intent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .setType("image/jpeg")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: ActivityNotFoundException) {
            ToastUtils.error("分享失败")
        }
    }

    companion object {
        const val KEY_GALLERY_URLS = "keyGalleryUrl"
        const val KEY_GALLERY_CUR_URL = "keyGalleryCurUrl"
    }
}

/** 解析启动参数：只传当前图片时浏览单张；当前图片不在列表中时从第一张开始。 */
internal fun galleryArguments(urls: Array<String>?, current: String?): Pair<List<String>, Int> {
    val list = urls?.filterNotNull() ?: listOfNotNull(current)
    return list to list.indexOf(current).coerceAtLeast(0)
}
