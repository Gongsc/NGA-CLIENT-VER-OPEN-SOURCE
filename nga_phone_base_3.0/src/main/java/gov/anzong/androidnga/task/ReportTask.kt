package gov.anzong.androidnga.task

import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import gov.anzong.androidnga.network.post.ForumActionClient

/** 举报楼层，经 [ForumActionClient] 提交；论坛拒绝（如举报间隔未到）或结果不明时回调错误。 */
class ReportTask @JvmOverloads constructor(private val client: () -> ForumActionClient = ForumActionClient::create) {

    @JvmOverloads
    fun execute(tid: Int, pid: Int, reason: String, callBack: OnHttpCallBack<String> = ToastResult) {
        AppScope.launchWithCallback(callBack) { client().report(tid, pid, reason) }
    }
}
