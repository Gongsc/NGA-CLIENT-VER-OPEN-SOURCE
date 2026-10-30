package gov.anzong.androidnga.account

import android.content.Context
import gov.anzong.androidnga.network.bean.ThreadData

/** 已登录账号、黑名单与头像缓存。 */
interface UserManager {

    val userSize: Int

    val activeUser: User?

    fun initialize(context: Context)

    val activeUserIndex: Int

    val userList: List<User>

    fun hasValidUser(): Boolean

    fun setActiveUser(index: Int)

    fun toggleUser(isNext: Boolean): Int

    fun addUser(user: User)

    fun addUser(uid: String?, cid: String?, name: String?)

    fun removeUser(index: Int)

    fun swapUser(from: Int, to: Int)

    // User 类辅助接口

    val cookie: String?

    fun getCookie(user: User): String?

    val nextCookie: String?

    val userId: String?

    val cid: String?

    val userName: String?

    fun setAvatarUrl(userId: Int, url: String?)

    /** 以个人资料中的头像为准强制写入账号头像与头像缓存（[setAvatarUrl] 只在账号头像缺失时补全）。 */
    fun updateAvatarUrl(uid: String?, url: String?) {}

    // 黑名单

    fun addToBlackList(authorName: String?, authorId: String?)

    fun addToBlackList(user: User)

    fun removeFromBlackList(authorId: String?)

    fun checkBlackList(authorId: String?): Boolean

    val blackList: List<User>

    fun removeAllBlackList()

    fun putAvatarUrl(uid: String?, url: String?)

    fun putAvatarUrl(info: ThreadData)

    fun getAvatarUrl(uid: String?): String?

    fun clearAvatarUrl()
}
