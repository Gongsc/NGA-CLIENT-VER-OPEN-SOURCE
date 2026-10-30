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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gov.anzong.androidnga.settings.PhoneConfiguration

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

// 回复数达到该值的主题视为热帖，回复胶囊改用主色实底。
internal const val HOT_TOPIC_REPLIES = 50

// 页面底色比 background 再深一档，主题卡片用 surface 浮起；只依赖色板，四套配色与深浅色通用。
@Composable
private fun topicFeedBackdrop(): Color = MaterialTheme.colorScheme.let {
    if (it.background.luminance() > 0.5f) lerp(it.background, it.surfaceContainer, 0.6f) else lerp(it.background, Color.Black, 0.2f)
}

@Composable
private fun ReplyCount(replies: Int) {
    val colors = MaterialTheme.colorScheme
    val hot = replies >= HOT_TOPIC_REPLIES
    Surface(shape = RoundedCornerShape(12.dp), color = if (hot) colors.primary else colors.surfaceContainer,
        contentColor = if (hot) colors.onPrimary else colors.onSurface,
        modifier = Modifier.clearAndSetSemantics { contentDescription = "$replies 回复" }) {
        Row(Modifier.heightIn(min = 24.dp).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(ActionIcons.Reply, null, Modifier.size(14.dp), tint = if (hot) colors.onPrimary else colors.primary)
            Text(replies.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"))
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
    var postExpanded by remember(state) { mutableStateOf(true) }
    val scrollConnection = rememberScrollControlsConnection(listState) { postExpanded = it }
    val postHeight = with(LocalDensity.current) {
        (MaterialTheme.typography.labelLarge.lineHeight.toDp() + 32.dp).coerceAtLeast(56.dp)
    }
    LaunchedEffect(state.refreshGeneration) {
        if (state.refreshGeneration > 0) {
            listState.scrollToItem(0)
            postExpanded = true
        }
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
    val backdrop = topicFeedBackdrop()
    Scaffold(containerColor = backdrop, topBar = {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = backdrop), title = { Text(state.title) }, navigationIcon = {
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
        if (state.canPost) ExtendedFloatingActionButton(
            onClick = onPost,
            expanded = postExpanded,
            modifier = Modifier.heightIn(min = postHeight).semantics { contentDescription = "发表主题" },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
            text = { Text("发表主题", maxLines = 1) },
        )
    }, floatingActionButtonPosition = if (PhoneConfiguration.getInstance().isLeftHandMode) FabPosition.Start else FabPosition.End) { padding ->
        PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding).nestedScroll(scrollConnection)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("topic_feed_list"),
                contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = postHeight + 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.combinedClickable(onClick = { onTopic(index) },
                            onLongClickLabel = if (state.favorite) "删除收藏" else null,
                            onLongClick = if (state.favorite) ({ deleteKey = row.key }) else null).padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(row.title, fontSize = PhoneConfiguration.getInstance().topicTitleSize.sp, fontWeight = FontWeight.Medium)
                            row.replyContent?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(listOfNotNull(row.author, row.lastPoster.takeIf { it.isNotBlank() }?.let { "最后 $it" }).joinToString(" · "),
                                    Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                ReplyCount(row.replies)
                            }
                            if (state.favorite) TextButton(onClick = { deleteKey = row.key }) { Text("删除收藏") }
                        }
                    }
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
