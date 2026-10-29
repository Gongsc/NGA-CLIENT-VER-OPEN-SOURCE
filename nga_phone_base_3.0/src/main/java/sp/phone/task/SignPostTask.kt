package sp.phone.task

import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import kotlinx.coroutines.Job
import sp.phone.http.retrofit.NgaApi
import sp.phone.param.SignPostParam
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/** 修改签名；提交中再次调用会被忽略。 */
class SignPostTask @JvmOverloads constructor(private val api: () -> NgaApi = NgaApi::get) {

    private val paramMap = hashMapOf(
        "__lib" to "set_sign",
        "__act" to "set",
        "raw" to "3",
        "lite" to "js",
        "charset" to "gbk",
    )
    private var job: Job? = null

    fun execute(postParam: SignPostParam, callBack: OnHttpCallBack<String>) {
        if (isRunning()) return
        job = AppScope.launchWithCallback({ s: String ->
            job = null
            callBack.onSuccess(s)
        }, {
            job = null
            callBack.onError(it.message)
        }) { submit(postParam) }
    }

    /** 提交签名；成功返回提示文字，论坛返回其他内容时以该内容作为错误抛出。 */
    suspend fun submit(postParam: SignPostParam): String {
        var sign = postParam.sign
        try {
            sign = URLEncoder.encode(sign, "gbk")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        val fields = HashMap(paramMap)
        fields["uid"] = postParam.uid
        fields["sign"] = sign
        val response = api().post(fields)
        if (!response.contains("操作成功")) throw IllegalStateException(response)
        return "修改成功！"
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    fun isRunning(): Boolean = job?.isActive == true
}
