package gov.anzong.androidnga.network.favorites

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonArray
import gov.anzong.androidnga.base.json.NgaJsonObject
import gov.anzong.androidnga.coroutines.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.network.article.ArticleResponse

/** 论坛端版块收藏及其按账号保存的离线缓存。 */
data class ForumFavoriteBoard(val fid: Int, val stid: Int, val name: String, val head: String? = null) {
    val serverId get() = (if (stid != 0) stid else fid).also { require(it != 0) { "版块编号无效。" } }.toString()
    val key get() = if (stid != 0) "stid:$stid" else "fid:$fid"
}

object ForumFavoritesParser {
    fun parseResponse(source: String): List<ForumFavoriteBoard> {
        val root = NgaJson.parseObject(source.trim().removePrefix("/*\$js\$*/"))
            ?: throw IllegalArgumentException("论坛收藏响应为空，请稍后重试。")
        ArticleResponse.businessError(root)?.let { throw IllegalArgumentException(it) }
        val data = root["data"] ?: throw IllegalArgumentException("无法读取论坛收藏，请稍后重试。")
        if (data is NgaJsonObject) ArticleResponse.businessError(data)?.let { throw IllegalArgumentException(it) }
        val rows = when (data) {
            is NgaJsonObject -> data["0"]
            is NgaJsonArray -> data.firstOrNull()
            else -> null
        } ?: throw IllegalArgumentException("论坛收藏数据格式不兼容，请稍后重试。")
        return parseRows(rows)
    }

    fun mutationFields(board: ForumFavoriteBoard, add: Boolean): Map<String, String> =
        mapOf("action" to if (add) "add" else "del", "fid" to board.serverId)

    fun parseMutationResponse(source: String) {
        val root = NgaJson.parseObject(source.trim().removePrefix("/*\$js\$*/"))
            ?: throw IllegalArgumentException("收藏操作响应为空，请重试。")
        ArticleResponse.businessError(root)?.let { throw IllegalArgumentException(it) }
        val data = root["data"] ?: throw IllegalArgumentException("未收到论坛收藏操作结果，请重试。")
        if (data is NgaJsonObject) ArticleResponse.businessError(data)?.let { message ->
            val normalized = message.trim().trimEnd('!', '！', '。')
            if (normalized !in setOf("操作成功", "收藏成功", "添加成功", "删除成功", "取消收藏成功"))
                throw IllegalArgumentException(message)
        }
    }

    fun parseCache(source: String): List<ForumFavoriteBoard> = parseRows(NgaJson.parseArray(source))

    fun serialize(boards: List<ForumFavoriteBoard>): String = NgaJson.toJSONString(boards.map {
        linkedMapOf("fid" to it.fid, "stid" to it.stid, "name" to it.name, "topped_topic" to it.head)
    })

    /** 接口响应与离线缓存共用。 */
    private fun parseRows(rows: Any?): List<ForumFavoriteBoard> {
        val values = when (rows) {
            is Map<*, *> -> rows.values.toList()
            is List<*> -> rows.toList()
            else -> throw IllegalArgumentException("论坛收藏数据格式不兼容，请稍后重试。")
        }
        return values.map { entry ->
            val board = entry as? Map<*, *> ?: throw IllegalArgumentException("论坛收藏条目格式不兼容，请稍后重试。")
            val fid = board.text("fid")?.toIntOrNull() ?: 0
            val stid = board.text("stid")?.toIntOrNull() ?: 0
            val name = board.text("name")?.trim().orEmpty()
            require((fid != 0 || stid != 0) && name.isNotEmpty()) { "论坛收藏条目不完整，请稍后重试。" }
            ForumFavoriteBoard(fid, stid, name, board.text("topped_topic")?.takeIf { it.isNotBlank() })
        }.distinctBy { it.key }
    }

    /** 与 getString 一致：对象与数组取 JSON 文本，其余取字符串形式。 */
    private fun Map<*, *>.text(key: String): String? = this[key]?.toString()
}

interface ForumFavoritesCache {
    fun read(uid: String): List<ForumFavoriteBoard>
    fun write(uid: String, boards: List<ForumFavoriteBoard>)
}

