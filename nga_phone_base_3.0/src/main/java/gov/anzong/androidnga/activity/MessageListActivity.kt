package gov.anzong.androidnga.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.mvvm.viewmodel.HomeInboxViewModel
import gov.anzong.androidnga.ui.compose.MessageListScreen
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.account.UserManagerImpl

/** 短消息会话列表；系统通知栏的短消息提醒也打开这里。从其他页面返回时刷新。 */
class MessageListActivity : BaseActivity() {

    private val inbox: HomeInboxViewModel by viewModels()
    private var accounts by mutableStateOf(emptyList<String>())
    private var activeAccount by mutableIntStateOf(0)
    private var paused = false

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        setContent {
            NgaTheme {
                MessageListScreen(accounts, activeAccount, inbox.messages, inbox.messagesLoading, inbox.messageError,
                    hasMore = inbox.nextPage > 0, onSwitchAccount = ::switchAccount,
                    onRefresh = { inbox.loadMessages(force = true) }, onRetry = inbox::retryMessages,
                    onLoadMore = { inbox.loadMessages(more = true) },
                    onOpen = { startActivity(Intent(this, PhoneConfiguration.getInstance().messageDetialActivity).putExtra("mid", it.mid)) },
                    onCompose = { AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_MESSAGE_POST).withString("action", "new").navigation(this) },
                    onLogin = { startActivity(Intent(this, LoginActivity::class.java)) }, onBack = ::finish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 登录、切换账号或发信后返回时重新读取账号与列表。
        val users = UserManagerImpl.getInstance()
        accounts = users.userList.map { it.nickName.orEmpty().ifBlank { it.userId.orEmpty() } }
        activeAccount = users.activeUserIndex
        inbox.selectAccount(users.userId.orEmpty())
        inbox.loadMessages(force = paused)
        paused = false
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    private fun switchAccount(index: Int) {
        val users = UserManagerImpl.getInstance()
        users.setActiveUser(index)
        activeAccount = users.activeUserIndex
        inbox.selectAccount(users.userId.orEmpty())
        inbox.loadMessages()
    }
}
