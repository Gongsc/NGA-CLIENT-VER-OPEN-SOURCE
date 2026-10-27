package gov.anzong.androidnga.mvvm.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.alibaba.fastjson.JSON
import gov.anzong.androidnga.Utils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import sp.phone.common.UserManagerImpl
import sp.phone.http.bean.MessageListInfo
import sp.phone.http.bean.MessageThreadPageInfo
import sp.phone.http.retrofit.RetrofitHelper
import sp.phone.mvp.model.convert.ForumNotificationFactory
import sp.phone.mvp.model.convert.MessageConvertFactory
import sp.phone.mvp.model.entity.RecentReplyInfo

/** 首页消息只接收当前账号、当前代次的响应；切换账号即清空旧内容。 */
class HomeInboxViewModel(
    private val currentAccount: () -> String = { UserManagerImpl.getInstance().userId.orEmpty() },
    private val currentCookie: () -> String = { UserManagerImpl.getInstance().cookie.orEmpty() },
    private val replyRequest: (String) -> Observable<List<RecentReplyInfo>> = ::requestHomeReplies,
    private val messageRequest: (String, Int) -> Observable<MessageListInfo> = ::requestHomeMessages,
    private val delivery: io.reactivex.Scheduler = AndroidSchedulers.mainThread(),
    private val worker: io.reactivex.Scheduler = Schedulers.io(),
) : ViewModel() {
    var replies by mutableStateOf(emptyList<RecentReplyInfo>()); private set
    var messages by mutableStateOf(emptyList<MessageThreadPageInfo>()); private set
    var repliesLoading by mutableStateOf(false); private set
    var messagesLoading by mutableStateOf(false); private set
    var replyError by mutableStateOf<String?>(null); private set
    var messageError by mutableStateOf<String?>(null); private set
    var nextPage by mutableIntStateOf(0); private set
    @Volatile private var account = ""
    @Volatile private var session = ""
    @Volatile private var generation = 0
    private var repliesLoaded = false
    private var messagesLoaded = false
    private var retryMore = false
    private val requests = CompositeDisposable()

    fun selectAccount(uid: String) {
        val cookie = if (uid.isEmpty()) "" else currentCookie()
        if (account == uid && session == cookie) return
        session = cookie
        account = uid
        generation++
        requests.clear()
        replies = emptyList(); messages = emptyList()
        replyError = null; messageError = null
        repliesLoading = false; messagesLoading = false
        repliesLoaded = false; messagesLoaded = false; nextPage = 0; retryMore = false
    }

    private fun current(token: Int, uid: String) = token == generation && uid == account && uid == currentAccount() && session == currentCookie()

    fun loadReplies(force: Boolean = false, onRead: () -> Unit = {}) {
        if (account.isEmpty() || repliesLoading || (repliesLoaded && !force)) return
        val token = generation; val uid = account; val cookie = session
        repliesLoading = true; replyError = null
        // 首次可能仅返回未读通知；再读取一次历史，合并时保留未读标记，不无限重试。
        fun query() = replyRequest(cookie)
        requests.add(query().subscribeOn(worker).flatMap { unread ->
            if (unread.any { it.isUnread } && current(token, uid)) query().map { history ->
                (unread + history).distinctBy { "${it.tidStr}:${it.pidStr}" }
            }.onErrorReturnItem(unread) else Observable.just(unread)
        }.observeOn(delivery).subscribe({ data ->
            if (current(token, uid)) {
                replies = data; repliesLoaded = true; repliesLoading = false; onRead()
            }
        }, { error ->
            runCatching { sp.phone.util.NLog.e("HomeInbox", "回复通知加载失败", error) }
            if (current(token, uid)) { repliesLoading = false; replyError = error.message ?: "回复通知加载失败" }
        }))
    }

    fun loadMessages(force: Boolean = false, more: Boolean = false) {
        if (account.isEmpty() || messagesLoading || (!force && !more && messagesLoaded) || (more && nextPage <= 0)) return
        val token = generation; val uid = account; val cookie = session
        val page = if (more) nextPage else 1
        retryMore = more
        messagesLoading = true; messageError = null
        requests.add(messageRequest(cookie, page).subscribeOn(worker).observeOn(delivery).subscribe({ data: MessageListInfo ->
            if (current(token, uid)) {
                messages = ((if (more) messages else emptyList()) + data.messageEntryList.orEmpty()).distinctBy { it.mid }
                nextPage = data.get__nextPage().takeIf { it > page } ?: 0
                messagesLoaded = true; messagesLoading = false
            }
        }, { error -> if (current(token, uid)) { messagesLoading = false; messageError = error.message ?: "短消息加载失败" } }))
    }
    fun retryMessages() = loadMessages(force = true, more = retryMore)

    override fun onCleared() { generation++; requests.dispose() }
}

private fun requestHomeReplies(cookie: String): Observable<List<RecentReplyInfo>> =
    RetrofitHelper.getInstance().service.get(Utils.getNGAHost() + "nuke.php?__lib=noti&__output=8&__act=get_all", mapOf("Cookie" to cookie))
        .map { content ->
            // 没有通知时服务端不一定返回 data.0，与旧通知页一致按空列表处理；只有明确的错误信息才视为失败。
            homeReplyError(content)?.let { throw IllegalStateException(it) }
            ForumNotificationFactory.buildRecentReplyList(content)
        }

internal fun homeReplyError(content: String): String? {
    val json = runCatching { JSON.parseObject(content.removePrefix("window.script_muti_get_var_store=")) }.getOrNull()
        ?: return if (content.isBlank()) "回复通知加载失败，请稍后重试" else null
    val error = json["error"] ?: return null
    return (error as? com.alibaba.fastjson.JSONObject)?.getString("0")
        ?: (error as? com.alibaba.fastjson.JSONArray)?.getString(0)
        ?: error.toString()
}

private fun requestHomeMessages(cookie: String, page: Int): Observable<MessageListInfo> =
    RetrofitHelper.getInstance().service.get(Utils.getNGAHost() + "nuke.php?__lib=message&__act=message&act=list&lite=js&page=$page", mapOf("Cookie" to cookie))
        .map { body ->
            val factory = MessageConvertFactory()
            factory.getMessageListInfo(body) ?: throw IllegalStateException(factory.errorMsg ?: "短消息加载失败")
        }
