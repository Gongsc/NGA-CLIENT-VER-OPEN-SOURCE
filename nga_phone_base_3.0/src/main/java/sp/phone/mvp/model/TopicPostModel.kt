package sp.phone.mvp.model

import android.net.Uri
import gov.anzong.androidnga.R
import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.util.StreamUtils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.coroutines.scopeOf
import gov.anzong.androidnga.http.OnHttpCallBack
import gov.anzong.androidnga.base.util.ToastUtils
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import sp.phone.http.post.TopicPostClient
import sp.phone.http.retrofit.NgaApi
import sp.phone.http.retrofit.RetrofitHelper
import sp.phone.mvp.contract.TopicPostContract
import sp.phone.param.ParamKey
import sp.phone.param.PostParam
import sp.phone.util.ImageUtils
import sp.phone.util.NLog
import sp.phone.util.StringUtils
import java.util.concurrent.TimeUnit

/**
 * Created by Justwen on 2017/6/10.
 *
 * 发帖、回复与编辑经 [TopicPostClient] 预检取得 auth 后提交；附件上传使用连接超时 5 分钟的独立实例。
 * 附件上传随页面销毁取消，其余请求不随页面取消。
 */
class TopicPostModel : BaseModel(), TopicPostContract.Model {

    private val api: NgaApi = run {
        val builder = RetrofitHelper.getInstance().createOkHttpClientBuilder()
        builder.connectTimeout(5, TimeUnit.MINUTES)
        RetrofitHelper.getInstance().createRetrofit(builder).create(NgaApi::class.java)
    }

    private val postClient = TopicPostClient.create()

    override fun getPostInfo(postParam: PostParam, callBack: OnHttpCallBack<PostParam>) {
        AppScope.launchWithCallback({ param: PostParam -> callBack.onSuccess(param) },
            { callBack.onError(it.message ?: "无法获取发帖信息，请稍后重试") }) {
            postParam.authCode = postClient.prepare(postParam)
            postParam
        }
    }

    override fun loadTopicCategory(postParam: PostParam, callBack: OnHttpCallBack<List<String>>) {
        val map = mapOf(
            "__lib" to "topic_key", "__act" to "get",
            ParamKey.KEY_FID to postParam.postFid.toString(), "__output" to "8",
        )
        AppScope.launchWithCallback({ list: List<String> -> callBack.onSuccess(list) }, { callBack.onError(it.localizedMessage) }) {
            val obj = NgaJson.parseObject(api.get(map)).getJSONObject("data").getJSONObject("0")
            val ret = ArrayList<String>()
            var index = 0
            while (obj.containsKey(index.toString())) {
                ret.add(obj.getJSONObject(index.toString()).getString("0"))
                index++
            }
            ret
        }
    }

    override fun post(postParam: PostParam, callBack: OnHttpCallBack<String>) {
        AppScope.launchWithCallback({ message: String -> callBack.onSuccess(message) },
            { callBack.onError(it.message ?: "提交失败，请稍后重试") }) {
            postClient.submit(postParam)
        }
    }

    override fun uploadFile(uri: Uri, postParam: PostParam, callBack: OnHttpCallBack<String>) {
        uploadFile(uri, postParam, callBack, false)
    }

    private fun uploadFile(uri: Uri, postParam: PostParam, callBack: OnHttpCallBack<String>, compress: Boolean) {
        AppScope.launchWithCallback({ body: MultipartBody -> uploadFileInner(body, uri, postParam, callBack, compress) },
            { callBack.onError(it.message) }) {
            val context = ContextUtils.getContext()
            val cr = context.contentResolver
            val pfd = cr.openFileDescriptor(uri, "r")
            val contentType = cr.getType(uri)
            if (StringUtils.isEmpty(contentType)) {
                throw IllegalArgumentException(context.getString(R.string.invalid_img_selected))
            }
            val fileName = contentType!!.replace('/', '.')
            val fileSize = pfd!!.statSize
            val img = if (compress && fileSize >= 1024 * 1024) {
                ImageUtils.fitImageToUpload(cr.openInputStream(uri), cr.openInputStream(uri))
            } else {
                StreamUtils.toByteArray(cr.openInputStream(uri))
            }
            buildMultipartBody(fileName, img, postParam)
        }
    }

    /** 没有页面时不上传，与原先一致；上传随页面销毁取消。 */
    private fun uploadFileInner(multipartBody: MultipartBody, uri: Uri, postParam: PostParam,
                                callBack: OnHttpCallBack<String>, compress: Boolean) {
        val owner = lifecycleOwner ?: return
        scopeOf(owner).launchWithCallback({ response: String ->
            val s = response.replace("window.script_muti_get_var_store=", "")
            try {
                var obj = NgaJson.parseObject(s)
                if (obj.containsKey("error_code")) {
                    val errorCode = obj.getInteger("error_code")
                    if (errorCode == 9 && !compress) {
                        ToastUtils.info("附件过大，无法上传，重新进行压缩并上传")
                        uploadFile(uri, postParam, callBack, true)
                        return@launchWithCallback
                    }
                }
                obj = obj.getJSONObject("data")
                postParam.appendAttachment(obj.getString("attachments"), obj.getString("attachments_check"))
                callBack.onSuccess(obj.getString("url"))
            } catch (e: Exception) {
                NLog.e("exception occur while uploading file $s")
                callBack.onError("上传图片失败，请尝试更换域名后重试")
            }
        }, { callBack.onError(it.message) }) {
            api.uploadFile(BASE_URL_ATTACHMENT_SERVER, multipartBody).use { it.string() }
        }
    }

    private fun buildMultipartBody(fileName: String, bytes: ByteArray, postParam: PostParam): MultipartBody =
        MultipartBody.Builder()
            .setType("multipart/form-data".toMediaType())
            .addPart(MultipartBody.Part.createFormData("attachment_file1", fileName, bytes.toRequestBody("image/jpeg".toMediaType())))
            .addFormDataPart("attachment_file1_url_utf8_name", String(fileName.toByteArray(), Charsets.UTF_8))
            .addFormDataPart("fid", postParam.postFid.toString())
            .addFormDataPart("auth", postParam.authCode)
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

    companion object {
        private const val BASE_URL_ATTACHMENT_SERVER = "https://img8.nga.cn/attach.php?"
    }
}
