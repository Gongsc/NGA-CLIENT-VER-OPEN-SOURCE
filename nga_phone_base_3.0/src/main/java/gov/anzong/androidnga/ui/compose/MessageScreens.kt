package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import sp.phone.http.bean.MessageArticlePageInfo
import sp.phone.http.bean.MessageThreadPageInfo
import sp.phone.util.StringUtils

/** 一条短消息会话：对方、标题、条数与最后时间。 */
@Composable
fun MessageThreadCard(message: MessageThreadPageInfo, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(message.from_username?.ifBlank { "系统消息" } ?: "系统消息", style = MaterialTheme.typography.titleSmall)
            Text(StringUtils.removeBrTag(StringUtils.unEscapeHtml(message.subject.orEmpty())), style = MaterialTheme.typography.bodyLarge)
            Text("${message.posts} 条消息 · ${message.lastTime.orEmpty()}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 列表滚动到最后几项时请求下一页；是否真的加载由调用方判断。 */
@Composable
private fun LoadMoreWhenNearEnd(state: androidx.compose.foundation.lazy.LazyListState, onLoadMore: () -> Unit) {
    val currentLoadMore by rememberUpdatedState(onLoadMore)
    val nearEnd by remember(state) {
        derivedStateOf {
            val info = state.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: 0) >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) currentLoadMore() }
}

/** 短消息会话列表：多账号时可在标题栏切换账号，滚动到底部自动加载下一页。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(accounts: List<String>, activeAccount: Int, messages: List<MessageThreadPageInfo>, loading: Boolean,
                      error: String?, hasMore: Boolean, onSwitchAccount: (Int) -> Unit, onRefresh: () -> Unit, onRetry: () -> Unit,
                      onLoadMore: () -> Unit, onOpen: (MessageThreadPageInfo) -> Unit, onCompose: () -> Unit,
                      onLogin: () -> Unit, onBack: () -> Unit) {
    val loggedIn = accounts.isNotEmpty()
    var choosing by remember { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = {
            Column {
                Text("短消息")
                accounts.getOrNull(activeAccount)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }, navigationIcon = { ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack) },
            actions = {
                if (accounts.size > 1) Box {
                    ActionIcon(Icons.Default.AccountCircle, "切换账号", { choosing = true })
                    DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                        accounts.forEachIndexed { index, name ->
                            DropdownMenuItem(text = { Text(name, fontWeight = if (index == activeAccount) FontWeight.Bold else null) },
                                onClick = { choosing = false; if (index != activeAccount) onSwitchAccount(index) })
                        }
                    }
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }, floatingActionButton = {
        if (loggedIn) ExtendedFloatingActionButton(onClick = onCompose, icon = { Icon(Icons.Default.Edit, null) }, text = { Text("写短消息") })
    }) { padding ->
        if (!loggedIn) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text("登录后查看短消息")
                Button(onClick = onLogin, modifier = Modifier.padding(top = 16.dp)) { Text("登录 NGA 账号") }
            }
            return@Scaffold
        }
        val list = rememberLazyListState()
        if (hasMore && !loading && error == null) LoadMoreWhenNearEnd(list, onLoadMore)
        PullToRefreshBox(isRefreshing = loading && messages.isEmpty(), onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("message_list"),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!loading && error == null && messages.isEmpty()) item {
                    Text("暂无短消息", Modifier.fillMaxWidth().padding(vertical = 32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(messages, key = { it.mid }) { message -> MessageThreadCard(message) { onOpen(message) } }
                if (error != null) item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry, enabled = !loading) { Text("重试") }
                }
                if (loading && messages.isNotEmpty()) item {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
                }
            }
        }
    }
}

/** 发信人名称：被禁言账号标注“传说”，-1 声望（VIP）标注 VIP，与旧页面一致。 */
internal fun messageAuthorLabel(entry: MessageArticlePageInfo): String {
    val name = entry.author.orEmpty()
    return when {
        entry.yz == "-1" -> "$name(VIP)"
        !entry.mute_time.isNullOrEmpty() && entry.mute_time != "0" -> "$name(传说)"
        else -> name
    }
}

/** 短消息会话内容：每条消息的正文仍由 WebView 渲染；滚动到底部加载下一页。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(title: String, entries: List<MessageArticlePageInfo>, loading: Boolean, error: String?, hasNextPage: Boolean,
                        onRefresh: () -> Unit, onLoadMore: () -> Unit, onReply: () -> Unit, onBack: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text(title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
            navigationIcon = { ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }, floatingActionButton = {
        if (entries.isNotEmpty()) FloatingActionButton(onClick = onReply) { Icon(Icons.AutoMirrored.Filled.Send, "回复") }
    }) { padding ->
        val list = rememberLazyListState()
        if (hasNextPage && !loading && error == null && entries.isNotEmpty()) LoadMoreWhenNearEnd(list, onLoadMore)
        PullToRefreshBox(isRefreshing = loading && entries.isNotEmpty() && !hasNextPage, onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("message_detail"),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries.size) { index ->
                    val entry = entries[index]
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val special = entry.yz == "-1" || (!entry.mute_time.isNullOrEmpty() && entry.mute_time != "0")
                                Text(messageAuthorLabel(entry), style = MaterialTheme.typography.titleSmall,
                                    color = if (special) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                Text("#${entry.lou}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.weight(1f))
                                Text(entry.time.orEmpty(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ArticleHtml(entry.formated_html_data.orEmpty(), emptyList(),
                                background = MaterialTheme.colorScheme.surfaceContainerLow)
                        }
                    }
                }
                if (error != null) item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = if (entries.isEmpty()) onRefresh else onLoadMore, enabled = !loading) { Text("重试") }
                }
                if (loading && (entries.isEmpty() || hasNextPage)) item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
                }
                if (!loading && error == null && entries.isNotEmpty() && !hasNextPage) item {
                    Text("已加载到短消息内容的最后一页", Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** 写短消息或回复：新消息需要收件人；发送中禁用输入与重复提交。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagePostScreen(newMessage: Boolean, initialRecipient: String?, sending: Boolean,
                      onSend: (title: String, recipient: String, body: String) -> Unit, onBack: () -> Unit) {
    var recipient by rememberSaveable { mutableStateOf(initialRecipient.orEmpty()) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var recipientError by rememberSaveable { mutableStateOf(false) }
    var bodyError by rememberSaveable { mutableStateOf(false) }
    val bodyFocus = remember { FocusRequester() }
    fun submit() {
        recipientError = newMessage && recipient.isBlank()
        bodyError = body.isBlank()
        if (!recipientError && !bodyError) onSend(title, recipient, body)
    }
    SettingsSubPage(if (newMessage) "发新消息" else "回短消息", onBack, actions = {
        if (sending) CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).size(24.dp))
        else ActionIcon(Icons.AutoMirrored.Filled.Send, "发送", ::submit)
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (newMessage) OutlinedTextField(recipient, { recipient = it; recipientError = false }, Modifier.fillMaxWidth(),
                enabled = !sending, singleLine = true, label = { Text("收件人") },
                placeholder = { Text("ID/用户名，多人用逗号分隔") }, isError = recipientError,
                supportingText = if (recipientError) ({ Text("请输入收件人") }) else null,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false))
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), enabled = !sending, singleLine = true, label = { Text("标题") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None))
            OutlinedTextField(body, { body = it; bodyError = false }, Modifier.fillMaxWidth().weight(1f).focusRequester(bodyFocus),
                enabled = !sending, label = { Text("内容") }, isError = bodyError,
                supportingText = if (bodyError) ({ Text("请输入内容") }) else null)
        }
    }
    LaunchedEffect(Unit) { runCatching { bodyFocus.requestFocus() } }
}
