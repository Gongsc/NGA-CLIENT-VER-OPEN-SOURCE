package gov.anzong.androidnga.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.mvvm.viewmodel.AccountEditViewModel
import gov.anzong.androidnga.ui.compose.AvatarEditScreen
import gov.anzong.androidnga.ui.compose.NgaTheme

/** 修改头像：填写图片地址后提交。成功时返回结果码 123，data 的 "avatar" 为新头像地址。 */
class AvatarPostActivity : BaseActivity() {

    private val viewModel: AccountEditViewModel by viewModels()

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        setContent {
            NgaTheme {
                var url by rememberSaveable { mutableStateOf("") }
                AvatarEditScreen(url, { url = it }, viewModel.sending, onSubmit = {
                    if (!viewModel.submitAvatar(url.trim())) ToastUtils.show(getString(R.string.avoidWindfury))
                }, onBack = ::finish)
                LaunchedEffect(viewModel.result) {
                    val result = viewModel.result ?: return@LaunchedEffect
                    viewModel.resultShown()
                    ToastUtils.show(result.message)
                    if (result.success) {
                        setResult(123, Intent().putExtra("avatar", result.value))
                        finish()
                    }
                }
            }
        }
    }
}
