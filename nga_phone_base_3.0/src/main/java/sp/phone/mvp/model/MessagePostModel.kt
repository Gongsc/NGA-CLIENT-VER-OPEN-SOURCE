package sp.phone.mvp.model

import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import sp.phone.http.post.MessagePostClient
import sp.phone.mvp.contract.MessagePostContract
import sp.phone.param.MessagePostParam
import sp.phone.util.ActivityUtils

/**
 * Created by Justwen on 2017/6/25.
 *
 * 短消息经 [MessagePostClient] 提交；不随页面取消，与原先一致。
 */
class MessagePostModel : BaseModel(), MessagePostContract.Model {

    override fun postMessage(param: MessagePostParam, callBack: OnHttpCallBack<String>) {
        ActivityUtils.getInstance().noticeSaying(ContextUtils.getContext())
        AppScope.launchWithCallback({ message: String -> callBack.onSuccess(message) },
            { callBack.onError(it.message ?: "发送失败，请稍后重试") }) {
            MessagePostClient.create().send(param)
        }
    }
}
