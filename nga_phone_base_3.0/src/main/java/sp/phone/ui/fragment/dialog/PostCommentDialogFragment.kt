package sp.phone.ui.fragment.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.ui.compose.NgaTheme
import sp.phone.task.PostCommentTask

class PostCommentDialogFragment : BaseDialogFragment(), PostCommentTask.OnPostCommentFinishedListener {
    private var submitting by mutableStateOf(false)
    private var error by mutableStateOf<String?>(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NgaTheme {
                    var body by rememberSaveable { mutableStateOf("") }
                    var anonymous by rememberSaveable { mutableStateOf(false) }
                    Surface(shape = MaterialTheme.shapes.extraLarge) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("发表贴条", style = MaterialTheme.typography.headlineSmall)
                            OutlinedTextField(value = body, onValueChange = { body = it; error = null }, enabled = !submitting,
                                label = { Text("内容（6–650 字）") }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                                isError = error != null, supportingText = { error?.let { Text(it) } })
                            Row {
                                Checkbox(checked = anonymous, enabled = !submitting, onCheckedChange = { anonymous = it })
                                Text("匿名发表（扣除 100 铜币）", modifier = Modifier.padding(top = 12.dp))
                            }
                            if (submitting) LinearProgressIndicator(Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { dismiss() }, enabled = !submitting) { Text("取消") }
                                TextButton(onClick = {
                                    if (body.length !in 6..650) error = "内容长度应为 6–650 字。"
                                    else {
                                        submitting = true
                                        isCancelable = false
                                        val args = requireArguments()
                                        PostCommentTask(args.getInt("fid"), args.getInt("pid"), args.getInt("tid"), if (anonymous) 1 else 0,
                                            args.getString("prefix"), requireActivity(), this@PostCommentDialogFragment).execute(body)
                                    }
                                }, enabled = !submitting && body.length in 6..650) { Text("提交") }
                            }
                        }
                    }
                }
            }
        }
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
    override fun OnPostCommentFinished(result: String?, success: Boolean) {
        if (!isAdded) return
        submitting = false
        isCancelable = true
        if (success) {
            parentFragmentManager.setFragmentResult("article_comment_posted_${requireArguments().getInt("page")}", Bundle().apply { putInt("tid", requireArguments().getInt("tid")) })
            dismiss()
        } else error = result ?: "提交失败，请重试。"
    }
}
