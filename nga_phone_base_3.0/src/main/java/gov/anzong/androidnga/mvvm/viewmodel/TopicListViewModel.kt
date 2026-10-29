package gov.anzong.androidnga.mvvm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import gov.anzong.androidnga.mvvm.model.TopicListRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import sp.phone.mvp.model.BoardModel
import sp.phone.mvp.model.BoardStore
import sp.phone.mvp.model.convert.ErrorConvertFactory
import sp.phone.mvp.model.entity.Board
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.mvp.model.entity.TopicListInfo
import sp.phone.param.ParamKey
import sp.phone.param.TopicListParam

/**
 * 版面、搜索、收藏与 24 小时热帖的主题列表。
 *
 * 列表请求随 ViewModel 结束取消，刷新会取消进行中的翻页；取消收藏是写操作，在 [AppScope] 中执行。
 * 每次结果都是新的对象：[firstPage] 为第一页（替换列表），[nextPage] 为之后各页（追加）。
 */
class TopicListViewModel(
    val param: TopicListParam,
    private val loadPage: suspend (page: Int, param: TopicListParam) -> TopicListInfo = TopicListRepository()::loadPage,
    private val removeFavorite: suspend (ThreadPageInfo) -> Boolean = TopicListRepository()::removeFavorite,
    private val boards: BoardStore = BoardModel.getInstance(),
    private val writeScope: CoroutineScope = AppScope,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
    private val notify: (String) -> Unit = { ToastUtils.show(it) },
) : ViewModel() {

    /** 由 Intent extras 中的 [ParamKey.KEY_PARAM] 创建。 */
    constructor(state: SavedStateHandle) : this(requireNotNull(state.get<TopicListParam>(ParamKey.KEY_PARAM)) { "缺少主题列表参数" })

    private val _firstPage = MutableLiveData<TopicListInfo>()
    private val _nextPage = MutableLiveData<TopicListInfo>()
    private val _refreshing = MutableLiveData(false)
    private val _error = MutableLiveData<String?>()
    private val _removedTopic = MutableLiveData<ThreadPageInfo>()

    val firstPage: LiveData<TopicListInfo> get() = _firstPage
    val nextPage: LiveData<TopicListInfo> get() = _nextPage
    val refreshing: LiveData<Boolean> get() = _refreshing
    val error: LiveData<String?> get() = _error
    val removedTopic: LiveData<ThreadPageInfo> get() = _removedTopic

    private var job: Job? = null
    private var generation = 0
    private var started = false

    /** 24 小时热帖：一次取回的全部主题，及已显示的条数。 */
    private var hotTopics = emptyList<ThreadPageInfo>()
    private var hotShown = 0

    /** 首次进入时加载第一页；页面重建时不重复加载。 */
    fun start() {
        if (started) return
        started = true
        refresh()
    }

    fun refresh() {
        load { if (param.twentyfour == 1) firstHotPage() else _firstPage.value = loadPage(1, param) }
    }

    fun loadNextPage(page: Int) {
        if (job?.isActive == true) return
        if (param.twentyfour == 1) {
            _nextPage.value = nextHotPage()
            return
        }
        load { _nextPage.value = loadPage(page, param) }
    }

    private fun load(block: suspend () -> Unit) {
        job?.cancel()
        val current = ++generation
        _refreshing.value = true
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = ErrorConvertFactory.getErrorMessage(e)
            } finally {
                if (current == generation) _refreshing.value = false
            }
        }
    }

    private suspend fun loadPage(page: Int, param: TopicListParam) = withContext(worker) { loadPage.invoke(page, param) }

    private suspend fun firstHotPage() {
        val pages = (1..HOT_PAGE_COUNT).map { loadPage(it, param) }
        hotTopics = hotTopics(pages)
        hotShown = 0
        _firstPage.value = nextHotPage()
    }

    private fun nextHotPage(): TopicListInfo {
        val end = (hotShown + HOT_PAGE_SIZE).coerceAtMost(hotTopics.size)
        val info = TopicListInfo().apply { threadPageList = hotTopics.subList(hotShown, end).toMutableList() }
        hotShown = end
        return info
    }

    /** 取消收藏主题；成功后通过 [removedTopic] 通知页面移除。 */
    fun removeFavorite(info: ThreadPageInfo) {
        writeScope.launch {
            val removed = try {
                withContext(worker) { removeFavorite.invoke(info) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (removed) {
                notify("操作成功！")
                _removedTopic.value = info
            } else {
                _error.value = "操作失败!"
            }
        }
    }

    fun errorShown() {
        _error.value = null
    }

    val isBookmarkBoard get() = boards.isBookmark(param.fid, param.stid)

    fun addBookmarkBoard(board: Board) = boards.addBookmark(board)

    fun removeBookmarkBoard() = boards.removeBookmark(param.fid, param.stid)

    companion object {
        /** 24 小时热帖请求的页数、最多显示的主题数和每次显示的条数。 */
        const val HOT_PAGE_COUNT = 5
        const val HOT_TOPIC_COUNT = 50
        const val HOT_PAGE_SIZE = 20

        /** 合并各页，只保留最后一页服务器时间前 24 小时内发布的主题，按回复数从多到少取前 [HOT_TOPIC_COUNT] 个。 */
        fun hotTopics(pages: List<TopicListInfo>): List<ThreadPageInfo> {
            val now = pages.lastOrNull()?.curTime ?: return emptyList()
            return pages.flatMap { it.threadPageList.orEmpty() }
                .filter { now - it.postDate <= 24 * 60 * 60 }
                .distinctBy { it.tid }
                .sortedByDescending { it.replies }
                .take(HOT_TOPIC_COUNT)
        }
    }
}
