package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.util.StringUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun historyDay(timestamp: Long, today: LocalDate = LocalDate.now()): String {
    if (timestamp <= 0) return "更早"
    val day = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (day) { today -> "今天"; today.minusDays(1) -> "昨天"; else -> day.toString() }
}

@Composable
fun HomeHistoryScreen(rows: List<ThreadPageInfo>, onOpen: (ThreadPageInfo) -> Unit,
    onRemove: (ThreadPageInfo) -> Unit, onClear: () -> Unit, onControlsExpandedChange: (Boolean) -> Unit) {
    val list = rememberLazyListState()
    val scroll = rememberScrollControlsConnection(list, onControlsExpandedChange)
    var clear by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ThreadPageInfo?>(null) }
    LazyColumn(Modifier.fillMaxSize().nestedScroll(scroll).testTag("home_history"), state = list,
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalHomeBottomPadding.current), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("本地浏览记录", Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { clear = true }, enabled = rows.isNotEmpty()) { Text("清空记录") }
            }
            Text("仅保存在此设备 · 长按可删除单条记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (rows.isEmpty()) item { Text("暂无浏览记录", Modifier.padding(vertical = 32.dp)) }
        rows.groupBy { historyDay(it.historyVisitedAt) }.forEach { (day, topics) ->
            item("day:$day") { Text(day, Modifier.padding(top = 16.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
            // 与 ThreadPageInfo.equals 一致：同一主题的整帖与单条回复记录可同时存在。
            items(topics, key = { "${it.tid}:${it.pid}" }) { topic ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(topic) }, onLongClickLabel = "删除记录", onLongClick = { selected = topic })) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(StringUtils.unEscapeHtml(topic.subject.orEmpty()), fontSize = PhoneConfiguration.getInstance().topicTitleSize.sp)
                        Text("${topic.author.orEmpty()} · ${topic.replies} 回复", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("上次读到第 ${topic.page.coerceAtLeast(1)} 页", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
    if (clear || selected != null) AlertDialog(onDismissRequest = { clear = false; selected = null },
        title = { Text(if (clear) "清空浏览记录？" else "删除这条记录？") },
        text = { Text("删除后无法恢复。") },
        confirmButton = { TextButton(onClick = { if (clear) onClear() else selected?.let(onRemove); clear = false; selected = null }) { Text("删除") } },
        dismissButton = { TextButton(onClick = { clear = false; selected = null }) { Text("取消") } })
}
