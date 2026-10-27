package sp.phone.mvp.model

import android.util.Base64
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.coroutines.scopeOf
import gov.anzong.androidnga.http.OnHttpCallBack
import okhttp3.MultipartBody
import sp.phone.http.retrofit.RetrofitHelper
import sp.phone.mvp.contract.LoginContract
import sp.phone.param.LoginParam
import sp.phone.util.ActivityUtils
import sp.phone.util.NLog

/**
 * Created by Justwen on 2017/6/16.
 *
 * 验证码与登录请求随页面销毁取消；两者使用不带公共拦截器的独立实例，与原先一致。
 */
class LoginModel : BaseModel(), LoginContract.Model {

    override fun loadAuthCode(callBack: OnHttpCallBack<LoginParam>) {
        val cookie = "_" + Math.random()
        val url = "https://bbs.ngacn.cc/login_check_code.php?id=$cookie/"
        scopeOf(lifecycleOwner).launchWithCallback({ base64: String ->
            val loginParam = LoginParam()
            loginParam.authCodeCookie = cookie
            loginParam.dataUrl = "data:image/png;base64,$base64"
            callBack.onSuccess(loginParam)
        }, { error ->
            callBack.onError(error.message)
            NLog.e(error.message)
        }) {
            RetrofitHelper.getAuthCodeApi().getAuthCodeImage(url).use { Base64.encodeToString(it.bytes(), Base64.DEFAULT) }
        }
    }

    override fun login(loginParam: LoginParam, callBack: OnHttpCallBack<String>) {
        val fieldMap = HashMap<String, String>()
        fieldMap["name"] = loginParam.userName
        fieldMap["type"] = "name"
        fieldMap["password"] = loginParam.password
        fieldMap["rid"] = loginParam.authCodeCookie
        fieldMap["captcha"] = loginParam.authCode.uppercase()
        fieldMap["__lib"] = "login"
        fieldMap["__act"] = "login"
        fieldMap["__output"] = "1"
        fieldMap["__inchst"] = "UTF-8"
        fieldMap["raw"] = "3"
        fieldMap["qrkey"] = ""
        // 结果只以提示显示，失败只记录日志，与原先一致。
        scopeOf(lifecycleOwner).launchWithCallback({ s: String -> ActivityUtils.showToast(s) }, { NLog.e(it.message) }) {
            RetrofitHelper.getDefaultApi().login(buildMultipartBody(fieldMap))
        }
    }

    private fun buildMultipartBody(fieldMap: Map<String, String>): MultipartBody {
        val builder = MultipartBody.Builder()
        for ((key, value) in fieldMap) builder.addFormDataPart(key, value)
        return builder.build()
    }
}
