package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

fun articlePageCount(rows: Int): Int = ((rows.coerceAtLeast(0).toLong() + 19) / 20).toInt().coerceAtLeast(1)
data class ReaderPosition(val pageIndex: Int, val floor: Int? = null)
fun readerPosition(input: String, floorMode: Boolean, pages: Int, rows: Int): ReaderPosition? {
    val number = input.trim().toIntOrNull() ?: return null
    return if (floorMode) {
        if (number < 0 || number >= rows) null else ReaderPosition(number / 20, number)
    } else if (number in 1..pages) ReaderPosition(number - 1) else null
}
class ArticleReaderState {
    var title by mutableStateOf("帖子详情")
    var pages by mutableIntStateOf(1)
    var rows by mutableIntStateOf(0)
    var currentPage by mutableIntStateOf(0)
    var jump by mutableStateOf<ReaderPosition?>(null)
    var floor by mutableStateOf<Int?>(null)
    var actions by mutableStateOf(emptyList<HomeAction>())
    var showJump by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(state: ArticleReaderState, initialPage: Int, leftHand: Boolean,
                        onBack: () -> Unit, onRefresh: () -> Unit, onReply: () -> Unit,
                        onAction: (Int) -> Unit, onPage: (Int) -> Unit, content: @Composable (Int, Boolean) -> Unit) {
    val pager = rememberPagerState(initialPage = (initialPage - 1).coerceIn(0, state.pages - 1), pageCount = { state.pages })
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(pager.settledPage) { state.currentPage = pager.settledPage; onPage(pager.settledPage + 1) }
    LaunchedEffect(state.jump) {
        state.jump?.let { target ->
            state.floor = target.floor
            pager.scrollToPage(target.pageIndex.coerceIn(0, state.pages - 1))
            state.jump = null
        }
    }
    @Composable fun ReplyButton() {
        ActionIcon(ActionIcons.Reply, "回复主题", onReply,
            Modifier.background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)))
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text(state.title, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }, navigationIcon = {
            ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack)
        }, actions = {
            ActionIcon(ActionIcons.Refresh, "刷新", onRefresh)
            Box {
                ActionIcon(Icons.Default.MoreVert, "帖子操作", { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    state.actions.forEach { action ->
                        DropdownMenuItem(text = { Text(action.label) }, enabled = action.enabled, onClick = { menuOpen = false; onAction(action.id) })
                    }
                }
            }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }, bottomBar = {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(horizontal = 8.dp, vertical = 8.dp).testTag("reader_pagination"),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (leftHand) ReplyButton()
                    ActionIcon(ActionIcons.First, "回到首页", { scope.launch { pager.animateScrollToPage(0) } }, enabled = pager.currentPage > 0 && !pager.isScrollInProgress)
                    ActionIcon(ActionIcons.Left, "上一页", { scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) } }, enabled = pager.currentPage > 0 && !pager.isScrollInProgress)
                    OutlinedButton(onClick = { state.showJump = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { contentDescription = "跳转页码或楼层"; stateDescription = "第 ${pager.currentPage + 1} 页，共 ${state.pages} 页" },
                        contentPadding = PaddingValues(horizontal = 4.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text("${pager.currentPage + 1} / ${state.pages}", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                        Icon(ActionIcons.Down, null, Modifier.padding(start = 4.dp).size(14.dp))
                    }
                    ActionIcon(ActionIcons.Right, "下一页", { scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(state.pages - 1)) } }, enabled = pager.currentPage < state.pages - 1 && !pager.isScrollInProgress)
                    if (!leftHand) ReplyButton()
                }
            }
        }
    }) { padding ->
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().padding(padding)) { page ->
            content(page, pager.settledPage == page)
        }
    }
    if (state.showJump) {
        var text by rememberSaveable { mutableStateOf("") }
        var floorMode by rememberSaveable { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { state.showJump = false }, title = { Text("跳转位置") }, text = {
            Column {
                Row {
                    FilterChip(selected = !floorMode, onClick = { floorMode = false; error = null }, label = { Text("按页码") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = floorMode, onClick = { floorMode = true; error = null }, label = { Text("按楼层") })
                }
                OutlinedTextField(value = text, onValueChange = { text = it; error = null }, singleLine = true,
                    label = { Text(if (floorMode) "楼层（0–${(state.rows - 1).coerceAtLeast(0)}）" else "页码（1–${state.pages}）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null, supportingText = { error?.let { Text(it) } })
            }
        }, confirmButton = { TextButton(onClick = {
            val target = readerPosition(text, floorMode, state.pages, state.rows)
            if (target == null) error = "请输入有效范围内的位置。"
            else { state.jump = target; state.showJump = false }
        }) { Text("跳转") } }, dismissButton = { TextButton(onClick = { state.showJump = false }) { Text("取消") } })
    }
}
