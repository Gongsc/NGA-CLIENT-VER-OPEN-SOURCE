package gov.anzong.androidnga.mvvm.model

import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import sp.phone.http.bean.MessageListInfo
import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.model.convert.MessageConvertFactory

/**
 * Created by Justwen on 2017/10/10.
 *
 * http://bbs.nga.cn/nuke.php?__lib=message&__act=message&act=list&page=1&lite=js
 */
class MessageListModel @JvmOverloads constructor(private val api: () -> NgaApi = NgaApi::get) {

    fun loadPage(page: Int, callBack: OnHttpCallBack<MessageListInfo>) {
        val params = mapOf("__lib" to "message", "__act" to "message", "act" to "list", "lite" to "js", "page" to page.toString())
        AppScope.launchWithCallback(callBack) {
            val factory = MessageConvertFactory()
            factory.getMessageListInfo(api().get(params)) ?: throw Exception(factory.errorMsg)
        }
    }
}
