package gov.anzong.androidnga.ui.compose

import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.mvvm.viewmodel.HomeInboxViewModel
import sp.phone.http.bean.MessageThreadPageInfo
import sp.phone.mvp.model.entity.RecentReplyInfo
import sp.phone.util.StringUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeMessagesScreen(account: HomeAccount?, inbox: HomeInboxViewModel, replyCount: Int,
    onLogin: () -> Unit, onReply: (RecentReplyInfo) -> Unit, onMessage: (MessageThreadPageInfo) -> Unit,
    onCompose: () -> Unit, onRead: () -> Unit, onControlsExpandedChange: (Boolean) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(account?.uid, tab) {
        inbox.selectAccount(account?.uid.orEmpty())
        if (account != null) {
            if (tab == 0) inbox.loadReplies(onRead = onRead) else inbox.loadMessages()
        }
        onControlsExpandedChange(true)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnRead by rememberUpdatedState(onRead)
    DisposableEffect(lifecycle, account?.uid, tab) {
        // 注册时已处于 RESUMED 会立即补发 ON_RESUME；只在真正离开后返回时强制刷新，首次加载交给 LaunchedEffect。
        var paused = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) paused = true
            if (event == Lifecycle.Event.ON_RESUME && paused && account != null) {
                paused = false
                inbox.selectAccount(account.uid)
                if (tab == 0) inbox.loadReplies(force = true, onRead = currentOnRead) else inbox.loadMessages(force = true)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            listOf("回复通知", "短消息").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(title)
                        if (index == 0 && replyCount > 0 && account != null) Badge { Text(if (replyCount > 99) "99+" else replyCount.toString()) }
                    }
                })
            }
        }
        if (account == null) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("登录后查看回复通知和短消息")
                Button(onClick = onLogin, modifier = Modifier.padding(top = 16.dp)) { Text("登录 NGA 账号") }
            }
        } else key(tab) {
            val list = rememberLazyListState()
            val scroll = rememberScrollControlsConnection(list, onControlsExpandedChange)
            val loading = if (tab == 0) inbox.repliesLoading else inbox.messagesLoading
            val error = if (tab == 0) inbox.replyError else inbox.messageError
            val refresh = { if (tab == 0) inbox.loadReplies(force = true, onRead = onRead) else inbox.loadMessages(force = true) }
            PullToRefreshBox(isRefreshing = loading, onRefresh = refresh,
                modifier = Modifier.fillMaxSize().nestedScroll(scroll)) {
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("home_messages"),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalHomeBottomPadding.current), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (tab == 0) "最近的回复" else "短消息会话", style = MaterialTheme.typography.labelLarge)
                            if (tab == 1) TextButton(onClick = onCompose) { Text("写短消息") }
                            else TextButton(onClick = refresh, enabled = !loading) { Text("刷新") }
                        }
                    }
                    if (error != null) item {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { if (tab == 1) inbox.retryMessages() else refresh() }, enabled = !loading) { Text("重试") }
                    }
                    if (tab == 0) {
                        if (!loading && error == null && inbox.replies.isEmpty()) item { Text("暂无回复通知") }
                        items(inbox.replies, key = { "${it.tidStr}:${it.pidStr}" }) { reply ->
                            OutlinedCard(onClick = { onReply(reply) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(reply.userName.orEmpty(), style = MaterialTheme.typography.titleSmall)
                                    Text("回复了你的主题或评论", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(StringUtils.unEscapeHtml(reply.title.orEmpty()), style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    } else {
                        if (!loading && error == null && inbox.messages.isEmpty()) item { Text("暂无短消息") }
                        items(inbox.messages, key = { it.mid }) { message ->
                            OutlinedCard(onClick = { onMessage(message) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(message.from_username?.ifBlank { "系统消息" } ?: "系统消息", style = MaterialTheme.typography.titleSmall)
                                    Text(StringUtils.removeBrTag(StringUtils.unEscapeHtml(message.subject.orEmpty())), style = MaterialTheme.typography.bodyLarge)
                                    Text("${message.posts} 条消息 · ${message.lastTime.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (inbox.nextPage > 0) item { TextButton(onClick = { inbox.loadMessages(more = true) }, enabled = !loading) { Text("加载更多") } }
                    }
                }
            }
        }
    }
}
