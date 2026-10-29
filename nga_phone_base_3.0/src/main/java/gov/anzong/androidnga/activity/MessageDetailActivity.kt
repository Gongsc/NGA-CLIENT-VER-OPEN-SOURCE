package gov.anzong.androidnga.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import gov.anzong.androidnga.R
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.mvvm.viewmodel.MessageDetailViewModel
import gov.anzong.androidnga.ui.compose.MessageDetailScreen
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.observedValue
import sp.phone.util.StringUtils

/** 短消息会话内容；extras 为 "mid"，或由 nuke.php?…&mid= 链接打开。回复成功后刷新。 */
class MessageDetailActivity : BaseActivity() {

    private val viewModel: MessageDetailViewModel by viewModels()
    /** 最近一次请求的错误；重新请求时清除。 */
    private var error by mutableStateOf<String?>(null)
    private val reply = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) load(viewModel::refresh)
    }

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        intent.dataString?.let { intent.putExtra("mid", StringUtils.getUrlParameter(it, "mid")) }
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        // 重建时沿用 ViewModel 中已加载的消息
        if (!viewModel.isLoaded) load(viewModel::refresh)
        setContent {
            NgaTheme {
                val pages = viewModel.pages.observedValue().orEmpty()
                val loading = viewModel.loading.observedValue() == true
                val failure = viewModel.error.observedValue()
                LaunchedEffect(failure) {
                    if (failure != null) {
                        error = failure.ifEmpty { getString(R.string.error_network) }
                        viewModel.errorShown()
                    }
                }
                MessageDetailScreen(viewModel.title?.takeIf { it.isNotBlank() } ?: getString(R.string.label_activity_message_detail),
                    pages.flatMap { it.messageEntryList.orEmpty() }, loading, error,
                    hasNextPage = pages.lastOrNull()?.let { it.get__nextPage() > 0 } ?: true,
                    onRefresh = { load(viewModel::refresh) }, onLoadMore = { load(viewModel::loadNextPage) },
                    onReply = ::startReply, onBack = ::finish)
            }
        }
    }

    private fun load(request: () -> Unit) {
        error = null
        request()
    }

    private fun startReply() {
        reply.launch(AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_MESSAGE_POST)
            .withInt("mid", viewModel.mid)
            .withString("action", "reply")
            .withString("to", viewModel.recipient)
            .withString("title", viewModel.title)
            .intent(this))
    }
}