data class ForumFavoritesSnapshot(
    val accountId: String = "",
    val boards: List<ForumFavoriteBoard> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val mutating: Boolean = false
)

/**
 * 账号切换会立即取消旧请求，失败时保留该账号的上次同步结果。
 *
 * 请求在 [scope] 中启动，结果在其调度器上处理；[fetch] 与 [write] 自行切换到后台线程。
 */
class ForumFavoriteSync(
    private val fetch: suspend (String) -> List<ForumFavoriteBoard>,
    private val cache: ForumFavoritesCache,
    private val onState: (ForumFavoritesSnapshot) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
    private val write: suspend (String, ForumFavoriteBoard, Boolean) -> Unit = { _, _, _ ->
        throw IllegalStateException("收藏写入不可用。")
    },
    private val scope: CoroutineScope = AppScope,
) {
    var state = ForumFavoritesSnapshot()
        private set
    private var cookie = ""
    private var request: Job? = null
    private var mutation: Job? = null
    private var generation = 0
    private var lastSuccess = 0L

    fun refresh(uid: String, currentCookie: String, force: Boolean = false) {
        if (uid != state.accountId || currentCookie != cookie) {
            generation++
            request?.cancel()
            request = null
            mutation?.cancel()
            mutation = null
            cookie = currentCookie
            lastSuccess = 0
            publish(ForumFavoritesSnapshot(uid, if (uid.isEmpty()) emptyList() else cache.read(uid)))
        }
        if (uid.isEmpty() || state.mutating) return
        if (!force && (state.loading || (lastSuccess != 0L && clock() - lastSuccess < 60_000))) return
        val ticket = ++generation
        request?.cancel()
        publish(state.copy(loading = true, error = null))
        request = scope.launch {
            val boards = try {
                fetch(currentCookie)
            } catch (e: CancellationException) {
                throw e
            } catch (failure: Exception) {
                if (ticket == generation && state.accountId == uid) {
                    publish(state.copy(loading = false, error = failure.message ?: "论坛收藏同步失败，请稍后重试。"))
                }
                return@launch
            }
            if (ticket == generation && state.accountId == uid) {
                cache.write(uid, boards)
                lastSuccess = clock()
                publish(state.copy(boards = boards, loading = false, error = null))
            }
        }
    }

    fun change(uid: String, currentCookie: String, board: ForumFavoriteBoard, add: Boolean,
               onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (uid.isEmpty() || currentCookie.isBlank()) { onError("请先登录后再操作论坛收藏。"); return }
        runCatching { board.serverId }.onFailure { onError(it.message ?: "版块编号无效。"); return }
        if (uid != state.accountId || currentCookie != cookie) {
            generation++
            request?.cancel()
            mutation?.cancel()
            cookie = currentCookie
            lastSuccess = 0
            publish(ForumFavoritesSnapshot(uid, cache.read(uid)))
        }
        if (state.mutating) { onError("收藏操作正在处理中，请稍候。"); return }
        val ticket = ++generation
        request?.cancel()
        lastSuccess = 0
        publish(state.copy(loading = false, error = null, mutating = true))
        mutation = scope.launch {
            try {
                write(currentCookie, board, add)
            } catch (e: CancellationException) {
                throw e
            } catch (failure: Exception) {
                if (ticket == generation && state.accountId == uid) {
                    val message = failure.message ?: "论坛收藏操作失败，请重试。"
                    publish(state.copy(mutating = false, error = message))
                    onError(message)
                }
                return@launch
            }
            if (ticket == generation && state.accountId == uid) {
                val boards = if (add) (state.boards + board).distinctBy { it.key }
                    else state.boards.filterNot { it.key == board.key }
                cache.write(uid, boards)
                publish(state.copy(boards = boards, mutating = false, error = null))
                onSuccess()
            }
        }
    }

    fun close() { generation++; request?.cancel(); mutation?.cancel() }
    private fun publish(value: ForumFavoritesSnapshot) { state = value; onState(value) }
}
