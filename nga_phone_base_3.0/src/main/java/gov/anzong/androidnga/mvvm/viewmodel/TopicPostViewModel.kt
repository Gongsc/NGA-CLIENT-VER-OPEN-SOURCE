package gov.anzong.androidnga.mvvm.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import gov.anzong.androidnga.mvvm.model.NgaTopicPostRepository
import gov.anzong.androidnga.mvvm.model.TopicPostRepository
import gov.anzong.androidnga.mvvm.model.UploadImage
import gov.anzong.androidnga.mvvm.model.UploadResult
import gov.anzong.androidnga.ui.compose.PostEditorState
import gov.anzong.androidnga.ui.compose.insertEditorText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.mvvm.model.param.PostParam

/**
 * 发帖、回复与编辑页：编辑内容、预检得到的 auth 与已上传的附件都保存在这里，页面重建后保留。
 *
 * 提交在 [AppScope] 中执行，离开页面也不会中途放弃（写操作已发出后取消只会让结果不明）；
 * 预检、分类与附件上传随 ViewModel 结束取消。
 */
class TopicPostViewModel(
    private val param: PostParam,
    private val repository: TopicPostRepository = NgaTopicPostRepository(),
    private val submitScope: CoroutineScope = AppScope,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
    private val notify: (String) -> Unit = { ToastUtils.info(it) },
) : ViewModel() {

    /** 由 Fragment 的 arguments（"param"）创建。 */
    constructor(state: SavedStateHandle) : this(requireNotNull(state.get<PostParam>("param")) { "缺少发帖参数" })

    val editor = PostEditorState()

    val action: String get() = param.postAction.orEmpty()

    /** 提交成功，页面应关闭。 */
    var posted by mutableStateOf(false)
        private set

    private var started = false

    /**
     * 首次进入时填入编辑内容并开始预检；页面重建时不做任何事。
     * 进程被回收后重新进入时，[title]、[body] 等为保存的编辑内容，[cursor] 小于 0 表示正文末尾。
     */
    fun start(title: String?, body: String?, cursor: Int, anonymous: Boolean) {
        if (started) return
        started = true
        editor.originalTitle = param.postSubject.orEmpty()
        editor.originalBody = param.postContent.orEmpty()
        editor.title = TextFieldValue(title ?: editor.originalTitle)
        val text = body ?: editor.originalBody
        editor.body = TextFieldValue(text, TextRange(if (cursor < 0) text.length else cursor.coerceAtMost(text.length)))
        editor.anonymous = anonymous
        prepare()
    }

    /** 预检取得 auth；失败时可重试。 */
    fun prepare() {
        editor.error = null
        editor.preparing = true
        viewModelScope.launch {
            try {
                param.authCode = withContext(worker) { repository.prepare(param) }
                editor.attachmentReady = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                editor.attachmentReady = false
                editor.error = e.message ?: "无法获取发帖信息，请稍后重试"
            }
            editor.preparing = false
        }
    }

    fun submit() {
        if (editor.submitting) return
        val title = editor.title.text
        val body = editor.body.text
        if (body.isBlank() || (param.postAction == "new" && title.isBlank())) {
            editor.error = "请填写标题及正文后提交。"
            return
        }
        editor.submitting = true
        editor.error = null
        param.isAnonymous = editor.anonymous
        param.postSubject = title
        param.postContent = body
        submitScope.launch {
            try {
                withContext(worker) { repository.submit(param) }
                posted = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                editor.error = e.message ?: "提交失败，请稍后重试。"
            } finally {
                editor.submitting = false
            }
        }
    }

    /** 上传图片并在光标处插入；超过服务器限制时压缩后重试一次。 */
    fun upload(image: UploadImage) {
        editor.uploading = true
        editor.error = null
        viewModelScope.launch {
            try {
                var result = withContext(worker) { repository.upload(image.read(compress = false), param) }
                if (result == UploadResult.TooLarge) {
                    notify("附件过大，无法上传，重新进行压缩并上传")
                    result = withContext(worker) { repository.upload(image.read(compress = true), param) }
                }
                when (result) {
                    is UploadResult.Uploaded -> {
                        param.appendAttachment(result.attachments, result.attachmentsCheck)
                        editor.body = insertEditorText(editor.body, "\n[img]./${result.url}[/img]\n")
                        notify("上传成功")
                    }
                    UploadResult.TooLarge -> editor.error = "附件过大，无法上传"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                editor.error = e.message ?: "上传图片失败，请稍后重试"
            } finally {
                editor.uploading = false
            }
        }
    }

    fun loadCategories() {
        if (editor.categoryLoading) return
        editor.categoryLoading = true
        viewModelScope.launch {
            try {
                editor.categories = withContext(worker) { repository.loadCategories(param.postFid) }.distinct()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                editor.error = e.message ?: "无法加载主题分类"
            } finally {
                editor.categoryLoading = false
            }
        }
    }
}
