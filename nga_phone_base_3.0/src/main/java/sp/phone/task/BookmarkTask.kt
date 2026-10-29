package sp.phone.task

import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import sp.phone.http.post.ForumActionClient

/** 收藏主题或回复，经 [ForumActionClient] 提交；默认以提示显示结果或失败原因。 */
class BookmarkTask @JvmOverloads constructor(private val client: () -> ForumActionClient = ForumActionClient::create) {

    @JvmOverloads
    fun execute(tid: String, pid: String? = null, callBack: OnHttpCallBack<String> = ToastResult) {
        AppScope.launchWithCallback(callBack) { client().bookmark(tid, pid) }
    }
}
