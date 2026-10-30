package gov.anzong.androidnga.task

import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import gov.anzong.androidnga.network.post.ForumActionClient

/** 点赞或点踩，经 [ForumActionClient] 提交；论坛拒绝、结果不明或网络失败时回调错误。 */
class LikeTask @JvmOverloads constructor(private val client: () -> ForumActionClient = ForumActionClient::create) {

    @JvmOverloads
    fun execute(tid: Int, pid: Int, like: Int, callBack: OnHttpCallBack<String> = ToastResult) {
        AppScope.launchWithCallback(callBack) { client().like(tid, pid, like) }
    }

    companion object {
        const val SUPPORT = 1
        const val OPPOSE = -1
    }
}
