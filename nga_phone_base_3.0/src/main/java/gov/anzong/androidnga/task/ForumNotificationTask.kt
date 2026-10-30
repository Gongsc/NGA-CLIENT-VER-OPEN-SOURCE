package gov.anzong.androidnga.task

import androidx.lifecycle.LifecycleOwner
import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.coroutines.scopeOf
import gov.anzong.androidnga.http.OnHttpCallBack
import gov.anzong.androidnga.network.post.ForumActionClient
import gov.anzong.androidnga.network.retrofit.NgaApi
import gov.anzong.androidnga.mvvm.model.convert.ForumNotificationFactory
import gov.anzong.androidnga.mvvm.model.entity.NotificationInfo
import gov.anzong.androidnga.mvvm.model.entity.RecentReplyInfo
import gov.anzong.androidnga.util.NLog

/**
 * 论坛通知。[owner] 不为空时最近回复随页面销毁取消；全部通知与清空不随页面取消。
 */
class ForumNotificationTask @JvmOverloads constructor(
    private val owner: LifecycleOwner?,
    private val api: () -> NgaApi = NgaApi::get,
    private val host: () -> String = Utils::getNGAHost,
    private val client: () -> ForumActionClient = ForumActionClient::create,
) {
    private val notificationUrl get() = host() + "nuke.php?__lib=noti&__output=8&__act=get_all"

    /** 只返回最近被喷的信息。 */
    fun queryRecentReply(callBack: OnHttpCallBack<List<RecentReplyInfo>>) {
        scopeOf(owner).launchWithCallback(callBack) {
            ForumNotificationFactory.buildRecentReplyList(api().get(notificationUrl))
        }
    }

    /** 返回最近被喷和短信的信息。 */
    fun queryNotification(callBack: OnHttpCallBack<List<NotificationInfo>>) {
        AppScope.launchWithCallback(callBack) {
            ForumNotificationFactory.buildNotificationList(api().get(notificationUrl))
        }
    }

    /** 清空论坛提醒，经 [ForumActionClient] 提交；失败只记录日志。 */
    fun clearAllNotification() {
        AppScope.launchWithCallback({ s: String -> NLog.d(s) }, { NLog.e(it.message) }) { client().clearNotifications() }
    }
}
