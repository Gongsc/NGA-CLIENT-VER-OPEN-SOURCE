package gov.anzong.androidnga.mvvm.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import gov.anzong.androidnga.coroutines.PendingResponse
import gov.anzong.androidnga.mvvm.viewmodel.BoardViewModel.AddBoardResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import sp.phone.common.User
import sp.phone.common.UserManager
import sp.phone.mvp.model.BoardStore
import sp.phone.mvp.model.entity.Board
import sp.phone.mvp.model.entity.BoardCategory
import java.io.IOException
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModelTest {
    @get:Rule val liveData = InstantTaskExecutorRule()
    private val main = UnconfinedTestDispatcher()
    @Before fun setMain() = Dispatchers.setMain(main)
    @After fun resetMain() = Dispatchers.resetMain()

    private class FakeBoards : BoardStore by unusedBoards() {
        val bookmarks = BoardCategory("我的收藏")
        val names = mapOf((7 to 0) to "艾泽拉斯议事厅")
        override fun getBookmarkCategory() = bookmarks
        override fun isBookmark(fid: Int, stid: Int) = bookmarks.boardList.any { it.fid == fid && it.stid == stid }
        override fun getBoardName(fid: Int, stid: Int) = names[fid to stid]
        override fun addBookmark(fid: Int, stid: Int, boardName: String) = bookmarks.addBoard(Board(fid, stid, boardName))
    }

    private class FakeUsers(val list: MutableList<User> = mutableListOf()) : UserManager by unusedUsers() {
        var active = 0
        val avatars = mutableMapOf<String, String>()
        override fun hasValidUser() = list.isNotEmpty()
        override fun getUserSize() = list.size
        override fun getActiveUserIndex() = active
        override fun setActiveUser(index: Int) { active = index }
        override fun getActiveUser() = list.getOrNull(active)
        override fun getCookie(user: User) = "cookie-${user.userId}"
        override fun getAvatarUrl(uid: String) = avatars[uid].orEmpty()
        override fun putAvatarUrl(uid: String, url: String) { avatars[uid] = url }
        override fun setAvatarUrl(userId: Int, url: String) { list.filter { it.userId == userId.toString() }.forEach { it.avatarUrl = url } }
    }

    private fun user(uid: String, name: String = "用户$uid") = User(uid, name, "cid")

    private fun viewModel(boards: BoardStore = FakeBoards(), users: UserManager = FakeUsers(),
                          avatar: suspend (String, String) -> String = { _, _ -> "" }) =
        BoardViewModel(boards, users, avatar, main)

    @Test fun addBoardValidatesInputAndUsesKnownName() {
        val boards = FakeBoards()
        val vm = viewModel(boards, FakeUsers(mutableListOf(user("1"))))
        assertEquals(AddBoardResult.Invalid("请输入版块编号或合集编号"), vm.addBoard(" ", ""))
        assertEquals(AddBoardResult.Invalid("版块编号或合集编号格式不正确"), vm.addBoard("abc", ""))
        assertEquals(AddBoardResult.Added, vm.addBoard(" 7 ", ""))
        assertEquals(AddBoardResult.Exists, vm.addBoard("7", "0"))
        assertEquals(AddBoardResult.Added, vm.addBoard("", "42"))
        assertEquals(listOf("艾泽拉斯议事厅", "合集 42"), boards.bookmarks.boardList.map { it.name })
    }

    @Test fun addBoardWithoutAccountAsksForLogin() {
        val boards = FakeBoards()
        val vm = viewModel(boards, FakeUsers())
        assertEquals(AddBoardResult.NeedLogin, vm.addBoard("7", ""))
        assertEquals(0, boards.bookmarks.size())
        assertEquals(1, vm.defaultCategory)
    }

    @Test fun switchingAccountsWrapsAroundAndNeedsTwoAccounts() {
        val users = FakeUsers(mutableListOf(user("1")))
        val vm = viewModel(users = users)
        assertNull(vm.switchToNextAccount())
        users.list += user("2")
        assertEquals("2", vm.switchToNextAccount()?.userId)
        assertEquals("1", vm.switchToNextAccount()?.userId)
        assertEquals(0, vm.defaultCategory)
    }

    @Test fun missingAvatarIsFetchedOnceAndWrittenBack() {
        val alice = user("1")
        val users = FakeUsers(mutableListOf(alice))
        val requests = mutableListOf<Pair<String, String>>()
        val vm = viewModel(users = users, avatar = { uid, cookie -> requests += uid to cookie; "https://img.example.test/a.jpg" })
        val updated = mutableListOf<String>()
        vm.avatarUpdated.observeForever { updated += it }
        vm.fetchMissingAvatar(alice)
        vm.fetchMissingAvatar(alice)
        assertEquals(listOf("1" to "cookie-1"), requests)
        assertEquals("https://img.example.test/a.jpg", alice.avatarUrl)
        assertEquals("https://img.example.test/a.jpg", users.avatars["1"])
        assertEquals(listOf("1"), updated)
    }

    @Test fun cachedAvatarIsUsedWithoutRequest() {
        val bob = user("2")
        val users = FakeUsers(mutableListOf(bob)).apply { avatars["2"] = "https://img.example.test/b.jpg" }
        val vm = viewModel(users = users, avatar = { _, _ -> error("不应请求") })
        vm.fetchMissingAvatar(bob)
        assertEquals("https://img.example.test/b.jpg", bob.avatarUrl)
    }

    @Test fun failedAvatarRequestCanBeRetriedButEmptyProfileIsNot() {
        val alice = user("1")
        var calls = 0
        var respond: suspend () -> String = { throw IOException("离线") }
        val vm = viewModel(users = FakeUsers(mutableListOf(alice)), avatar = { _, _ -> calls++; respond() })
        vm.fetchMissingAvatar(alice)
        respond = { "" }
        vm.fetchMissingAvatar(alice)
        vm.fetchMissingAvatar(alice)
        assertEquals(2, calls)
        assertNull(alice.avatarUrl)
    }

    @Test fun pendingAvatarRequestIsNotDuplicated() {
        val alice = user("1")
        val pending = PendingResponse<String>()
        var calls = 0
        val vm = viewModel(users = FakeUsers(mutableListOf(alice)), avatar = { _, _ -> calls++; pending.await() })
        vm.fetchMissingAvatar(alice)
        vm.fetchMissingAvatar(alice)
        assertEquals(1, calls)
        pending.complete("https://img.example.test/a.jpg")
        assertEquals("https://img.example.test/a.jpg", alice.avatarUrl)
    }

    /** 只实现用到的方法，其余调用视为测试错误。 */
    private companion object {
        fun unusedBoards(): BoardStore = unusedOf(BoardStore::class.java)
        fun unusedUsers(): UserManager = unusedOf(UserManager::class.java)
        fun <T> unusedOf(type: Class<T>): T = type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            throw UnsupportedOperationException(method.name)
        })
    }
}
