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
        var sign = postParam.sign
        try {
            sign = URLEncoder.encode(sign, "gbk")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        paramMap["uid"] = postParam.uid
        paramMap["sign"] = sign
        val fields = HashMap(paramMap)
        job = AppScope.launchWithCallback({ s: String ->
            job = null
            if (s.contains("操作成功")) callBack.onSuccess("修改成功！") else callBack.onError(s)
        }, {
            job = null
            callBack.onError(it.message)
        }) { api().post(fields) }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    fun isRunning(): Boolean = job?.isActive == true
}
