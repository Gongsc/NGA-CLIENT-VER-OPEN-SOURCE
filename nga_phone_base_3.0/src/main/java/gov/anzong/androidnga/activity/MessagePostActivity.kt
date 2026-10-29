package gov.anzong.androidnga.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.mvvm.viewmodel.MessagePostViewModel
import gov.anzong.androidnga.ui.compose.MessagePostScreen
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.observedValue
import sp.phone.param.MessagePostParam

/** 写短消息（action=new）或回复会话（action=reply，带 mid、title）；extras 中的 "to" 为预填收件人。发送成功返回 RESULT_OK。 */
class MessagePostActivity : BaseActivity() {

    private val viewModel: MessagePostViewModel by viewModels()

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // ViewModel 从 extras 的 "param" 创建；重建时沿用，保留进行中的提交
        if (!intent.hasExtra("param")) intent.putExtra("param", postParam())
        super.onCreate(savedInstanceState)
        setContent {
            NgaTheme {
                val sending = viewModel.sending.observedValue() == true
                val result = viewModel.result.observedValue()
                MessagePostScreen(viewModel.isNewMessage, viewModel.initialRecipient, sending, onSend = { title, recipient, body ->
                    if (!viewModel.send(title, recipient, body)) ToastUtils.show(getString(R.string.avoidWindfury))
                }, onBack = ::finish)
                LaunchedEffect(result) {
                    result ?: return@LaunchedEffect
                    viewModel.resultShown()
                    ToastUtils.show(result.message)
                    if (result.success) {
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }
        }
    }

    private fun postParam(): MessagePostParam {
        val param = if (intent.getStringExtra("action") == "reply") {
            MessagePostParam("reply", intent.getIntExtra("mid", 0), intent.getStringExtra("title"))
        } else MessagePostParam()
        param.recipient = intent.getStringExtra("to")
        return param
    }
}
