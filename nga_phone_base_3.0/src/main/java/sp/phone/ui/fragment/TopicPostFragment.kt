package sp.phone.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.os.BundleCompat
import gov.anzong.androidnga.R
import gov.anzong.androidnga.http.OnHttpCallBack
import gov.anzong.androidnga.ui.compose.*
import sp.phone.mvp.contract.TopicPostContract
import sp.phone.mvp.presenter.TopicPostPresenter
import sp.phone.param.PostParam
import gov.anzong.androidnga.coroutines.UiEvent

class TopicPostFragment : BaseMvpFragment<TopicPostPresenter>(), TopicPostContract.View {
    private val state = PostEditorState()
    private lateinit var param: PostParam
    private var initializing = false
    private val selectImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { mPresenter.startUploadTask(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        param = BundleCompat.getParcelable(requireArguments(), "param", PostParam::class.java)!!
        state.originalTitle = param.postSubject.orEmpty()
        state.originalBody = param.postContent.orEmpty()
        state.title = TextFieldValue(savedInstanceState?.getString("title") ?: state.originalTitle)
        val body = savedInstanceState?.getString("body") ?: state.originalBody
        state.body = TextFieldValue(body, TextRange(savedInstanceState?.getInt("cursor", body.length)?.coerceIn(0, body.length) ?: body.length))
        state.anonymous = savedInstanceState?.getBoolean("anonymous") ?: false
        mPresenter.setPostParam(param)
        registerUiEvents()
    }

    override fun onCreatePresenter() = TopicPostPresenter()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.compose_post_editor
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NgaTheme {
                    PostEditorScreen(state, param.postAction, onBack = { requireActivity().finish() },
                        onSubmit = { mPresenter.post(state.title.text, state.body.text, state.anonymous) },
                        onAttach = ::showFilePicker, onCategories = ::loadCategories,
                        onRetry = { state.error = null; state.preparing = true; mPresenter.setPostParam(param) })
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializing = true
        super.onViewCreated(view, savedInstanceState)
        initializing = false
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("title", state.title.text)
        outState.putString("body", state.body.text)
        outState.putInt("cursor", state.body.selection.end)
        outState.putBoolean("anonymous", state.anonymous)
        super.onSaveInstanceState(outState)
    }
    override fun insertBodyText(text: CharSequence) { insertBodyText(text, text.length) }
    override fun insertBodyText(text: CharSequence, position: Int) {
        if (!initializing) state.body = insertEditorText(state.body, text.toString(), position)
    }
    override fun insertTitleText(text: CharSequence) {
        if (!initializing) state.title = insertEditorText(state.title.copy(selection = TextRange.Zero), text.toString())
    }
    override fun insertFile(path: String?, file: CharSequence) {
        state.body = insertEditorText(state.body, "\n${file}\n")
    }
    override fun showFilePicker() { selectImage.launch(arrayOf("image/*")) }
    override fun showUploadFileProgressBar() { state.uploading = true; state.error = null }
    override fun hideUploadFileProgressBar() { state.uploading = false }
    fun setSubmitting(value: Boolean) { state.submitting = value }
    fun setEditorError(message: String?) { state.error = message }
    fun setAttachmentReady(ready: Boolean) { state.attachmentReady = ready; state.preparing = false }
    override fun onUiEvent(event: UiEvent) {
        if (event.what == UiEvent.EVENT_INSERT_EMOTICON) insertBodyText(event.obj as String) else super.onUiEvent(event)
    }
    private fun loadCategories() {
        state.categoryLoading = true
        mPresenter.loadTopicCategory(object : OnHttpCallBack<List<String>> {
            override fun onSuccess(data: List<String>) { state.categories = data.distinct(); state.categoryLoading = false }
            override fun onError(text: String) { state.error = text; state.categoryLoading = false }
        })
    }
}
