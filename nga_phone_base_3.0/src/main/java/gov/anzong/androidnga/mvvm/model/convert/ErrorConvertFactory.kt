package gov.anzong.androidnga.mvvm.model.convert

import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonObject
import gov.anzong.androidnga.base.util.ContextUtils
import java.net.UnknownHostException

/**
 * Created by Justwen on 2017/11/23.
 */
object ErrorConvertFactory {

    /** 论坛错误响应中的提示文字；无法识别时返回 null。 */
    @JvmStatic
    fun getErrorMessage(js: String): String? = when {
        js.isEmpty() -> ContextUtils.getString(R.string.network_error)
        js.contains("未登录") -> "请重新登录"
        js.contains("无此页") -> ContextUtils.getString(R.string.last_page_prompt)
        else -> try {
            val data = (NgaJson.parse(js) as NgaJsonObject)["data"] as NgaJsonObject
            (data["__MESSAGE"] as NgaJsonObject).getString("1")
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun getErrorMessage(throwable: Throwable): String? =
        if (throwable is UnknownHostException) ContextUtils.getString(R.string.network_error) else throwable.message
}
