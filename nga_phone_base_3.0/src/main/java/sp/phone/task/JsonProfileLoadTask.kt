package sp.phone.task

import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import kotlinx.coroutines.Job
import sp.phone.http.bean.ProfileData
import sp.phone.http.retrofit.NgaApi
import sp.phone.util.ActivityUtils

/** 加载个人资料，解析见 [ProfileParser]；页面销毁时调用 [cancel] 后不再回调。 */
class JsonProfileLoadTask @JvmOverloads constructor(
    private val callback: OnHttpCallBack<ProfileData>,
    private val api: () -> NgaApi = NgaApi::get,
) {
    private var job: Job? = null

    fun execute(params: String) {
        val headers = mapOf("Referer" to Utils.getNGAHost() + "nuke.php?func=ucp&lite=jsx&" + params)
        val url = Utils.getNGAHost() + "nuke.php?__lib=ucp&__act=get&lite=js&noprefix&" + params
        job = AppScope.launchWithCallback({ profile: ProfileData ->
            ActivityUtils.getInstance().dismiss()
            callback.onSuccess(profile)
            job = null
        }, { error ->
            ActivityUtils.getInstance().dismiss()
            callback.onError(error.message)
            job = null
        }) { ProfileParser.parse(api().get(url, headers)) }
    }

    fun cancel() {
        job?.cancel()
    }
}
