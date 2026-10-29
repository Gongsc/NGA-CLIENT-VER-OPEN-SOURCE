package gov.anzong.androidnga.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.mvvm.viewmodel.AccountEditViewModel
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.SignatureEditScreen
import sp.phone.util.FunctionUtils

/** 修改签名；extras 中的 "prefix" 为当前签名。成功时返回 RESULT_OK，data 的 "sign" 为新签名。 */
class SignPostActivity : BaseActivity() {

    private val viewModel: AccountEditViewModel by viewModels()

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = intent.getStringExtra("prefix").orEmpty()
        setContent {
            NgaTheme {
                SignatureEditScreen(initial, viewModel.sending, onSubmit = { text ->
                    if (!viewModel.submitSignature(FunctionUtils.ColorTxtCheck(text))) ToastUtils.show(getString(R.string.avoidWindfury))
                }, onBack = ::finish)
                LaunchedEffect(viewModel.result) {
                    val result = viewModel.result ?: return@LaunchedEffect
                    viewModel.resultShown()
                    ToastUtils.show(result.message)
                    if (result.success) {
                        setResult(RESULT_OK, Intent().putExtra("sign", result.value))
                        finish()
                    }
                }
            }
        }
    }
}
