package gov.anzong.androidnga.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import android.content.Context
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.mvvm.viewmodel.HomeInboxViewModel
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.RecentNotificationScreen
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.mvvm.model.entity.RecentReplyInfo
import gov.anzong.androidnga.mvvm.model.param.ParamKey

/** 回复通知；系统通知栏的“被喷提醒”也打开这里，并带上其中的未读通知。 */
class RecentNotificationActivity : BaseActivity() {

    private val inbox: HomeInboxViewModel by viewModels()

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val unreadPids = IntentCompat.getParcelableArrayListExtra(intent, "unread", RecentReplyInfo::class.java)
            .orEmpty().mapNotNull { it.pidStr }.toSet()
        val uid = UserManagerImpl.getInstance().userId.orEmpty()
        inbox.selectAccount(uid)
        inbox.loadReplies(onRead = ::markRead)
        setContent {
            NgaTheme {
                RecentNotificationScreen(uid.isNotEmpty(), inbox.replies, inbox.repliesLoading, inbox.replyError,
                    isUnread = { it.isUnread || it.pidStr in unreadPids },
                    onRefresh = { inbox.loadReplies(force = true, onRead = ::markRead) },
                    onReply = ::openReply, onClear = inbox::clearReplies,
                    onLogin = { startActivity(Intent(this, LoginActivity::class.java)); finish() }, onBack = ::finish)
            }
        }
    }

    /** 读取成功后清除首页消息角标。 */
    private fun markRead() {
        getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE).edit().putInt(PreferenceKey.KEY_REPLY_COUNT, 0).apply()
    }

    private fun openReply(reply: RecentReplyInfo) {
        val tid = reply.tidStr?.toIntOrNull() ?: return
        val pid = reply.pidStr?.toIntOrNull() ?: return
        startActivity(Intent(this, PhoneConfiguration.getInstance().articleActivityClass)
            .putExtra(ParamKey.KEY_TID, tid).putExtra(ParamKey.KEY_PID, pid)
            .putExtra(ParamKey.KEY_TITLE, reply.title).putExtra(ParamKey.KEY_SEARCH_POST, 1))
    }
}
