package gov.anzong.androidnga.ui.compose

import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import gov.anzong.androidnga.R
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import sp.phone.common.PhoneConfiguration
import sp.phone.http.bean.ThreadRowInfo
import sp.phone.util.FunctionUtils
import sp.phone.util.ImageUtils

class ArticlePageState {
    var rows by mutableStateOf(emptyList<ThreadRowInfo>())
    var hotReplies by mutableStateOf(emptyList<ThreadRowInfo>())
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var targetFloor by mutableStateOf<Int?>(null)
    var scrollRequest by mutableIntStateOf(0)
    var owner by mutableStateOf<String?>(null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlePageScreen(state: ArticlePageState, onRefresh: () -> Unit, onQuote: (ThreadRowInfo) -> Unit,
                      onProfile: (ThreadRowInfo) -> Unit, onAvatar: (ThreadRowInfo) -> Unit,
                      actions: (ThreadRowInfo) -> List<HomeAction>, onAction: (ThreadRowInfo, Int) -> Unit) {
    val list = rememberLazyListState()
    val bodySize = rememberArticleFontSize()
    val entries = remember(state.rows, state.hotReplies) { articleEntries(state.rows, state.hotReplies) }
    LaunchedEffect(state.scrollRequest, entries) {
        state.targetFloor?.let { floor ->
            val index = entries.indexOfFirst { !it.hot && it.row?.lou == floor }
            if (index >= 0) { list.scrollToItem(index + if (state.error == null) 0 else 1); state.targetFloor = null }
        }
    }
    PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("article_floors"), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.error?.let { message ->
                item("error") {
                    Column(Modifier.padding(16.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.error)
                        ActionIcon(ActionIcons.Refresh, "重新加载", onRefresh, enabled = !state.loading)
                    }
                }
            }
            if (!state.loading && state.error == null && state.rows.isEmpty()) item("empty") { Text("暂无可显示的内容", Modifier.padding(24.dp)) }
            items(entries, key = { it.key }, contentType = { if (it.row == null) "hot_header" else "floor" }) { entry ->
                val background = if (entry.hot) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                val foreground = if (entry.hot) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                val secondary = if (entry.hot) foreground else MaterialTheme.colorScheme.onSurfaceVariant
                val row = entry.row
                if (row == null) {
                    Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().testTag("hot_replies_header")) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("热点回复", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
                            Text("${entries.count { it.hot && it.row != null }} 条", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                Surface(color = background, contentColor = foreground, shape = RoundedCornerShape(14.dp), modifier = Modifier.testTag(if (entry.hot) "hot_reply_${row.pid}" else "floor_${row.lou}")) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onAvatar(row) }, modifier = Modifier.semantics { contentDescription = "查看头像" }) {
                            AndroidView(factory = { ImageView(it) }, modifier = Modifier.size(32.dp), update = { image ->
                                ImageUtils.loadRoundCornerAvatar(image, FunctionUtils.parseAvatarUrl(row.js_escap_avatar), !PhoneConfiguration.getInstance().isAvatarLoadEnabled)
                                image.importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
                            })
                        }
                        TextButton(onClick = { onProfile(row) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                Text(row.author.orEmpty() + if (row.author == state.owner) " · 楼主" else "", style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Text(row.postdate.orEmpty(), style = MaterialTheme.typography.labelSmall, color = secondary)
                            }
                        }
                        Text(if (row.lou >= 0) "#${row.lou}" else "回复", style = MaterialTheme.typography.labelSmall, color = secondary)
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            ActionIcon(Icons.Default.MoreVert, "楼层操作", { expanded = true })
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                actions(row).forEach { action ->
                                    DropdownMenuItem(text = { Text(action.label) }, onClick = { expanded = false; onAction(row, action.id) })
                                }
                            }
                        }
                    }
                    if (!row.memberGroup.isNullOrBlank() || !row.postCount.isNullOrBlank()) Text("${row.memberGroup.orEmpty()} · 威望 ${row.reputation} · 发帖 ${row.postCount.orEmpty()}",
                        style = MaterialTheme.typography.labelSmall, color = secondary)
                    val plain = remember(row, row.formattedHtmlData) { plainArticleText(row) }
                    if (plain != null) Text(plain, modifier = Modifier.testTag("article_native_text"), fontSize = bodySize.sp, lineHeight = (bodySize * 1.6f).sp)
                    else if (row.formattedHtmlData.isNullOrEmpty()) Text(row.content.orEmpty(), fontSize = bodySize.sp, lineHeight = (bodySize * 1.6f).sp)
                    else ArticleHtml(row.formattedHtmlData, row.imageUrls.orEmpty(), background, foreground)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("赞同：${row.score}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        if (!row.fromClientModel.isNullOrBlank()) ClientPlatformIcon(row.fromClientModel, row.fromClient.orEmpty())
                        ActionIcon(ActionIcons.Reply, "引用回复", { onQuote(row) })
                    }
                }
                }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientPlatformIcon(model: String, details: String) {
    val (resource, label) = when (model.lowercase()) {
        "android" -> R.drawable.ic_android_12dp to "Android 客户端"
        "ios" -> R.drawable.ic_apple_12dp to "iOS 客户端"
        "wp" -> R.drawable.ic_windows_12dp to "Windows Phone 客户端"
        else -> R.drawable.ic_smartphone_12dp to "其他客户端"
    }
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        IconButton(onClick = { gov.anzong.androidnga.base.util.ToastUtils.info(details.ifBlank { label }) }) {
            Icon(painterResource(resource), contentDescription = label, modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ArticleHtml(html: String, images: List<String>, background: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface, foreground: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    var height by rememberSaveable(html) { mutableFloatStateOf(48f) }
    val colors = MaterialTheme.colorScheme
    val bodySize = rememberArticleFontSize()
    val density = LocalDensity.current
    val cssSize = with(density) { bodySize.sp.toPx() / density.density }
    fun css(color: androidx.compose.ui.graphics.Color) = "#%06X".format(color.toArgb() and 0xFFFFFF)
    val themedHtml = remember(html, colors, background, foreground, cssSize) { themedArticleHtml(html, css(foreground), css(background), css(colors.primary), css(colors.background), css(colors.outlineVariant), cssSize) }
    AndroidView(modifier = Modifier.fillMaxWidth().height(height.dp).testTag("article_html"),
        factory = { context -> ArticleContentWebView(context) },
        update = { web -> web.bind(themedHtml, images) { height = it } },
        onReset = { web -> web.resetForReuse() },
        onRelease = { web -> web.release() })
}

/** 覆盖网页容器及引用区的旧配色，保留正文自行指定的彩色文字。 */
fun themedArticleHtml(html: String, foreground: String, background: String, accent: String, quote: String, outline: String, fontSize: Float = 16.8f): String {
    val style = """<meta name="viewport" content="width=device-width,initial-scale=1"><style id="nga-compose-theme">
        html,body{background:$background!important;color:$foreground!important;}
        body{margin:0!important;padding:0!important;font-family:sans-serif!important;font-size:${fontSize}px!important;line-height:1.6!important;overflow-wrap:anywhere;-webkit-text-size-adjust:100%;}
        p,div,table,table.default,td,th,li,blockquote,h5{font-size:inherit;}
        a{color:$accent!important;}
        .quote,blockquote,pre{background:$quote!important;color:$foreground!important;border-color:$outline!important;}
    </style>""".trimIndent()
    val source = html.replace(Regex("<meta\\b[^>]*name\\s*=\\s*(['\"])viewport\\1[^>]*>", RegexOption.IGNORE_CASE), "")
    val head = source.indexOf("</head>", ignoreCase = true)
    return if (head >= 0) source.substring(0, head) + style + source.substring(head) else style + source
}
