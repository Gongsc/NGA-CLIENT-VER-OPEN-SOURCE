package gov.anzong.androidnga.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.mvvm.viewmodel.AccountEditViewModel
import gov.anzong.androidnga.ui.compose.AvatarEditScreen
import gov.anzong.androidnga.ui.compose.NgaTheme
import sp.phone.task.AvatarFileUploadTask

/** 修改头像：填写地址或选择图片上传后提交。成功时返回结果码 123，data 的 "avatar" 为新头像地址。 */
class AvatarPostActivity : BaseActivity(), AvatarFileUploadTask.onFileUploaded {

    private val viewModel: AccountEditViewModel by viewModels()
    /** 上传完成后的图片地址，由 Compose 状态接收。 */
    private var uploadedUrl by mutableStateOf<String?>(null)

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) AvatarFileUploadTask(this, this, uri).execute()
    }

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NgaTheme {
                var url by rememberSaveable { mutableStateOf("") }
                LaunchedEffect(uploadedUrl) { uploadedUrl?.let { url = it; uploadedUrl = null } }
                AvatarEditScreen(url, { url = it }, viewModel.sending, onPick = { pickImage.launch("image/*") }, onSubmit = {
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

    override fun finishUpload(picUrl: String, uri: Uri): Int {
        uploadedUrl = picUrl
        return 1
    }
}
