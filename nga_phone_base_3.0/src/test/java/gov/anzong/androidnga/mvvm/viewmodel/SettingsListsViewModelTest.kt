package gov.anzong.androidnga.mvvm.viewmodel

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import sp.phone.common.FilterKeyword
import sp.phone.common.FilterKeywordsManager
import sp.phone.common.User
import sp.phone.common.UserManager
import java.lang.reflect.Proxy

class SettingsListsViewModelTest {

    private class Keywords : FilterKeywordsManager {
        val list = mutableListOf<FilterKeyword>()
        override fun initialize(context: Context?) = Unit
        override fun toggleKeyword(position: Int) { list[position].isEnabled = !list[position].isEnabled }
        override fun addKeyword(keyword: FilterKeyword) { list += keyword }
        override fun getKeywords() = list
        override fun removeKeyword(index: Int) { list.removeAt(index) }
    }

    /** 账号与黑名单的内存实现；切换、移除与调整顺序的规则与 UserManagerImpl 相同。 */
    private class Users : UserManager by unusedUsers() {
        val accounts = mutableListOf<User>()
        val blocked = mutableListOf<User>()
        var active = 0
        override fun getUserList() = accounts
        override fun getActiveUserIndex() = active
        override fun setActiveUser(index: Int) { active = index }
        override fun removeUser(index: Int) {
            accounts.removeAt(index)
            if (accounts.isEmpty() || active == index) active = 0 else if (index < active) active--
        }
        override fun swapUser(from: Int, to: Int) {
            val current = accounts.getOrNull(active)
            accounts.add(to, accounts.removeAt(from))
            if (current != null) active = accounts.indexOf(current)
        }
        override fun getBlackList() = blocked
        override fun removeFromBlackList(authorId: String) { blocked.removeIf { it.userId == authorId } }
        override fun removeAllBlackList() = blocked.clear()
    }

    private fun user(uid: String) = User(uid, "用户$uid")

    @Test fun keywordsAreTrimmedDeduplicatedToggledAndRemoved() {
        val store = Keywords()
        val vm = SettingsListsViewModel(store, Users())
        assertTrue(vm.addKeyword("  广告 "))
        assertFalse(vm.addKeyword("广告"))
        assertFalse(vm.addKeyword("   "))
        assertTrue(vm.addKeyword("剧透"))
        assertEquals(listOf(SettingsListsViewModel.Keyword("广告", true), SettingsListsViewModel.Keyword("剧透", true)), vm.keywords)
        vm.toggleKeyword(0)
        assertFalse(vm.keywords[0].enabled)
        vm.removeKeyword(0)
        assertEquals(listOf("剧透"), vm.keywords.map { it.text })
        assertEquals(listOf("剧透"), store.list.map { it.keyword })
    }

    @Test fun blacklistUnblockAndClear() {
        val users = Users().apply { blocked += listOf(user("1"), user("2"), user("3")) }
        val vm = SettingsListsViewModel(Keywords(), users)
        assertEquals(listOf("1", "2", "3"), vm.blacklist.map { it.uid })
        vm.unblock("2")
        assertEquals(listOf("1", "3"), vm.blacklist.map { it.uid })
        vm.clearBlacklist()
        assertTrue(vm.blacklist.isEmpty())
    }

    @Test fun removingOrMovingAccountsKeepsTheActiveAccount() {
        val users = Users().apply { accounts += listOf(user("a"), user("b"), user("c")); active = 2 }
        val vm = SettingsListsViewModel(Keywords(), users)
        assertEquals("c", vm.accounts.single { it.active }.uid)
        vm.removeAccount(0)
        assertEquals(listOf("b", "c"), vm.accounts.map { it.uid })
        assertEquals("c", vm.accounts.single { it.active }.uid)
        vm.moveAccount(1, 0)
        assertEquals(listOf("c", "b"), vm.accounts.map { it.uid })
        assertEquals("c", vm.accounts.single { it.active }.uid)
        vm.moveAccount(1, 5)
        assertEquals(listOf("c", "b"), vm.accounts.map { it.uid })
        vm.activateAccount(1)
        assertEquals("b", vm.accounts.single { it.active }.uid)
        vm.removeAccount(1)
        assertEquals("c", vm.accounts.single { it.active }.uid)
    }

    @Test fun reloadPicksUpAccountsAddedElsewhere() {
        val users = Users()
        val vm = SettingsListsViewModel(Keywords(), users)
        assertTrue(vm.accounts.isEmpty())
        users.accounts += user("新")
        vm.reload()
        assertEquals(listOf("新"), vm.accounts.map { it.uid })
        assertTrue(vm.accounts[0].active)
    }

    private companion object {
        fun unusedUsers(): UserManager = UserManager::class.java.cast(
            Proxy.newProxyInstance(UserManager::class.java.classLoader, arrayOf(UserManager::class.java)) { _, method, _ ->
                throw UnsupportedOperationException(method.name)
            })
    }
}
