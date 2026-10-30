package gov.anzong.androidnga.mvvm.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * 主题阅读页与各分页之间共享的状态与请求。
 * @author yangyihang
 */
class ArticleShareViewModel : ViewModel() {

    class ReaderControls(@JvmField val page: Int, @JvmField val expanded: Boolean)

    private val _readerControls = MutableLiveData<ReaderControls>()
    val readerControls: LiveData<ReaderControls> get() = _readerControls

    private val _topicId = MutableLiveData<Int>()
    val topicId: LiveData<Int> get() = _topicId

    private val _title = MutableLiveData<String?>()
    val title: LiveData<String?> get() = _title

    private val _replyCount = MutableLiveData<Int>()
    val replyCount: LiveData<Int> get() = _replyCount

    private val _refreshPage = MutableLiveData<Int>()
    val refreshPage: LiveData<Int> get() = _refreshPage

    private val _cachePage = MutableLiveData<Int>()
    val cachePage: LiveData<Int> get() = _cachePage

    private val _topicOwner = MutableLiveData<String?>()
    val topicOwner: LiveData<String?> get() = _topicOwner

    fun setReaderControls(page: Int, expanded: Boolean) {
        _readerControls.value = ReaderControls(page, expanded)
    }

    fun setTopicId(tid: Int) {
        _topicId.value = tid
    }

    fun setTitle(title: String?) {
        _title.value = title
    }

    fun setReplyCount(replyCount: Int) {
        _replyCount.value = replyCount
    }

    fun setRefreshPage(refreshPage: Int) {
        _refreshPage.value = refreshPage
    }

    fun setCachePage(cachePage: Int) {
        _cachePage.value = cachePage
    }

    fun setTopicOwner(owner: String?) {
        _topicOwner.value = owner
    }
}
