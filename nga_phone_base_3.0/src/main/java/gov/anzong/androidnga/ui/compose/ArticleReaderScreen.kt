package gov.anzong.androidnga.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
    var controlsExpanded by mutableStateOf(true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleReaderScreen(state: ArticleReaderState, initialPage: Int, leftHand: Boolean,
                        onBack: () -> Unit, onRefresh: () -> Unit, onReply: () -> Unit,
                        onAction: (Int) -> Unit, onPage: (Int) -> Unit, content: @Composable (Int, Boolean) -> Unit) {
    val pager = rememberPagerState(initialPage = (initialPage - 1).coerceIn(0, state.pages - 1), pageCount = { state.pages })
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    LaunchedEffect(pager.settledPage) { state.controlsExpanded = true; state.currentPage = pager.settledPage; onPage(pager.settledPage + 1) }
    LaunchedEffect(state.jump) {
        state.jump?.let { target ->
            state.floor = target.floor
            pager.scrollToPage(target.pageIndex.coerceIn(0, state.pages - 1))
            state.jump = null
        }
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
    }, floatingActionButtonPosition = if (leftHand) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            ReaderFloatingActions(
                page = pager.currentPage + 1,
                pages = state.pages,
                leftHand = leftHand,
                expanded = state.controlsExpanded || state.showJump,
                scrolling = pager.isScrollInProgress,
                onJump = { state.showJump = true },
                onPrevious = { scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) } },
                onNext = { scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(state.pages - 1)) } },
                onReply = onReply,
            )
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
        }) { Text("跳转") } }, dismissButton = {
            Row {
                TextButton(onClick = { state.jump = ReaderPosition(0); state.showJump = false },
                    modifier = Modifier.semantics { contentDescription = "回到首页" }) { Text("回到首页") }
                TextButton(onClick = { state.showJump = false }) { Text("取消") }
            }
        })
    }
}

@Composable
private fun readerPageKeyHeight(): Dp = with(LocalDensity.current) {
    (MaterialTheme.typography.labelLarge.lineHeight.toDp() +
        MaterialTheme.typography.labelSmall.lineHeight.toDp() + 12.dp).coerceAtLeast(48.dp)
}

@Composable
private fun readerReplyHeight(): Dp = with(LocalDensity.current) {
    (MaterialTheme.typography.labelLarge.lineHeight.toDp() + 32.dp).coerceAtLeast(56.dp)
}

// 分页宿主与正文位于不同的 ComposeView，使用相同尺寸计算末尾留白。
@Composable
fun readerActionsBottomPadding(): Dp = readerPageKeyHeight() + 96.dp + readerReplyHeight() + 48.dp

@Composable
private fun ReaderFloatingActions(
    page: Int, pages: Int, leftHand: Boolean, expanded: Boolean, scrolling: Boolean,
    onJump: () -> Unit, onPrevious: () -> Unit, onNext: () -> Unit, onReply: () -> Unit,
) {
    val keyHeight = readerPageKeyHeight()
    Column(
        modifier = Modifier.testTag("reader_pagination"),
        horizontalAlignment = if (leftHand) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onJump,
                modifier = Modifier.widthIn(min = 48.dp).heightIn(min = keyHeight)
                    .semantics {
                        contentDescription = "跳转页码或楼层"
                        stateDescription = "第 $page 页，共 $pages 页"
                    },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 2.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$page", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    Text("/ $pages", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = onPrevious, enabled = page > 1 && !scrolling,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Icon(Icons.Default.KeyboardArrowUp, "上一页") }
                    FilledTonalIconButton(
                        onClick = onNext, enabled = page < pages && !scrolling,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Icon(Icons.Default.KeyboardArrowDown, "下一页") }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onReply,
            expanded = expanded,
            modifier = Modifier.heightIn(min = readerReplyHeight()).semantics { contentDescription = "回复主题" },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(ActionIcons.Reply, contentDescription = null) },
            text = { Text("回复", maxLines = 1) },
        )
    }
}
