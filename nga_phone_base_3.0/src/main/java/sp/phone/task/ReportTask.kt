package sp.phone.task

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import sp.phone.http.retrofit.NgaApi

/** 举报：响应含 error 时回调错误文字，含 data 时回调成功文字。 */
class ReportTask @JvmOverloads constructor(private val api: () -> NgaApi = NgaApi::get) {

    /**
     * error : {"0":"你在217秒后方可举报"}
     * data : {"0":"操作成功"}
     * time : 1601530856
     */
    class ResultBean {
        var error: Map<String, String>? = null
        var data: Map<String, String>? = null
        var time = 0
    }

    fun pos(queryMap: Map<String, String>, fieldMap: Map<String, String>, callBack: OnHttpCallBack<String>) {
        AppScope.launchWithCallback({ result: ResultBean ->
            val error = result.error
            val data = result.data
            if (error != null) {
                callBack.onError(error["0"])
            } else if (data != null) {
                callBack.onSuccess(data["0"])
            }
        }, { callBack.onError(it.message) }) {
            NgaJson.parseObject(api().post(queryMap, fieldMap), ResultBean::class.java)
        }
    }
}
