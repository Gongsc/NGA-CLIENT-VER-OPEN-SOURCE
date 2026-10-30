package gov.anzong.androidnga.network.favorites

import android.content.Context
import androidx.core.content.edit
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.coroutines.NgaDispatchers
import gov.anzong.androidnga.network.retrofit.NgaApi
import gov.anzong.androidnga.util.ForumUtils
import java.nio.charset.Charset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** 首页、目录和版块菜单共享同一份论坛收藏状态。 */
class ForumFavoritesRepository private constructor() {
    private val preferences = ContextUtils.getContext().getSharedPreferences("forum_favorites", Context.MODE_PRIVATE)
    private val updates = MutableStateFlow(ForumFavoritesSnapshot())
    val changes: StateFlow<ForumFavoritesSnapshot> get() = updates.asStateFlow()
    private val cache = object : ForumFavoritesCache {
        override fun read(uid: String): List<ForumFavoriteBoard> = runCatching {
            ForumFavoritesParser.parseCache(preferences.getString("boards_$uid", "[]")!!)
        }.getOrDefault(emptyList())
        override fun write(uid: String, boards: List<ForumFavoriteBoard>) {
            preferences.edit { putString("boards_$uid", ForumFavoritesParser.serialize(boards)) }
        }
    }
    private val sync = ForumFavoriteSync(fetch = { cookie ->
        request(cookie, mapOf("action" to "get"), ForumFavoritesParser::parseResponse)
    }, cache = cache, onState = { updates.value = it }, write = { cookie, board, add ->
        request(cookie, ForumFavoritesParser.mutationFields(board, add), ForumFavoritesParser::parseMutationResponse)
    })

    /** 请求与解析都在后台线程执行。 */
    private suspend fun <T> request(cookie: String, fields: Map<String, String>, parse: (String) -> T): T =
        withContext(NgaDispatchers.io) {
            val domain = ForumUtils.getAvailableDomain().trimEnd('/')
            NgaApi.get().forumFavorites(
                "$domain/nuke.php?__lib=forum_favor2&__act=forum_favor&__output=8",
                mapOf("Cookie" to cookie, "Referer" to "$domain/"), fields
            ).use { body ->
                val charset = body.contentType()?.charset(Charset.forName("GB18030")) ?: Charset.forName("GB18030")
                parse(String(body.bytes(), charset))
            }
        }

    fun current(): ForumFavoritesSnapshot {
        val uid = UserManagerImpl.getInstance().userId.orEmpty()
        return if (sync.state.accountId == uid) sync.state
            else ForumFavoritesSnapshot(uid, if (uid.isEmpty()) emptyList() else cache.read(uid))
    }

    @JvmOverloads fun refresh(force: Boolean = false) {
        val users = UserManagerImpl.getInstance()
        sync.refresh(users.userId.orEmpty(), users.cookie.orEmpty(), force)
    }

    fun change(board: ForumFavoriteBoard, add: Boolean) {
        val users = UserManagerImpl.getInstance()
        sync.change(users.userId.orEmpty(), users.cookie.orEmpty(), board, add, onSuccess = {
            ToastUtils.success(if (add) "已收藏到论坛" else "已取消论坛收藏")
            refresh(true)
        }, onError = ToastUtils::error)
    }

    fun add(fid: Int, stid: Int, name: String, head: String?) = change(ForumFavoriteBoard(fid, stid, name, head), true)
    fun remove(fid: Int, stid: Int) = change(ForumFavoriteBoard(fid, stid, ""), false)
    fun contains(fid: Int, stid: Int): Boolean {
        val key = ForumFavoriteBoard(fid, stid, "").key
        return current().boards.any { it.key == key }
    }

    companion object {
        private val sharedRepository by lazy { ForumFavoritesRepository() }
        @JvmStatic fun getInstance() = sharedRepository
    }
}
