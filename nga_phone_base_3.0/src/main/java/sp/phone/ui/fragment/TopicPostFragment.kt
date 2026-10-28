package sp.phone.ui.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import gov.anzong.androidnga.R
import gov.anzong.androidnga.mvvm.model.ContentUploadImage
import gov.anzong.androidnga.mvvm.viewmodel.TopicPostViewModel
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.PostEditorScreen

class TopicPostFragment : BaseFragment() {
    private val viewModel: TopicPostViewModel by viewModels()
    private val selectImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.upload(ContentUploadImage(it)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 进程被回收后，从保存的状态恢复编辑内容；页面重建时 ViewModel 已有内容，不会覆盖
        viewModel.start(savedInstanceState?.getString("title"), savedInstanceState?.getString("body"),
            savedInstanceState?.getInt("cursor", -1) ?: -1, savedInstanceState?.getBoolean("anonymous") ?: false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.compose_post_editor
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NgaTheme {
                    PostEditorScreen(viewModel.editor, viewModel.action, onBack = { requireActivity().finish() },
                        onSubmit = viewModel::submit, onAttach = { selectImage.launch(arrayOf("image/*")) },
                        onCategories = viewModel::loadCategories, onRetry = viewModel::prepare)
                }
                LaunchedEffect(viewModel.posted) {
                    if (viewModel.posted) {
                        requireActivity().setResult(Activity.RESULT_OK)
                        requireActivity().finish()
                    }
                }
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        val editor = viewModel.editor
        outState.putString("title", editor.title.text)
        outState.putString("body", editor.body.text)
        outState.putInt("cursor", editor.body.selection.end)
        outState.putBoolean("anonymous", editor.anonymous)
        super.onSaveInstanceState(outState)
    }
}
