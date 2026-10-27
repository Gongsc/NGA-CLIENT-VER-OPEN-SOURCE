package gov.anzong.androidnga.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import sp.phone.http.favorites.ForumFavoritesSnapshot

class HomeState {
    var categories by mutableStateOf(emptyList<HomeCategory>())
    var selected by mutableStateOf(0)
    var account by mutableStateOf<HomeAccount?>(null)
    var otherAccounts by mutableStateOf(emptyList<HomeAccount>())
    var replyCount by mutableStateOf(0)
}
data class HomeCategory(val name: String, val bookmark: Boolean, val sections: List<BoardSection>)
data class HomeAction(val id: Int, val label: String, val enabled: Boolean = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState, actions: List<HomeAction>, onAction: (Int) -> Unit,
    onBoard: (BoardTile) -> Unit, onSearch: () -> Unit, onProfile: () -> Unit,
    onSwitchUser: () -> Unit, onSyncFavorites: () -> Unit = {}, onRemove: (BoardTile) -> Unit,
    favorites: ForumFavoritesSnapshot = ForumFavoritesSnapshot(),
    historyContent: @Composable ((Boolean) -> Unit) -> Unit = { Text("暂无浏览记录") },
    messagesContent: @Composable ((Boolean) -> Unit) -> Unit = { Text("暂无消息") }
) {
    var destination by rememberSaveable { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(true) }
    val savedPages = rememberSaveableStateHolder()
    var editing by rememberSaveable { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val category = state.categories.getOrNull(state.selected)
    val titles = listOf("首页", "历史", "消息", "我的")
    BackHandler(destination != 0) { destination = 0; controlsVisible = true }
    LaunchedEffect(destination, state.selected) { controlsVisible = true }
    val setControls: (Boolean) -> Unit = remember { { controlsVisible = it } }
    var barHeight by remember { mutableIntStateOf(0) }
    // 动画进度只在绘制阶段读取；组合阶段只关心底栏是否已完全移出，避免动画期间逐帧重组首页。
    val barHidden = animateFloatAsState(if (controlsVisible) 0f else 1f, label = "home_bottom_bar")
    val barGone by remember { derivedStateOf { !controlsVisible && barHidden.value >= 1f } }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = {
            Text(if (destination == 0) "NGA" else titles[destination], style = MaterialTheme.typography.headlineSmall)
        }, actions = {
            if (destination == 0) {
                ActionIcon(Icons.Default.Search, "搜索", onSearch)
                if (category?.bookmark == true) {
                    ActionIcon(ActionIcons.Refresh,
                        if (favorites.accountId.isEmpty()) "登录并同步论坛收藏" else "同步论坛收藏", onSyncFavorites,
                        enabled = !favorites.loading && !favorites.mutating)
                    if (favorites.accountId.isNotEmpty()) Box {
                        ActionIcon(Icons.Default.MoreVert, "首页操作", { moreOpen = true })
                        DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                            DropdownMenuItem(text = { Text(if (editing) "完成管理" else "管理收藏") },
                                onClick = { editing = !editing; moreOpen = false })
                        }
                    }
                }
            }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }, contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)) { padding ->
        // 底栏浮在内容上方，只做位移动画：内容区高度不随底栏显隐变化，滚动时不会逐帧重新布局列表。
        // 系统导航安全区始终保留，不让列表落入手势区域。
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)).clipToBounds()) {
            CompositionLocalProvider(LocalHomeBottomPadding provides with(LocalDensity.current) { barHeight.toDp() }) {
                savedPages.SaveableStateProvider(destination) {
                    when (destination) {
                        1 -> historyContent(setControls)
                        2 -> key(state.account?.uid) { messagesContent(setControls) }
                        3 -> HomeProfileContent(state, actions, onAction, onProfile, onSwitchUser, setControls)
                        else -> Column(Modifier.fillMaxSize()) {
                            if (state.categories.isNotEmpty()) {
                                PrimaryScrollableTabRow(containerColor = MaterialTheme.colorScheme.background,
                                    selectedTabIndex = state.selected.coerceIn(state.categories.indices), edgePadding = 8.dp) {
                                    state.categories.forEachIndexed { index, item ->
                                        Tab(selected = state.selected == index,
                                            onClick = { state.selected = index; editing = false }, text = { Text(item.name) })
                                    }
                                }
                            }
                            if (category?.bookmark == true) {
                                if (favorites.loading || favorites.mutating) LinearProgressIndicator(Modifier.fillMaxWidth())
                                favorites.error?.let { message ->
                                    Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                }
                            }
                            if (category != null) key(state.selected) {
                                val sections = remember(category, favorites.boards) {
                                    if (category.bookmark) listOf(BoardSection("", favorites.boards.map {
                                        BoardTile(it.fid, it.stid, it.name, it.head)
                                    })) else category.sections
                                }
                                BoardGrid(sections, onBoard, editing = editing && category.bookmark, onRemove = onRemove,
                                    onControlsExpandedChange = setControls, enabled = !favorites.loading && !favorites.mutating,
                                    emptyMessage = if (category.bookmark) {
                                        if (favorites.accountId.isEmpty()) "登录后可查看论坛收藏" else "暂无论坛收藏，可在版块菜单中收藏"
                                    } else "暂无版块")
                            }
                        }
                    }
                }
            }
            if (!barGone) NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.align(Alignment.BottomCenter).testTag("home_bottom_navigation")
                    .onSizeChanged { barHeight = it.height }
                    .graphicsLayer { translationY = size.height * barHidden.value }) {
                titles.forEachIndexed { index, title ->
                    NavigationBarItem(selected = destination == index,
                        onClick = { destination = index; controlsVisible = true },
                        modifier = Modifier.testTag("home_tab_$index"), label = { Text(title) }, icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Home, null)
                                1 -> Icon(ActionIcons.Restore, null)
                                2 -> BadgedBox(badge = {
                                    if (state.replyCount > 0 && state.account != null) Badge {
                                        Text(if (state.replyCount > 99) "99+" else state.replyCount.toString())
                                    }
                                }) { Icon(ActionIcons.Reply, null) }
                                else -> key(state.account?.uid, state.account?.avatarUrl) { AccountAvatar(state.account, 26.dp) }
                            }
                        })
                }
            }
        }
    }
}

/** 首页底栏占用的高度；各一级页面的列表据此留出底部内边距，保证最后一项可滚到底栏上方。 */
internal val LocalHomeBottomPadding = compositionLocalOf { 0.dp }
