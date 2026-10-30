package gov.anzong.androidnga.mvvm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.network.bean.MessageDetailInfo
import gov.anzong.androidnga.network.retrofit.NgaApi
import gov.anzong.androidnga.mvvm.model.convert.MessageConvertFactory

/**
 * 短消息详情：按页累积会话内容，页面重建后保留已加载的页；请求随 ViewModel 结束取消。
 *
 * 刷新会取消正在进行的翻页，旧请求的结果不会混入新列表。
 */
class MessageDetailViewModel(
    val mid: Int,
    private val request: suspend (page: Int, mid: Int) -> MessageDetailInfo = ::requestMessageDetail,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
) : ViewModel() {

    /** 由 Fragment 的 arguments（"mid"）创建。 */
    constructor(state: SavedStateHandle) : this(state.get<Int>("mid") ?: 0)

    private val _pages = MutableLiveData<List<MessageDetailInfo>>(emptyList())
    private val _loading = MutableLiveData(false)
    private val _error = MutableLiveData<String?>()
    private var job: Job? = null
    /** 每次请求加一，只有最新一次请求能结束加载状态。 */
    private var generation = 0

    /** 已加载的页，按页码顺序。 */
    val pages: LiveData<List<MessageDetailInfo>> get() = _pages
    val loading: LiveData<Boolean> get() = _loading
    /** 未提示过的错误；空字符串表示网络错误。提示后调用 [errorShown]。 */
    val error: LiveData<String?> get() = _error

    val isLoaded get() = _pages.value.orEmpty().isNotEmpty()

    val hasNextPage get() = _pages.value.orEmpty().lastOrNull()?.let { it.__nextPage > 0 } ?: true

    val recipient: String? get() = _pages.value.orEmpty().lastOrNull()?._Alluser

    val title: String? get() = _pages.value.orEmpty().lastOrNull()?._Title

    fun refresh() = load(1)

    fun loadNextPage() {
        if (job?.isActive == true || !hasNextPage) return
        load(_pages.value.orEmpty().size + 1)
    }

    fun errorShown() {
        _error.value = null
    }

    private fun load(page: Int) {
        job?.cancel()
        val current = ++generation
        _loading.value = true
        job = viewModelScope.launch {
            try {
                val info = withContext(worker) { request(page, mid) }
                val loaded = if (page == 1) emptyList() else _pages.value.orEmpty()
                _pages.value = loaded + info
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.message.orEmpty()
            } finally {
                if (current == generation) _loading.value = false
            }
        }
    }
}

/** nuke.php?__lib=message&__act=message&act=read&page=1&mid=1&lite=js */
private suspend fun requestMessageDetail(page: Int, mid: Int): MessageDetailInfo {
    val params = mapOf(
        "__lib" to "message", "__act" to "message", "act" to "read", "lite" to "js",
        "page" to page.toString(), "mid" to mid.toString(),
    )
    val factory = MessageConvertFactory()
    return factory.getMessageDetailInfo(NgaApi.get().get(params), page) ?: throw Exception(factory.errorMsg)
}
