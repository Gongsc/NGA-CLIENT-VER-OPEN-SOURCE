package gov.anzong.androidnga.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

fun insertEditorText(value: TextFieldValue, text: String, cursorOffset: Int = text.length): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    return TextFieldValue(value.text.replaceRange(start, end, text), TextRange(start + cursorOffset.coerceIn(0, text.length)))
}

fun formatEditorSelection(value: TextFieldValue, tag: String): TextFieldValue {
    val selected = value.text.substring(value.selection.min, value.selection.max)
    val opening = "[$tag]"
    val closing = "[/${tag.substringBefore('=')}]"
    return insertEditorText(value, opening + selected + closing, opening.length + selected.length)
}

class PostEditorState {
    var title by mutableStateOf(TextFieldValue(""))
    var body by mutableStateOf(TextFieldValue(""))
    var anonymous by mutableStateOf(false)
    var submitting by mutableStateOf(false)
    var uploading by mutableStateOf(false)
    var preparing by mutableStateOf(true)
    var attachmentReady by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var categories by mutableStateOf(emptyList<String>())
    var categoryLoading by mutableStateOf(false)
    var originalTitle = ""
    var originalBody = ""
    val dirty get() = title.text != originalTitle || body.text != originalBody || anonymous
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostEditorScreen(state: PostEditorState, action: String, onBack: () -> Unit, onSubmit: () -> Unit,
                     onAttach: () -> Unit, onCategories: () -> Unit, onRetry: () -> Unit) {
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSend by rememberSaveable { mutableStateOf(false) }
    val busy = state.submitting || state.uploading
    fun leave() { if (state.dirty) confirmExit = true else onBack() }
    BackHandler { if (panel != null) panel = null else if (!busy) leave() }
    Scaffold(topBar = {
        TopAppBar(title = { Text(when(action) { "reply" -> "回复主题"; "modify" -> "编辑帖子"; else -> "发表主题" }) }, navigationIcon = {
            IconButton(onClick = { leave() }, enabled = !busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
        }, actions = {
            TextButton(enabled = !busy && state.body.text.isNotBlank() && (action != "new" || state.title.text.isNotBlank()),
                onClick = { confirmSend = true }) { Text(if (state.submitting) "提交中" else "提交") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            if (busy || state.preparing) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { error ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    if (!state.attachmentReady && !state.preparing) ActionIcon(ActionIcons.Refresh, "重试", onRetry)
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = state.title, onValueChange = { state.title = it }, enabled = !busy,
                    label = { Text(if (action == "new") "标题" else "标题（可选）") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = state.body, onValueChange = { state.body = it }, enabled = !busy,
                    label = { Text("正文") }, modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp), minLines = 8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = state.anonymous, onCheckedChange = { state.anonymous = it }, enabled = !busy)
                    Text("匿名发表")
                }
                if (state.anonymous) Text("匿名发帖或回复每次扣除 100 铜币。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (state.uploading) Text("正在上传附件，请稍候。")
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item { TextButton(onClick = onAttach, enabled = !busy && state.attachmentReady) { Text("添加图片") } }
                item { TextButton(onClick = { panel = "格式" }, enabled = !busy) { Text("格式") } }
                item { TextButton(onClick = { panel = "表情" }, enabled = !busy) { Text("表情") } }
                item { TextButton(onClick = { panel = "颜色" }, enabled = !busy) { Text("颜色") } }
                item { TextButton(onClick = { panel = "字号" }, enabled = !busy) { Text("字号") } }
                if (action == "new") item { TextButton(onClick = { panel = "分类"; onCategories() }, enabled = !busy) { Text("主题分类") } }
            }
        }
    }
    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false }, title = { Text("放弃编辑？") },
        text = { Text("当前内容尚未提交，离开页面将放弃本次编辑。") },
        confirmButton = { TextButton(onClick = onBack) { Text("放弃编辑") } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("继续编辑") } })
    if (confirmSend) AlertDialog(onDismissRequest = { confirmSend = false }, title = { Text("确认提交") },
        text = { Text(if (state.anonymous) "是否以匿名身份提交当前内容？本次操作将扣除 100 铜币。" else "是否提交当前编辑的内容？") },
        confirmButton = { TextButton(onClick = { confirmSend = false; onSubmit() }) { Text("提交") } },
        dismissButton = { TextButton(onClick = { confirmSend = false }) { Text("取消") } })
    panel?.let { selected ->
        val formats = listOf("加粗" to "b", "斜体" to "i", "下划线" to "u", "删除线" to "del", "引用" to "quote", "折叠" to "collapse", "链接" to "url", "提及" to "@")
        val options = when (selected) {
            "格式" -> formats
            "颜色" -> listOf("skyblue", "royalblue", "blue", "darkblue", "orange", "orangered", "crimson", "red", "firebrick", "darkred", "green", "limegreen", "seagreen", "teal", "deeppink", "tomato", "coral", "purple", "indigo", "burlywood", "sandybrown", "sienna", "chocolate", "silver").map { it to "color=$it" }
            "字号" -> listOf(110, 120, 130, 150, 200, 250, 300, 400, 500).map { "$it%" to "size=$it%" }
            "分类" -> state.categories.map { it to it }
            else -> listOf(1, 2, 3, 24, 25, 26, 27, 28, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39, 4, 40, 41, 42, 43, 5, 6, 7, 8).map { "[s:$it]" to "[s:$it]" }
        }
        ModalBottomSheet(onDismissRequest = { panel = null }) {
            Text(selected, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
            if (state.categoryLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (selected == "分类" && options.isEmpty() && !state.categoryLoading) Text("暂无可用分类", Modifier.padding(16.dp))
            if (selected == "表情") EmoticonPicker { code ->
                state.body = insertEditorText(state.body, code)
                panel = null
            } else androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(options, key = { it.first }) { (label, value) ->
                    TextButton(onClick = {
                        when (selected) {
                            "分类" -> state.title = insertEditorText(state.title.copy(selection = TextRange.Zero), value)
                            "表情" -> state.body = insertEditorText(state.body, value)
                            else -> state.body = if (value == "@") insertEditorText(state.body, "[@]", 2) else formatEditorSelection(state.body, value)
                        }
                        panel = null
                    }, modifier = Modifier.fillMaxWidth()) { Text(label) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
