package gov.anzong.androidnga.mvvm.model

import android.net.Uri
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.util.StreamUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import gov.anzong.androidnga.network.post.TopicPostClient
import gov.anzong.androidnga.network.retrofit.NgaApi
import gov.anzong.androidnga.network.retrofit.RetrofitHelper
import gov.anzong.androidnga.mvvm.model.param.ParamKey
import gov.anzong.androidnga.mvvm.model.param.PostParam
import gov.anzong.androidnga.util.FunctionUtils
import gov.anzong.androidnga.util.ImageUtils
import gov.anzong.androidnga.util.NLog
import java.util.concurrent.TimeUnit

/** 发帖页的请求；实现中含阻塞调用，由调用方切到后台线程执行。 */
interface TopicPostRepository {

    /** 预检取得提交与上传附件所需的 auth。 */
    suspend fun prepare(param: PostParam): String?

    suspend fun loadCategories(fid: Int): List<String>

    /** 提交 [param]，返回服务器提示；失败时抛出异常，消息为失败原因。 */
    suspend fun submit(param: PostParam): String

    /** 上传图片附件。 */
    suspend fun upload(image: ImageFile, param: PostParam): UploadResult
}

class ImageFile(val name: String, val bytes: ByteArray)

/** 待上传的图片，在后台线程读取；[compress] 为 true 时缩小 1MB 以上的图片。 */
fun interface UploadImage {
    fun read(compress: Boolean): ImageFile
}

/** 系统文件选择器返回的图片。 */
class ContentUploadImage(private val uri: Uri) : UploadImage {
    override fun read(compress: Boolean): ImageFile {
        val context = ContextUtils.getContext()
        val cr = context.contentResolver
        val contentType = cr.getType(uri)
        if (contentType.isNullOrEmpty()) throw IllegalArgumentException(context.getString(R.string.invalid_img_selected))
        val fileSize = cr.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        val bytes = if (compress && fileSize >= 1024 * 1024) {
            ImageUtils.fitImageToUpload(cr.openInputStream(uri), cr.openInputStream(uri))
        } else {
            requireNotNull(cr.openInputStream(uri)).use { StreamUtils.toByteArray(it) }
        }
        return ImageFile(contentType.replace('/', '.'), bytes)
    }
}

sealed interface UploadResult {
    data class Uploaded(val url: String, val attachments: String, val attachmentsCheck: String) : UploadResult

    /** 附件超过服务器限制（error_code 9）。 */
    data object TooLarge : UploadResult
}

/** 解析附件服务器响应；无法识别时抛出异常。 */
fun parseUploadResponse(response: String): UploadResult {
    val obj = checkNotNull(NgaJson.parseObject(response.replace("window.script_muti_get_var_store=", ""))) { "上传图片失败" }
    if (obj.containsKey("error_code") && obj.getInteger("error_code") == 9) return UploadResult.TooLarge
    val data = obj.getJSONObject("data") ?: throw IllegalStateException("上传图片失败，请尝试更换域名后重试")
    return UploadResult.Uploaded(
        data.getString("url").orEmpty(),
        data.getString("attachments").orEmpty(),
        data.getString("attachments_check").orEmpty(),
    )
}

/**
 * 发帖、回复与编辑经 [TopicPostClient] 预检取得 auth 后提交；附件上传使用连接超时 5 分钟的独立实例。
 */
class NgaTopicPostRepository : TopicPostRepository {

    private val api: NgaApi by lazy {
        val builder = RetrofitHelper.getInstance().createOkHttpClientBuilder()
        builder.connectTimeout(5, TimeUnit.MINUTES)
        RetrofitHelper.getInstance().createRetrofit(builder).create(NgaApi::class.java)
    }

    private val postClient by lazy { TopicPostClient.create() }

    override suspend fun prepare(param: PostParam): String? = postClient.prepare(param)

    override suspend fun loadCategories(fid: Int): List<String> {
        val map = mapOf("__lib" to "topic_key", "__act" to "get", ParamKey.KEY_FID to fid.toString(), "__output" to "8")
        val obj = NgaJson.parseObject(api.get(map))!!.getJSONObject("data")!!.getJSONObject("0")!!
        return generateSequence(0) { it + 1 }
            .takeWhile { obj.containsKey(it.toString()) }
            .map { obj.getJSONObject(it.toString())!!.getString("0").orEmpty() }
            .toList()
    }

    override suspend fun submit(param: PostParam): String {
        param.postContent = param.postContent?.let(FunctionUtils::ColorTxtCheck)
        return postClient.submit(param)
    }

    override suspend fun upload(image: ImageFile, param: PostParam): UploadResult {
        val response = api.uploadFile(ATTACHMENT_SERVER, buildMultipartBody(image.name, image.bytes, param)).use { it.string() }
        return try {
            parseUploadResponse(response)
        } catch (e: Exception) {
            NLog.e("exception occur while uploading file $response")
            throw IllegalStateException("上传图片失败，请尝试更换域名后重试")
        }
    }

    private fun buildMultipartBody(fileName: String, bytes: ByteArray, param: PostParam): MultipartBody =
        MultipartBody.Builder()
            .setType("multipart/form-data".toMediaType())
            .addPart(MultipartBody.Part.createFormData("attachment_file1", fileName, bytes.toRequestBody("image/jpeg".toMediaType())))
            .addFormDataPart("attachment_file1_url_utf8_name", fileName)
            .addFormDataPart("fid", param.postFid.toString())
            .addFormDataPart("auth", param.authCode.orEmpty())
            .addFormDataPart("func", "upload")
            .addFormDataPart("v2", "1")
            .addFormDataPart("lite", "js")
            // 1 为自动缩图
            .addFormDataPart("attachment_file1_auto_size", "")
            // 水印位置tl/tr/bl/br 左上右上左下右下 不设为无水印
            .addFormDataPart("attachment_file1_watermark", "")
            .addFormDataPart("attachment_file1_dscp", "")
            .addFormDataPart("attachment_file1_img", "1")
            .addFormDataPart("origin_domain", "bbs.ngacn.cc")
            .build()

    private companion object {
        const val ATTACHMENT_SERVER = "https://img8.nga.cn/attach.php?"
    }
}
