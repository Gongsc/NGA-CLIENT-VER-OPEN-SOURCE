package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.util.StringUtils
import java.io.File

/** 离线缓存的主题：点击阅读，长按或删除按钮确认后删除；菜单中导入、导出缓存文件。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopicCacheScreen(topics: List<ThreadPageInfo>, loading: Boolean, onOpen: (ThreadPageInfo) -> Unit,
                     onDelete: (ThreadPageInfo) -> Unit, onImport: () -> Unit, onExport: () -> Unit, onBack: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ThreadPageInfo?>(null) }
    SettingsSubPage("我的缓存", onBack, actions = {
        Box {
            ActionIcon(Icons.Default.MoreVert, "更多操作", { menu = true })
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("导入缓存") }, onClick = { menu = false; onImport() }, enabled = !loading)
                DropdownMenuItem(text = { Text("导出缓存") }, onClick = { menu = false; onExport() }, enabled = !loading && topics.isNotEmpty())
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.fillMaxSize().testTag("topic_cache"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (topics.isNotEmpty()) item {
                    Text("仅保存在此设备 · 长按可删除缓存", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!loading && topics.isEmpty()) item {
                    Text("暂无缓存的帖子\n在帖子详情的菜单中可以缓存帖子", Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(topics, key = { it.tid }) { topic ->
                    val subject = StringUtils.unEscapeHtml(topic.subject.orEmpty())
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(topic) }, onLongClickLabel = "删除缓存",
                            onLongClick = { selected = topic })) {
                        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(subject, fontSize = PhoneConfiguration.getInstance().topicTitleSize.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Text(listOfNotNull(topic.board?.takeIf { it.isNotBlank() }, topic.author, "${topic.replies} 回复").joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ActionIcon(Icons.Default.Delete, "删除缓存$subject", { selected = topic })
                        }
                    }
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
    selected?.let { topic ->
        AlertDialog(onDismissRequest = { selected = null }, title = { Text("删除缓存？") },
            text = { Text("将删除“${StringUtils.unEscapeHtml(topic.subject.orEmpty())}”的离线缓存，删除后无法恢复。") },
            confirmButton = { TextButton(onClick = { selected = null; onDelete(topic) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("取消") } })
    }
}

/** 缓存目录中的页码文件（如 1.json），按页码排序；主题信息文件（文件名含 tid）与非数字文件不计入。 */
fun cachedArticlePages(dir: File, tid: String): List<Int> = dir.listFiles().orEmpty()
    .map { it.name }.filterNot { it.contains(tid) }
    .mapNotNull { it.removeSuffix(".json").toIntOrNull() }.sorted()

/**
 * 离线缓存阅读：每个缓存页一个分页，[page] 渲染对应页内容。
 * [bottomTabs] 对应“页码标签在底部”的设置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleCacheScreen(title: String, pages: List<Int>, bottomTabs: Boolean, onBack: () -> Unit,
                       page: @Composable (page: Int, active: Boolean) -> Unit) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val tabs = @Composable {
        if (pages.size > 1) PrimaryScrollableTabRow(selectedTabIndex = pager.currentPage, containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 8.dp) {
            pages.forEachIndexed { index, number ->
                Tab(selected = pager.currentPage == index, onClick = { scope.launch { pager.animateScrollToPage(index) } },
                    text = { Text(number.toString()) })
            }
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        Column {
            TopAppBar(title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
            if (!bottomTabs) tabs()
        }
    }, bottomBar = { if (bottomTabs) Box(Modifier.navigationBarsPadding()) { tabs() } }) { padding ->
        if (pages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text("没有找到这个帖子的缓存内容", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        HorizontalPager(pager, Modifier.fillMaxSize().padding(padding).testTag("article_cache_pager"), key = { pages[it] }) { index ->
            page(pages[index], index == pager.currentPage)
        }
    }
}
