package gov.anzong.androidnga.mvvm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import sp.phone.common.User
import sp.phone.common.UserManager
import sp.phone.common.UserManagerImpl
import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.model.BoardModel
import sp.phone.mvp.model.BoardStore
import sp.phone.mvp.model.entity.BoardCategory
import sp.phone.ui.fragment.parseProfileAvatar

/** 首页版块页：本地版块收藏、账号切换，以及补取缺失的账号头像。 */
class BoardViewModel(
    private val boards: BoardStore = BoardModel.getInstance(),
    private val users: UserManager = UserManagerImpl.getInstance(),
    private val avatarRequest: suspend (uid: String, cookie: String) -> String = ::requestProfileAvatar,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
) : ViewModel() {

    sealed interface AddBoardResult {
        data object Added : AddBoardResult
        data object Exists : AddBoardResult
        data object NeedLogin : AddBoardResult
        data class Invalid(val message: String) : AddBoardResult
    }

    val categories: List<BoardCategory> get() = boards.boardCategories

    /** 首次进入时，没有收藏且未登录则默认显示第二个分类。 */
    val defaultCategory get() = if (boards.bookmarkCategory.size() == 0 && !users.hasValidUser()) 1 else 0

    /** 按输入的版块编号或合集编号收藏版块。 */
    fun addBoard(fidText: String, stidText: String): AddBoardResult {
        val fid: Int
        val stid: Int
        try {
            fid = fidText.trim().ifEmpty { "0" }.toInt()
            stid = stidText.trim().ifEmpty { "0" }.toInt()
        } catch (e: NumberFormatException) {
            return AddBoardResult.Invalid("版块编号或合集编号格式不正确")
        }
        if (fid == 0 && stid == 0) return AddBoardResult.Invalid("请输入版块编号或合集编号")
        if (!users.hasValidUser()) return AddBoardResult.NeedLogin
        if (boards.isBookmark(fid, stid)) return AddBoardResult.Exists
        val name = boards.getBoardName(fid, stid)?.takeIf { it.isNotEmpty() }
            ?: if (stid != 0) "合集 $stid" else "版块 $fid"
        boards.addBookmark(fid, stid, name)
        return AddBoardResult.Added
    }

    /** 切换到下一个账号并返回它；不足两个账号时返回 null，应去登录。 */
    fun switchToNextAccount(): User? {
        if (users.userSize < 2) return null
        users.setActiveUser((users.activeUserIndex + 1) % users.userSize)
        return users.activeUser
    }

    private val avatarRequests = mutableSetOf<String>()
    private val _avatarUpdated = MutableLiveData<String>()

    /** 账号头像补取成功后发出该账号的 uid。 */
    val avatarUpdated: LiveData<String> get() = _avatarUpdated

    /** 账号头像只在浏览到自己的帖子时才会记录；缺失时从个人资料补取一次并写回账号。 */
    fun fetchMissingAvatar(user: User?) {
        val uid = user?.userId?.takeIf { it.isNotEmpty() } ?: return
        if (!user.avatarUrl.isNullOrBlank() || !avatarRequests.add(uid)) return
        val cached = users.getAvatarUrl(uid).orEmpty()
        if (cached.isNotBlank()) {
            applyAvatar(uid, cached)
            return
        }
        val cookie = users.getCookie(user).orEmpty()
        viewModelScope.launch {
            // 请求失败或被取消时允许下次重试；资料中确实没有头像时不再请求
            val url = try {
                withContext(worker) { avatarRequest(uid, cookie) }
            } catch (e: Exception) {
                avatarRequests.remove(uid)
                if (e is CancellationException) throw e
                return@launch
            }
            if (url.isNotBlank()) applyAvatar(uid, url)
        }
    }

    private fun applyAvatar(uid: String, url: String) {
        users.putAvatarUrl(uid, url)
        uid.toIntOrNull()?.let { users.setAvatarUrl(it, url) }
        _avatarUpdated.value = uid
    }
}

private suspend fun requestProfileAvatar(uid: String, cookie: String): String {
    val headers = mapOf("Cookie" to cookie, "Referer" to Utils.getNGAHost() + "nuke.php?func=ucp&lite=jsx&uid=$uid")
    return parseProfileAvatar(NgaApi.get().get(Utils.getNGAHost() + "nuke.php?__lib=ucp&__act=get&lite=js&noprefix&uid=$uid", headers))
}
