package gov.anzong.androidnga.mvvm.viewmodel

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.coroutines.NgaDispatchers
import gov.anzong.androidnga.mvvm.model.ArticleListRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.account.UserManager
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.network.bean.ThreadData
import gov.anzong.androidnga.network.bean.ThreadRowInfo
import gov.anzong.androidnga.mvvm.model.convert.ErrorConvertFactory
import gov.anzong.androidnga.mvvm.model.param.ArticleListParam
import gov.anzong.androidnga.mvvm.model.param.ParamKey

/**
 * 主题内容的一页：加载（网络或离线缓存）、缓存本页与屏蔽楼层作者。
 * 请求随 ViewModel 结束取消；页面重建时保留已加载的内容，不重复请求。
 */
class ArticleListViewModel(
    val request: ArticleListParam,
    @set:VisibleForTesting var pageLoader: suspend (ArticleListParam) -> ThreadData = ArticleListRepository::loadPage,
    private val cacheLoader: (ArticleListParam) -> ThreadData = ArticleListRepository::loadCache,
    private val cacheWriter: (ArticleListParam, String?) -> Unit = ArticleListRepository::saveCache,
    private val users: () -> UserManager = UserManagerImpl::getInstance,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
    private val notify: (message: String, success: Boolean) -> Unit = { message, success ->
        if (success) ToastUtils.success(message) else ToastUtils.error(message)
    },
) : ViewModel() {

    /** 由 Fragment 的 arguments（[ParamKey.KEY_PARAM]）创建；页码至少为 1。 */
    constructor(state: SavedStateHandle) : this(requireNotNull(state.get<ArticleListParam>(ParamKey.KEY_PARAM)) { "缺少主题参数" })

    init {
        request.page = request.page.coerceAtLeast(1)
    }

    private val _data = MutableLiveData<ThreadData?>()
    private val _loading = MutableLiveData(false)
    private val _error = MutableLiveData<String?>()

    /** 最近一次加载成功的内容。 */
    val data: LiveData<ThreadData?> get() = _data
    val loading: LiveData<Boolean> get() = _loading
    /** 最近一次加载失败的原因；重新加载时清空。 */
    val error: LiveData<String?> get() = _error

    private var job: Job? = null
    private var started = false

    /** 首次进入时加载；页面重建时不重复加载。 */
    fun start() {
        if (started) return
        started = true
        load()
    }

    /** 重新加载本页；正在加载时不重复请求。 */
    fun load() {
        if (job?.isActive == true) return
        _error.value = null
        _loading.value = true
        job = viewModelScope.launch {
            try {
                _data.value = withContext(worker) {
                    if (request.loadCache) cacheLoader(request) else pageLoader(request)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = if (request.loadCache) "读取缓存失败！" else ErrorConvertFactory.getErrorMessage(e)
            } finally {
                _loading.value = false
            }
        }
    }

    /** 缓存已加载的本页内容，供离线阅读。 */
    fun saveCache() {
        val raw = _data.value?.rawData ?: return
        if (request.topicInfo.isNullOrEmpty()) {
            notify("缓存失败！", false)
            return
        }
        viewModelScope.launch {
            val saved = try {
                withContext(worker) { cacheWriter(request, raw) }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (saved) notify("缓存成功！", true) else notify("缓存失败！", false)
        }
    }

    /** 屏蔽或取消屏蔽楼层作者，返回提示文字；匿名楼层不能屏蔽。 */
    @StringRes
    fun toggleBlacklist(row: ThreadRowInfo): Int {
        if (row.isAnonymous) return R.string.cannot_add_to_blacklist_cause_anony
        val users = users()
        return if (row.isInBlackList) {
            row.isInBlackList = false
            users.removeFromBlackList(row.authorid.toString())
            R.string.remove_from_blacklist_success
        } else {
            row.isInBlackList = true
            users.addToBlackList(row.author, row.authorid.toString())
            R.string.add_to_blacklist_success
        }
    }
}
