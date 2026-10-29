package gov.anzong.androidnga.ui.compose

import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import gov.anzong.androidnga.mvvm.viewmodel.AccountEditViewModel
import sp.phone.util.ImageUtils

/** 修改签名：支持论坛代码；内容为空时不能提交，提交中显示进度。 */
@Composable
fun SignatureEditScreen(initial: String, sending: Boolean, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    var empty by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    fun submit() {
        empty = text.text.isBlank()
        if (!empty) onSubmit(text.text)
    }
    SettingsSubPage("修改签名", onBack, actions = {
        if (sending) CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).size(24.dp))
        else ActionIcon(Icons.AutoMirrored.Filled.Send, "提交", ::submit)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(16.dp)) {
            OutlinedTextField(text, { text = it; empty = false }, Modifier.fillMaxWidth().weight(1f).focusRequester(focus),
                enabled = !sending, label = { Text("签名") }, isError = empty,
                supportingText = { Text(if (empty) "请输入内容" else "支持论坛代码，如 [b]粗体[/b]、[url]链接[/url]") })
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** 修改头像：填写图片地址或从相册选择后上传（上传进度由上传任务的对话框显示），预览后提交。 */
@Composable
fun AvatarEditScreen(url: String, onUrlChange: (String) -> Unit, sending: Boolean,
                     onPick: () -> Unit, onSubmit: () -> Unit, onBack: () -> Unit) {
    var invalid by rememberSaveable { mutableStateOf(false) }
    val busy = sending
    SettingsSubPage("修改头像", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(url, { onUrlChange(it); invalid = false }, Modifier.fillMaxWidth(), enabled = !busy, singleLine = true,
                label = { Text("头像地址") }, placeholder = { Text("https://…") }, isError = invalid,
                supportingText = if (invalid) ({ Text("请输入正确的头像 URL 地址") }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false))
            OutlinedButton(onClick = onPick, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("从相册选择并上传")
            }
            if (AccountEditViewModel.isAvatarUrl(url)) {
                Text("预览", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Box(Modifier.size(96.dp).clip(CircleShape).align(Alignment.CenterHorizontally)) {
                    AndroidView(factory = { ImageView(it).apply { contentDescription = "头像预览" } }, modifier = Modifier.fillMaxSize(),
                        update = { image -> ImageUtils.loadRoundCornerAvatar(image, url.trim()) })
                }
            }
            Button(onClick = {
                invalid = !AccountEditViewModel.isAvatarUrl(url)
                if (!invalid) onSubmit()
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("提交")
            }
        }
    }
}
