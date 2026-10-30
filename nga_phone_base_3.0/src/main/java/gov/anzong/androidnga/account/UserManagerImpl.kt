package gov.anzong.androidnga.account

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.util.PreferenceUtils
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.coroutines.BackgroundTask
import gov.anzong.androidnga.db.AppDatabase
import gov.anzong.androidnga.network.bean.ThreadData

/**
 * 账号保存在数据库中（按保存顺序），当前账号序号、黑名单与头像缓存保存在设置中。
 */
class UserManagerImpl private constructor() : UserManager {

    private var activeIndex = 0

    private var blacklist: MutableList<User> = ArrayList()

    private var users: MutableList<User> = ArrayList()

    private lateinit var prefs: SharedPreferences

    private lateinit var avatarPreferences: SharedPreferences

    override fun initialize(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        avatarPreferences = context.getSharedPreferences(PreferenceKey.PREFERENCE_AVATAR, Context.MODE_PRIVATE)
        activeIndex = PreferenceUtils.getData(PreferenceKey.USER_ACTIVE_INDEX, 0)
        val savedBlacklist = prefs.getString(PreferenceKey.BLACK_LIST, "")
        blacklist = if (savedBlacklist.isNullOrEmpty()) ArrayList() else NgaJson.parseArray(savedBlacklist, User::class.java) ?: ArrayList()
        users = ArrayList(AppDatabase.getInstance().userDao().loadUser())
        transformData()
    }

    /** 旧版把账号列表以 JSON 保存在设置中，迁移到数据库。 */
    private fun transformData() {
        if (users.isNotEmpty()) return
        val oldUserStr = PreferenceUtils.getData(PreferenceKey.USER_LIST, "")
        if (!oldUserStr.isNullOrEmpty()) {
            val oldList = NgaJson.parseArray(oldUserStr, User::class.java)
            PreferenceUtils.edit().remove(PreferenceKey.USER_LIST).apply()
            if (oldList != null) {
                users.addAll(oldList)
                saveUsers()
            }
        }
    }

    /** 在调用时取账号列表的快照，按顺序在后台写入数据库，较早的快照不会覆盖较新的。 */
    private fun saveUsers() {
        val snapshot = users.toTypedArray()
        BackgroundTask.runSerially { AppDatabase.getInstance().userDao().replaceAll(*snapshot) }
    }

    override val activeUserIndex: Int
        get() = activeIndex

    override val activeUser: User?
        get() = if (users.isEmpty()) null else users[activeIndex]

    override val userList: List<User>
        get() = users

    override fun hasValidUser(): Boolean = users.isNotEmpty()

    override val cid: String
        get() = activeUser?.cid ?: ""

    override val userName: String
        get() = activeUser?.nickName ?: ""

    override val userId: String
        get() = activeUser?.userId ?: ""

    override fun setAvatarUrl(userId: Int, url: String?) {
        val uid = userId.toString()
        blacklist.firstOrNull { it.userId == uid }?.let { user ->
            if (user.avatarUrl == null) {
                user.avatarUrl = url
                commit()
            }
            return
        }
        users.firstOrNull { it.userId == uid }?.let { user ->
            // 帖子中的头像可能是修改前的旧地址，只用于补全；账号头像以个人资料为准（见 updateAvatarUrl）。
            if (user.avatarUrl.isNullOrEmpty()) {
                user.avatarUrl = url
                commit()
            }
        }
    }

    override fun updateAvatarUrl(uid: String?, url: String?) {
        if (uid.isNullOrEmpty() || url.isNullOrEmpty()) {
            return
        }
        avatarPreferences.edit { putString(uid, url) }
        users.firstOrNull { it.userId == uid }?.let { user ->
            user.avatarUrl = url
            commit()
        }
    }

    override fun setActiveUser(index: Int) {
        activeIndex = index
        commit()
    }

    override fun toggleUser(isNext: Boolean): Int {
        activeIndex = nextActiveIndex(isNext)
        commit()
        return activeIndex
    }

    private fun nextActiveIndex(isNext: Boolean): Int {
        val index = if (isNext) activeIndex + 1 else activeIndex + users.size - 1
        return index % users.size
    }

