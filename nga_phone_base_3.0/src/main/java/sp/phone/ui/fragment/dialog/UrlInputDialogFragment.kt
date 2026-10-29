package sp.phone.ui.fragment.dialog

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.activity.ArticleListActivity
import gov.anzong.androidnga.activity.TopicListActivity
import gov.anzong.androidnga.ui.compose.NgaDialogLayout
import java.util.Locale

/** 由论坛地址打开版面或帖子；剪贴板中有文字时预先填入并全选。 */
class UrlInputDialogFragment : BaseDialogFragment() {

    @Composable
    override fun Content() {
        var input by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(clipboardText().let { TextFieldValue(it, TextRange(0, it.length)) }) }
        var error by rememberSaveable { mutableStateOf<String?>(null) }
        fun open() {
            when (val target = forumUrlTarget(input.text, Utils.getNGAHost())) {
                is UrlTarget.Invalid -> error = target.message
                is UrlTarget.Board -> { startActivity(Intent(requireContext(), TopicListActivity::class.java).setData(Uri.parse(target.url))); dismiss() }
                is UrlTarget.Topic -> { startActivity(Intent(requireContext(), ArticleListActivity::class.java).setData(Uri.parse(target.url))); dismiss() }
            }
        }
        NgaDialogLayout("由URL读取", buttons = {
            TextButton(onClick = { dismiss() }) { Text("取消") }
            TextButton(onClick = ::open) { Text("打开") }
        }) {
            OutlinedTextField(input, { input = it; error = null }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("输入URL地址(板块或帖子)") }, isError = error != null, supportingText = error?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onGo = { open() }))
        }
    }

    private fun clipboardText(): String {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return ""
        if (!clipboard.hasPrimaryClip()) return ""
        return clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString().orEmpty()
    }
}

/** 地址识别结果：版面列表、帖子，或无法识别的原因。 */
sealed class UrlTarget {
    data class Board(val url: String) : UrlTarget()
    data class Topic(val url: String) : UrlTarget()
    data class Invalid(val message: String) : UrlTarget()
}

/** 把输入的论坛地址规范为当前域名 [host] 下的 thread.php / read.php 地址，规则与旧对话框一致。 */
fun forumUrlTarget(input: String, host: String): UrlTarget {
    var url = input.trim()
    if (url.isEmpty()) return UrlTarget.Invalid("请输入URL地址")
    url = url.lowercase(Locale.US)
    return when {
        url.contains("thread.php") -> UrlTarget.Board(url
            .replace(Regex("(?i)[^\\[|\\]]+stid=(-?\\d+)[^\\[|\\]]*"), host + "thread.php?stid=$1")
            .replace(Regex("(?i)[^\\[|\\]]+fid=(-?\\d+)[^\\[|\\]]*"), host + "thread.php?fid=$1"))
        url.contains("read.php") -> {
            if (url.contains("tid") && url.contains("pid")) {
                url = if (url.indexOf("tid") < url.indexOf("pid"))
                    url.replace(Regex("(?i)[^\\[|\\]]+tid=(\\d+)[^\\[|\\]]+pid=(\\d+)[^\\[|\\]]{0,}"), host + "read.php?pid=$2&tid=$1")
                else url.replace(Regex("(?i)[^\\[|\\]]+pid=(\\d+)[^\\[|\\]]+tid=(\\d+)[^\\[|\\]]{0,}"), host + "read.php?pid=$1&tid=$2")
            } else if (url.contains("tid")) {
                url = url.replace(Regex("(?i)[^\\[|\\]]+tid=(\\d+)[^\\[|\\]]{0,}"), host + "read.php?tid=$1")
            } else if (url.contains("pid")) {
                url = url.replace(Regex("(?i)[^\\[|\\]]+pid=(\\d+)[^\\[|\\]]{0,}"), host + "read.php?pid=$1")
            }
            UrlTarget.Topic(url)
        }
        else -> UrlTarget.Invalid("输入的地址并非NGA的板块地址或帖子地址,或缺少fid/pid/tid信息,请检查后再试")
    }
}
