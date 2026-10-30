package gov.anzong.androidnga.mvvm.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonArray
import gov.anzong.androidnga.base.json.NgaJsonObject
import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.network.bean.MessageListInfo
import gov.anzong.androidnga.network.bean.MessageThreadPageInfo
import gov.anzong.androidnga.network.retrofit.NgaApi
import gov.anzong.androidnga.mvvm.model.convert.ForumNotificationFactory
import gov.anzong.androidnga.mvvm.model.convert.MessageConvertFactory
import gov.anzong.androidnga.task.ForumNotificationTask
import gov.anzong.androidnga.mvvm.model.entity.RecentReplyInfo

/** 首页消息只接收当前账号、当前代次的响应；切换账号即清空旧内容。 */
class HomeInboxViewModel(
    private val currentAccount: () -> String = { UserManagerImpl.getInstance().userId.orEmpty() },
    private val currentCookie: () -> String = { UserManagerImpl.getInstance().cookie.orEmpty() },
    private val replyRequest: suspend (String) -> List<RecentReplyInfo> = ::requestHomeReplies,
    private val messageRequest: suspend (String, Int) -> MessageListInfo = ::requestHomeMessages,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
    private val replyClear: () -> Unit = { ForumNotificationTask(null).clearAllNotification() },
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
    /** 当前账号的请求；切换账号时整体取消。 */
    /** 当前账号的请求作用域：切换账号时整体取消，作为 viewModelScope 的子作用域随 ViewModel 结束。 */
    private var requests = newRequestScope()

    private fun newRequestScope() = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))

    fun selectAccount(uid: String) {
        val cookie = if (uid.isEmpty()) "" else currentCookie()
        if (account == uid && session == cookie) return
        session = cookie
        account = uid
        generation++
        requests.cancel()
        requests = newRequestScope()
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
        requests.launch {
            val data = try {
                withContext(worker) {
                    val unread = replyRequest(cookie)
                    if (unread.any { it.isUnread } && current(token, uid)) {
                        try {
                            (unread + replyRequest(cookie)).distinctBy { "${it.tidStr}:${it.pidStr}" }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            unread
                        }
                    } else unread
                }
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                runCatching { gov.anzong.androidnga.util.NLog.e("HomeInbox", "回复通知加载失败", error) }
                if (current(token, uid)) { repliesLoading = false; replyError = error.message ?: "回复通知加载失败" }
                return@launch
            }
            if (current(token, uid)) {
                replies = newestFirst(data); repliesLoaded = true; repliesLoading = false; onRead()
            }
        }
    }

    fun loadMessages(force: Boolean = false, more: Boolean = false) {
        if (account.isEmpty() || messagesLoading || (!force && !more && messagesLoaded) || (more && nextPage <= 0)) return
        val token = generation; val uid = account; val cookie = session
        val page = if (more) nextPage else 1
        retryMore = more
        messagesLoading = true; messageError = null
        requests.launch {
            val data = try {
                withContext(worker) { messageRequest(cookie, page) }
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                if (current(token, uid)) { messagesLoading = false; messageError = error.message ?: "短消息加载失败" }
                return@launch
            }
            if (current(token, uid)) {
                messages = ((if (more) messages else emptyList()) + data.messageEntryList.orEmpty()).distinctBy { it.mid }
                nextPage = data.__nextPage.takeIf { it > page } ?: 0
                messagesLoaded = true; messagesLoading = false
            }
        }
    }
    fun retryMessages() = loadMessages(force = true, more = retryMore)

    /** 清空服务端的回复通知；清空请求不随页面取消，进行中的读取结果不再显示。 */
    fun clearReplies() {
        if (account.isEmpty()) return
        replyClear()
        generation++
        requests.cancel()
        requests = newRequestScope()
        replies = emptyList(); replyError = null; repliesLoading = false; repliesLoaded = true
    }

    override fun onCleared() { generation++; requests.cancel() }
}

/** 最新的通知在前；没有时间的排在最后，同一时间保持服务端顺序。 */
internal fun newestFirst(replies: List<RecentReplyInfo>) = replies.sortedByDescending { it.timeStamp?.toLongOrNull() ?: Long.MIN_VALUE }

private suspend fun requestHomeReplies(cookie: String): List<RecentReplyInfo> {
    val content = NgaApi.get().get(Utils.getNGAHost() + "nuke.php?__lib=noti&__output=8&__act=get_all", mapOf("Cookie" to cookie))
    // 没有通知时服务端不一定返回 data.0，与旧通知页一致按空列表处理；只有明确的错误信息才视为失败。
    homeReplyError(content)?.let { throw IllegalStateException(it) }
    return ForumNotificationFactory.buildRecentReplyList(content)
}

internal fun homeReplyError(content: String): String? {
    val json = runCatching { NgaJson.parseObject(content.removePrefix("window.script_muti_get_var_store=")) }.getOrNull()
        ?: return if (content.isBlank()) "回复通知加载失败，请稍后重试" else null
    val error = json["error"] ?: return null
    return (error as? NgaJsonObject)?.getString("0")
        ?: (error as? NgaJsonArray)?.getString(0)
        ?: error.toString()
}

private suspend fun requestHomeMessages(cookie: String, page: Int): MessageListInfo {
    val body = NgaApi.get().get(Utils.getNGAHost() + "nuke.php?__lib=message&__act=message&act=list&lite=js&page=$page", mapOf("Cookie" to cookie))
    val factory = MessageConvertFactory()
    return factory.getMessageListInfo(body) ?: throw IllegalStateException(factory.errorMsg ?: "短消息加载失败")
}