    /** 已有同一 UID 的账号时替换，否则添加到末尾。 */
    override fun addUser(user: User) {
        val index = users.indexOfFirst { it.userId == user.userId }
        if (index >= 0) users[index] = user else users.add(user)
        commit()
    }

    override fun addUser(uid: String?, cid: String?, name: String?) {
        addUser(User().apply {
            this.cid = cid
            userId = uid
            nickName = name
        })
    }

    override fun removeUser(index: Int) {
        users.removeAt(index)
        if (users.isEmpty() || activeIndex == index) {
            activeIndex = 0
        } else if (index < activeIndex) {
            // 当前账号前移了一位，保持仍是同一个账号
            activeIndex--
        }
        commit()
    }

    private fun saveBlacklist(editor: SharedPreferences.Editor) {
        editor.putString(PreferenceKey.BLACK_LIST, NgaJson.toJSONString(blacklist))
    }

    private fun commit() {
        prefs.edit {
            putInt(PreferenceKey.USER_ACTIVE_INDEX, activeIndex)
            saveBlacklist(this)
        }
        saveUsers()
    }

    override val cookie: String
        get() = activeUser?.let(::getCookie) ?: ""

    override fun getCookie(user: User): String =
        if (!user.cid.isNullOrEmpty() && !user.userId.isNullOrEmpty()) {
            "ngaPassportUid=${user.userId}; ngaPassportCid=${user.cid}"
        } else {
            ""
        }

    override val nextCookie: String?
        get() {
            val nextIndex = nextActiveIndex(true)
            return if (nextIndex != activeIndex) getCookie(users[nextIndex]) else null
        }

    override fun swapUser(from: Int, to: Int) {
        val active = activeUser
        if (from < to) {
            for (i in from until to) java.util.Collections.swap(users, i, i + 1)
        } else {
            for (i in from downTo to + 1) java.util.Collections.swap(users, i, i - 1)
        }
        // 调整顺序不改变当前账号
        if (active != null) {
            activeIndex = users.indexOf(active)
        }
        commit()
    }

    override fun addToBlackList(authorName: String?, authorId: String?) {
        if (blacklist.any { it.userId == authorId }) {
            return
        }
        blacklist.add(User().apply {
            userId = authorId
            nickName = authorName
        })
        prefs.edit { saveBlacklist(this) }
    }

    override fun addToBlackList(user: User) {
        if (!blacklist.contains(user)) {
            blacklist.add(user)
        }
        prefs.edit { saveBlacklist(this) }
    }

    override fun removeFromBlackList(authorId: String?) {
        val index = blacklist.indexOfFirst { it.userId == authorId }
        if (index >= 0) {
            blacklist.removeAt(index)
            prefs.edit { saveBlacklist(this) }
        }
    }

    override val userSize: Int
        get() = users.size

    override fun checkBlackList(authorId: String?): Boolean = blacklist.any { it.userId == authorId }

    override val blackList: List<User>
        get() = blacklist

    override fun removeAllBlackList() {
        blacklist.clear()
        prefs.edit { saveBlacklist(this) }
    }

    override fun putAvatarUrl(uid: String?, url: String?) {
        if (!url.isNullOrEmpty()) {
            avatarPreferences.edit { putString(uid, url) }
        }
    }

    override fun putAvatarUrl(info: ThreadData) {
        val rows = info.rowList ?: return
        avatarPreferences.edit {
            for (row in rows) {
                val uid = row.authorid.toString()
                val url = row.js_escap_avatar
                if (uid != "0" && !url.isNullOrEmpty()) {
                    putString(uid, url)
                    setAvatarUrl(row.authorid, url)
                }
            }
        }
    }

    override fun getAvatarUrl(uid: String?): String =
        if (uid.isNullOrEmpty() || uid == "0") "" else avatarPreferences.getString(uid, "").orEmpty()

    override fun clearAvatarUrl() {
        avatarPreferences.edit { clear() }
    }

    companion object {

        private val INSTANCE = UserManagerImpl()

        @JvmStatic
        fun getInstance(): UserManager = INSTANCE
    }
}
