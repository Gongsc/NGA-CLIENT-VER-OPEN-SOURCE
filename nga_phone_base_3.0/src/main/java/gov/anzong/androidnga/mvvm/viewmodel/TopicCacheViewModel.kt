package gov.anzong.androidnga.mvvm.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import gov.anzong.androidnga.mvvm.model.TopicListRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.mvvm.model.entity.TopicListInfo

/** 离线缓存的主题：读取、删除、导入与导出，文件操作都在后台线程执行。 */
class TopicCacheViewModel(
    private val loadCache: () -> TopicListInfo = TopicListRepository()::loadCache,
    private val deleteCache: (tid: Int) -> Boolean = TopicListRepository()::deleteCache,
    private val exportCache: () -> Unit = TopicListRepository()::exportCache,
    private val importCache: (Uri) -> Unit = TopicListRepository()::importCache,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
) : ViewModel() {

    /** 提示消息；[success] 为 false 时以错误样式提示。 */
    class Notice(@JvmField val text: String, @JvmField val success: Boolean)

    private val _topics = MutableLiveData<TopicListInfo>()
    private val _loading = MutableLiveData(false)
    private val _removedTopic = MutableLiveData<ThreadPageInfo>()
    private val _notice = MutableLiveData<Notice?>()

    val topics: LiveData<TopicListInfo> get() = _topics
    val loading: LiveData<Boolean> get() = _loading
    val removedTopic: LiveData<ThreadPageInfo> get() = _removedTopic
    /** 未提示的消息，提示后调用 [noticeShown]。 */
    val notice: LiveData<Notice?> get() = _notice

    private var started = false

    /** 首次进入时读取缓存；页面重建时不重复读取。 */
    fun start() {
        if (started) return
        started = true
        reload()
    }

    fun reload() {
        _loading.value = true
        launchFileWork({ loadCache() }, onFailure = "读取缓存失败！", endsLoading = true) { _topics.value = it }
    }

    fun delete(info: ThreadPageInfo) {
        launchFileWork({ deleteCache(info.tid) }, onFailure = "删除失败！") { deleted ->
            if (deleted) {
                _notice.value = Notice("删除成功！", true)
                _removedTopic.value = info
            } else {
                _notice.value = Notice("删除失败！", false)
            }
        }
    }

    fun exportFile() {
        launchFileWork({ exportCache() }, onFailure = "导出失败") {
            _notice.value = Notice("已导出至下载目录的 NGA 文件夹", true)
        }
    }

    fun importFile(uri: Uri) {
        launchFileWork({ importCache(uri) }, onFailure = null) {
            _notice.value = Notice("导入成功！！", true)
            reload()
        }
    }

    fun noticeShown() {
        _notice.value = null
    }

    /** 在后台执行文件操作；失败时提示 [onFailure]（为 null 时提示异常消息）。 */
    private fun <T> launchFileWork(work: () -> T, onFailure: String?, endsLoading: Boolean = false, onSuccess: (T) -> Unit) {
        viewModelScope.launch {
            try {
                onSuccess(withContext(worker) { work() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _notice.value = Notice(onFailure ?: e.message ?: "导入失败", false)
            } finally {
                if (endsLoading) _loading.value = false
            }
        }
    }
}
