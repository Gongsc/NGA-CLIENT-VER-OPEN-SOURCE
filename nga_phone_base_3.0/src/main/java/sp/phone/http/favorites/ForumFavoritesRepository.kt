package sp.phone.http.favorites

import android.content.Context
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.base.util.ToastUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.nio.charset.Charset
import sp.phone.common.UserManagerImpl
import sp.phone.util.ForumUtils
import sp.phone.http.retrofit.RetrofitHelper

/** 首页、目录和版块菜单共享同一份论坛收藏状态。 */
class ForumFavoritesRepository private constructor() {
    private val preferences = ContextUtils.getContext().getSharedPreferences("forum_favorites", Context.MODE_PRIVATE)
    private val updates = BehaviorSubject.createDefault(ForumFavoritesSnapshot())
    val changes: Observable<ForumFavoritesSnapshot> get() = updates.hide()
    private val cache = object : ForumFavoritesCache {
        override fun read(uid: String): List<ForumFavoriteBoard> = runCatching {
            ForumFavoritesParser.parseCache(preferences.getString("boards_$uid", "[]")!!)
        }.getOrDefault(emptyList())
        override fun write(uid: String, boards: List<ForumFavoriteBoard>) {
            preferences.edit().putString("boards_$uid", ForumFavoritesParser.serialize(boards)).apply()
        }
    }
    private val sync = ForumFavoriteSync(fetch = { cookie ->
        request(cookie, mapOf("action" to "get")).map(ForumFavoritesParser::parseResponse)
            .observeOn(AndroidSchedulers.mainThread())
    }, cache = cache, onState = updates::onNext, write = { cookie, board, add ->
        request(cookie, ForumFavoritesParser.mutationFields(board, add)).map {
            ForumFavoritesParser.parseMutationResponse(it)
            Unit
        }.observeOn(AndroidSchedulers.mainThread())
    })

    private fun request(cookie: String, fields: Map<String, String>): Observable<String> {
        val domain = ForumUtils.getAvailableDomain().trimEnd('/')
        return RetrofitHelper.getInstance().service.forumFavorites(
            "$domain/nuke.php?__lib=forum_favor2&__act=forum_favor&__output=8",
            mapOf("Cookie" to cookie, "Referer" to "$domain/"), fields
        ).subscribeOn(Schedulers.io()).map { response ->
            response.use { body ->
                val charset = body.contentType()?.charset(Charset.forName("GB18030")) ?: Charset.forName("GB18030")
                String(body.bytes(), charset)
            }
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
