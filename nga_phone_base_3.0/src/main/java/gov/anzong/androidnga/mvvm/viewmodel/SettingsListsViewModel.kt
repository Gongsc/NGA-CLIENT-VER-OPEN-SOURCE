package gov.anzong.androidnga.mvvm.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import sp.phone.common.FilterKeyword
import sp.phone.common.FilterKeywordsManager
import sp.phone.common.FilterKeywordsManagerImpl
import sp.phone.common.UserManager
import sp.phone.common.UserManagerImpl

/** 设置中的关键词屏蔽、用户黑名单与账号管理；数据都在本机，读写是同步的。 */
class SettingsListsViewModel(
    private val keywordStore: FilterKeywordsManager = FilterKeywordsManagerImpl.getInstance(),
    private val users: UserManager = UserManagerImpl.getInstance(),
) : ViewModel() {

    data class Keyword(val text: String, val enabled: Boolean)
    data class BlockedUser(val uid: String, val name: String)
    data class Account(val uid: String, val name: String, val avatarUrl: String?, val active: Boolean)

    var keywords by mutableStateOf(emptyList<Keyword>())
        private set
    var blacklist by mutableStateOf(emptyList<BlockedUser>())
        private set
    var accounts by mutableStateOf(emptyList<Account>())
        private set

    init {
        reload()
    }

    /** 重新读取；从登录页等其他页面返回时调用。 */
    fun reload() {
        keywords = keywordStore.keywords.orEmpty().map { Keyword(it.keyword.orEmpty(), it.isEnabled) }
        blacklist = users.blackList.orEmpty().map { BlockedUser(it.userId.orEmpty(), it.nickName.orEmpty()) }
        val active = users.activeUserIndex
        accounts = users.userList.orEmpty().mapIndexed { index, user ->
            Account(user.userId.orEmpty(), user.nickName.orEmpty(), user.avatarUrl, index == active)
        }
    }

    /** 添加关键词；为空或已存在时不添加并返回 false。 */
    fun addKeyword(text: String): Boolean {
        val keyword = text.trim()
        if (keyword.isEmpty() || keywords.any { it.text == keyword }) return false
        keywordStore.addKeyword(FilterKeyword(keyword))
        reload()
        return true
    }

    fun toggleKeyword(index: Int) {
        keywordStore.toggleKeyword(index)
        reload()
    }

    fun removeKeyword(index: Int) {
        keywordStore.removeKeyword(index)
        reload()
    }

    fun unblock(uid: String) {
        users.removeFromBlackList(uid)
        reload()
    }

    fun clearBlacklist() {
        users.removeAllBlackList()
        reload()
    }

    fun activateAccount(index: Int) {
        users.setActiveUser(index)
        reload()
    }

    fun removeAccount(index: Int) {
        users.removeUser(index)
        reload()
    }

    /** 把账号移到 [to]；超出范围时不移动。 */
    fun moveAccount(from: Int, to: Int) {
        if (from == to || to !in accounts.indices) return
        users.swapUser(from, to)
        reload()
    }
}
