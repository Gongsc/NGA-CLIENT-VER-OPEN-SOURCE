package gov.anzong.androidnga.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import gov.anzong.androidnga.R
import gov.anzong.androidnga.mvvm.model.ContentUploadImage
import gov.anzong.androidnga.mvvm.viewmodel.TopicPostViewModel
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.PostEditorScreen
import sp.phone.param.ParamKey
import sp.phone.param.PostParam

/**
 * 发帖、回复与编辑。extras：action（new/reply/modify）、tid、fid、pid、stid、title、prefix（初始正文）。
 * 发布成功时返回 RESULT_OK。
 */
class PostActivity : BaseActivity() {

    private val viewModel: TopicPostViewModel by viewModels()
    private val selectImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.upload(ContentUploadImage(it)) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val param = postParam()
        val action = param.postAction ?: run { finish(); return }
        setTitle(when (action) {
            "reply" -> R.string.reply_thread
            "modify" -> R.string.modify_thread
            else -> R.string.new_thread
        })
        // TopicPostViewModel 经 SavedStateHandle 从 extras 读取 "param"
        intent.putExtra("param", param)
        // 进程被回收后，从保存的状态恢复编辑内容；页面重建时 ViewModel 已有内容，不会覆盖
        viewModel.start(savedInstanceState?.getString("title"), savedInstanceState?.getString("body"),
            savedInstanceState?.getInt("cursor", -1) ?: -1, savedInstanceState?.getBoolean("anonymous") ?: false)
        setContent {
            NgaTheme {
                PostEditorScreen(viewModel.editor, viewModel.action, onBack = ::finish,
                    onSubmit = viewModel::submit, onAttach = { selectImage.launch(arrayOf("image/*")) },
                    onCategories = viewModel::loadCategories, onRetry = viewModel::prepare)
            }
            LaunchedEffect(viewModel.posted) {
                if (viewModel.posted) {
                    setResult(RESULT_OK)
                    finish()
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

    private fun postParam() = PostParam(intent.getStringExtra(ParamKey.KEY_TID), "", "").apply {
        postAction = intent.getStringExtra(ParamKey.KEY_ACTION)
        postFid = intent.getIntExtra(ParamKey.KEY_FID, -7)
        postPid = intent.getStringExtra(ParamKey.KEY_PID)
        postContent = intent.getStringExtra("prefix")
        postSubject = intent.getStringExtra("title")
        stid = intent.getStringExtra(ParamKey.KEY_STID)
    }

    override fun usesComposeContent() = true
}
