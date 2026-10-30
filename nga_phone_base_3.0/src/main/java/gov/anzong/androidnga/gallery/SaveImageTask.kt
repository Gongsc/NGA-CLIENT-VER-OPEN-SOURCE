package gov.anzong.androidnga.gallery

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import gov.anzong.androidnga.util.SharedStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import gov.anzong.androidnga.network.OnSimpleHttpCallBack
import gov.anzong.androidnga.util.ActivityUtils
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/** 依次下载图片并保存到相册 NGA 文件夹；下载中再次调用会提示并忽略，任一张失败即停止。 */
class SaveImageTask {

    private val context: Context = ContextUtils.getContext()
    private var job: Job? = null

    class DownloadResult(val file: File, val url: String) {
        var uri: Uri? = null
    }

    fun execute(callBack: OnSimpleHttpCallBack<DownloadResult>, vararg urls: String) {
        if (job?.isActive == true) {
            ActivityUtils.showToast("图片正在下载，防止风怒！！")
            return
        }
        job = AppScope.launch(Dispatchers.Main.immediate) {
            try {
                urls.forEachIndexed { index, url ->
                    val result = withContext(NgaDispatchers.io) { download(url) }
                    if (index == urls.size - 1) {
                        ActivityUtils.showToast(if (urls.size > 1) "所有图片已保存" else "图片已保存至相册 NGA 文件夹")
                    }
                    callBack.onResult(result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ActivityUtils.showToast("下载失败")
            }
        }
    }

    private fun download(url: String): DownloadResult {
        val file = Glide.with(context).downloadOnly().load(url).submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get()
        val result = DownloadResult(file, url)
        val extension = MimeTypeMap.getFileExtensionFromUrl(url).ifEmpty { "jpg" }
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT)) ?: "image/jpeg"
        val name = "nga_" + UUID.randomUUID() + "." + extension
        result.uri = SharedStorage.publish(
            context, file, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            Environment.DIRECTORY_PICTURES + "/NGA", name, mimeType,
        )
        return result
    }
}
