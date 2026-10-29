package sp.phone.ui.fragment.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import sp.phone.task.ReportTask
import gov.anzong.androidnga.ui.compose.NgaDialogLayout

/** 举报楼层；arguments 为 "tid" 与 "pid"。举报在后台提交，结果以提示显示。 */
class ReportDialogFragment : BaseDialogFragment() {

    @Composable
    override fun DialogContent() {
        var reason by rememberSaveable { mutableStateOf("") }
        NgaDialogLayout("举报", buttons = {
            TextButton(onClick = { dismiss() }) { Text("取消") }
            TextButton(onClick = { report(reason); dismiss() }) { Text("举报") }
        }) {
            Text("不要仅仅因为意见不同而举报", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), label = { Text("举报理由") }, minLines = 2)
        }
    }

    private fun report(reason: String) {
        val args = arguments ?: return
        ReportTask().execute(args.getInt("tid"), args.getInt("pid"), reason)
    }
}
