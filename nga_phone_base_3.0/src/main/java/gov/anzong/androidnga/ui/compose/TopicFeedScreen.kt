package gov.anzong.androidnga.ui.compose

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sp.phone.common.PhoneConfiguration

// Preserve the legacy title formatter's attachment, lock, collection and font spans.
fun CharSequence.toComposeText(): AnnotatedString {
    val source = this
    return buildAnnotatedString {
        append(source.toString())
        if (source is Spanned) source.getSpans(0, source.length, Any::class.java).forEach { span ->
            val style = when (span) {
                is ForegroundColorSpan -> SpanStyle(color = Color(span.foregroundColor))
                is UnderlineSpan -> SpanStyle(textDecoration = TextDecoration.Underline)
                is StyleSpan -> SpanStyle(
                    fontWeight = if (span.style == Typeface.BOLD || span.style == Typeface.BOLD_ITALIC) FontWeight.Bold else null,
                    fontStyle = if (span.style == Typeface.ITALIC || span.style == Typeface.BOLD_ITALIC) FontStyle.Italic else null)
                else -> null
            }
            if (style != null) addStyle(style, source.getSpanStart(span), source.getSpanEnd(span))
        }
    }
}

data class TopicRow(val key: String, val title: AnnotatedString, val author: String, val lastPoster: String, val replies: Int, val replyContent: String?)
class TopicFeedState {
    var refreshGeneration by mutableStateOf(0)
    var title by mutableStateOf("主题列表")
    var rows by mutableStateOf(emptyList<TopicRow>())
    var refreshing by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var hasMore by mutableStateOf(true)
    var actions by mutableStateOf(emptyList<HomeAction>())
    var favorite by mutableStateOf(false)
    var canPost by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopicFeedScreen(state: TopicFeedState, onBack: () -> Unit, onRefresh: () -> Unit,
                    onNext: () -> Unit, onTopic: (Int) -> Unit, onDelete: (Int) -> Unit,
                    onPost: () -> Unit, onAction: (Int) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var deleteKey by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.refreshGeneration) {
        if (state.refreshGeneration > 0) listState.scrollToItem(0)
    }
    val nearEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            state.rows.isNotEmpty() && last >= state.rows.lastIndex - 2
        }
    }
    LaunchedEffect(nearEnd, state.rows.size, state.refreshing, state.error) {
        if (nearEnd && !state.refreshing && state.error == null && state.hasMore) onNext()
    }
    Scaffold(topBar = {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background), title = { Text(state.title) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
        }, actions = {
            ActionIcon(ActionIcons.Refresh, "刷新", onRefresh, enabled = !state.refreshing)
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "更多操作") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    state.actions.forEach { action ->
                        DropdownMenuItem(text = { Text(action.label) }, enabled = action.enabled, onClick = { menuOpen = false; onAction(action.id) })
                    }
                }
            }
        })
    }, floatingActionButton = {
        if (state.canPost) ExtendedFloatingActionButton(onClick = onPost) { Text("发表主题") }
    }, floatingActionButtonPosition = if (PhoneConfiguration.getInstance().isLeftHandMode) FabPosition.Start else FabPosition.End) { padding ->
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                state.error?.let { error ->
                    item(key = "error") {
                        Column(Modifier.padding(16.dp)) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            ActionIcon(ActionIcons.Refresh, "重试", if (state.rows.isEmpty()) onRefresh else onNext, enabled = !state.refreshing)
                        }
                    }
                }
                if (state.rows.isEmpty() && !state.refreshing && state.error == null) item(key = "empty") { Text("暂无主题", Modifier.padding(24.dp)) }
                itemsIndexed(state.rows, key = { _, row -> row.key }) { index, row ->
                    Column(Modifier.fillMaxWidth().combinedClickable(onClick = { onTopic(index) },
                        onLongClickLabel = if (state.favorite) "删除收藏" else null,
                        onLongClick = if (state.favorite) ({ deleteKey = row.key }) else null).padding(16.dp)) {
                        Text(row.title, fontSize = PhoneConfiguration.getInstance().topicTitleSize.sp)
                        row.replyContent?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
                        Spacer(Modifier.height(8.dp))
                        Text("${row.author} · ${row.replies} 回复 · ${row.lastPoster}",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.favorite) TextButton(onClick = { deleteKey = row.key }) { Text("删除收藏") }
                    }
                    HorizontalDivider()
                }
                item(key = "next_page") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        if (state.refreshing) CircularProgressIndicator()
                        else if (state.hasMore && state.rows.isNotEmpty()) TextButton(onClick = onNext) { Text("加载更多") }
                        else if (state.rows.isNotEmpty()) Text("已全部加载")
                    }
                }
            }
        }
    }
    deleteKey?.let { selectedKey ->
        AlertDialog(onDismissRequest = { deleteKey = null }, title = { Text("删除收藏") }, text = { Text("确定从收藏夹中删除这个主题？") },
            confirmButton = { TextButton(onClick = {
                state.rows.indexOfFirst { it.key == selectedKey }.takeIf { it >= 0 }?.let(onDelete)
                deleteKey = null
            }) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleteKey = null }) { Text("取消") } })
    }
}
