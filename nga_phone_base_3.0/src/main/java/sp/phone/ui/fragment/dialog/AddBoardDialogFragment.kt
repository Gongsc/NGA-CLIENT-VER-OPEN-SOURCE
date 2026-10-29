package sp.phone.ui.fragment.dialog

import android.os.Bundle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.os.bundleOf
import gov.anzong.androidnga.ui.compose.NgaDialogLayout

/** 按版面 ID（fid）或合集 ID（stid）添加版面；结果以 [RESULT_KEY] 的 Fragment Result 返回（"fid"、"stid"）。 */
class AddBoardDialogFragment : BaseDialogFragment() {

    @androidx.compose.runtime.Composable
    override fun DialogContent() {
        var fid by rememberSaveable { mutableStateOf("") }
        var stid by rememberSaveable { mutableStateOf("") }
        NgaDialogLayout("输入版面名称及ID添加版面", buttons = {
            TextButton(onClick = { dismiss() }) { Text("取消") }
            TextButton(onClick = {
                parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf("fid" to fid.trim(), "stid" to stid.trim()))
                dismiss()
            }, enabled = fid.isNotBlank() || stid.isNotBlank()) { Text("添加") }
        }) {
            OutlinedTextField(fid, { fid = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("版面ID（fid）") },
                supportingText = { Text("个人版面有负号") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(stid, { stid = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("合集ID（stid）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
    }

    companion object {
        const val RESULT_KEY = "add_board"
    }
}
