package gov.anzong.androidnga.activity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.os.BundleCompat
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.mvvm.viewmodel.AccountEditViewModel
import gov.anzong.androidnga.ui.compose.AvatarCropScreen
import gov.anzong.androidnga.ui.compose.AvatarEditScreen
import gov.anzong.androidnga.ui.compose.NgaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import sp.phone.task.AvatarImage

/**
 * 修改头像：填写图片地址后提交，或从相册选择图片、截取后上传。
 * 成功时返回结果码 123，填写地址时 data 的 "avatar" 为新头像地址。
 */
class AvatarPostActivity : BaseActivity() {

    private val viewModel: AccountEditViewModel by viewModels()

    /** 正在截取的图片，重建后继续截取。 */
    private var cropUri by mutableStateOf<Uri?>(null)

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) cropUri = uri
    }

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        cropUri = savedInstanceState?.let { BundleCompat.getParcelable(it, KEY_CROP, Uri::class.java) }
        setContent {
            NgaTheme {
                var url by rememberSaveable { mutableStateOf("") }
                val uri = cropUri
                if (uri == null) {
                    AvatarEditScreen(url, { url = it }, viewModel.sending, onPick = { pickImage.launch("image/*") }, onSubmit = {
                        if (!viewModel.submitAvatar(url.trim())) ToastUtils.show(getString(R.string.avoidWindfury))
                    }, onBack = ::finish)
                } else {
                    val context = applicationContext
                    val gif = remember(uri) { AvatarImage.isGif(context, uri) }
                    val bitmap by produceState<Bitmap?>(null, uri) {
                        value = try {
                            withContext(Dispatchers.IO) { AvatarImage.load(context, uri) }
                        } catch (e: IllegalArgumentException) {
                            ToastUtils.show(e.message)
                            cropUri = null
                            null
                        }
                    }
                    val image = remember(bitmap) { bitmap?.asImageBitmap() }
                    AvatarCropScreen(image, gif, viewModel.sending, onUpload = { area ->
                        val source = bitmap ?: return@AvatarCropScreen
                        if (!viewModel.uploadAvatar { AvatarImage.dataUrl(source, area) }) ToastUtils.show(getString(R.string.avoidWindfury))
                    }, onUploadOriginal = {
                        if (!viewModel.uploadAvatar { AvatarImage.dataUrl(context, uri) }) ToastUtils.show(getString(R.string.avoidWindfury))
                    }, onCancel = { cropUri = null })
                }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cropUri?.let { outState.putParcelable(KEY_CROP, it) }
    }

    private companion object {
        const val KEY_CROP = "crop_uri"
    }
}
