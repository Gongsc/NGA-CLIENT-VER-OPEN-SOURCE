package sp.phone.mvp.model

import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import sp.phone.http.bean.MessageDetailInfo
import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.contract.MessageDetailContract
import sp.phone.mvp.model.convert.MessageConvertFactory

/**
 * Created by Justwen on 2017/10/11.
 *
 * http://bbs.nga.cn/nuke.php?__lib=message&__act=message&act=read&page=1&mid=1&lite=js
 */
class MessageDetailModel @JvmOverloads constructor(private val api: () -> NgaApi = NgaApi::get) :
    BaseModel(), MessageDetailContract.IMessageModel {

    override fun loadPage(page: Int, mid: Int, callBack: OnHttpCallBack<MessageDetailInfo>) {
        val params = mapOf(
            "__lib" to "message", "__act" to "message", "act" to "read", "lite" to "js",
            "page" to page.toString(), "mid" to mid.toString(),
        )
        AppScope.launchWithCallback(callBack) {
            val factory = MessageConvertFactory()
            factory.getMessageDetailInfo(api().get(params), page) ?: throw Exception(factory.errorMsg)
        }
    }
}
