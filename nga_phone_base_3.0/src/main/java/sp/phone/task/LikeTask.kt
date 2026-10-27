package sp.phone.task

import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import sp.phone.http.OnSimpleHttpCallBack
import sp.phone.http.retrofit.NgaApi
import sp.phone.util.NLog

/** 点赞或点踩；响应无法解析时提示网络错误，请求失败只记录日志。 */
class LikeTask @JvmOverloads constructor(private val api: () -> NgaApi = NgaApi::get) {

    private val paramMap = mapOf(
        "__lib" to "topic_recommend",
        "__act" to "add",
        "raw" to "3",
        "pid" to "0",
        "__output" to "8",
    )

    fun execute(tid: Int, pid: Int, like: Int, callBack: OnSimpleHttpCallBack<String>) {
        val map = HashMap(paramMap)
        map["value"] = like.toString()
        map["tid"] = tid.toString()
        map["pid"] = pid.toString()
        AppScope.launchWithCallback({ message: String -> callBack.onResult(message) }, { NLog.e(it.message) }) {
            val response = api().post(map)
            try {
                NgaJson.parseObject(response).getJSONObject("data").getString("0")
            } catch (e: Exception) {
                ContextUtils.getString(R.string.network_error)
            }
        }
    }

    companion object {
        const val SUPPORT = 1
        const val OPPOSE = -1
    }
}
