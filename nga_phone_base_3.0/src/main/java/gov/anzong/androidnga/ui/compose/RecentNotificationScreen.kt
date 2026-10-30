package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.mvvm.model.entity.RecentReplyInfo

/** 回复通知页：下拉刷新、点击进入对应楼层、确认后清空全部通知。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentNotificationScreen(loggedIn: Boolean, replies: List<RecentReplyInfo>, loading: Boolean, error: String?,
                             isUnread: (RecentReplyInfo) -> Boolean, onRefresh: () -> Unit, onReply: (RecentReplyInfo) -> Unit,
                             onClear: () -> Unit, onLogin: () -> Unit, onBack: () -> Unit) {
    var clearing by remember { mutableStateOf(false) }
    SettingsSubPage("回复通知", onBack, actions = {
        TextButton(onClick = { clearing = true }, enabled = loggedIn && replies.isNotEmpty() && !loading) { Text("清空") }
    }) { padding ->
        if (!loggedIn) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text("登录后查看回复通知")
                Button(onClick = onLogin, modifier = Modifier.padding(top = 16.dp)) { Text("登录 NGA 账号") }
            }
            return@SettingsSubPage
        }
        PullToRefreshBox(isRefreshing = loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize().testTag("recent_notifications"), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error != null) item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRefresh, enabled = !loading) { Text("重试") }
                }
                if (!loading && error == null && replies.isEmpty()) item {
                    Text("暂无回复通知", Modifier.fillMaxWidth().padding(vertical = 32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(replies, key = { "${it.tidStr}:${it.pidStr}" }) { reply ->
                    ReplyNotificationCard(reply, isUnread(reply)) { onReply(reply) }
                }
            }
        }
    }
    if (clearing) AlertDialog(onDismissRequest = { clearing = false }, title = { Text("清空回复通知") },
        text = { Text("是否删除全部回复通知？删除后无法恢复。") },
        confirmButton = { TextButton(onClick = { clearing = false; onClear() }) { Text("清空") } },
        dismissButton = { TextButton(onClick = { clearing = false }) { Text("取消") } })
}
