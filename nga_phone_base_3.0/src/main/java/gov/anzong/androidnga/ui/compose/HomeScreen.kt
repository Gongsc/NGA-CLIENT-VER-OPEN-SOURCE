package gov.anzong.androidnga.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
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
    }, bottomBar = {
        Column {
            AnimatedVisibility(controlsVisible, enter = expandVertically(), exit = shrinkVertically()) {
                NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0), modifier = Modifier.testTag("home_bottom_navigation")) {
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
            // 底栏收起时仍保留系统手势区，不让列表落入系统导航区域。
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            savedPages.SaveableStateProvider(destination) {
                when (destination) {
                    1 -> historyContent { controlsVisible = it }
                    2 -> key(state.account?.uid) { messagesContent { controlsVisible = it } }
                    3 -> HomeProfileContent(state, actions, onAction, onProfile, onSwitchUser) { controlsVisible = it }
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
                            val sections = if (category.bookmark) listOf(BoardSection("", favorites.boards.map {
                                BoardTile(it.fid, it.stid, it.name, it.head)
                            })) else category.sections
                            BoardGrid(sections, onBoard, editing = editing && category.bookmark, onRemove = onRemove,
                                onControlsExpandedChange = { controlsVisible = it }, enabled = !favorites.loading && !favorites.mutating,
                                emptyMessage = if (category.bookmark) {
                                    if (favorites.accountId.isEmpty()) "登录后可查看论坛收藏" else "暂无论坛收藏，可在版块菜单中收藏"
                                } else "暂无版块")
                        }
                    }
                }
            }
        }
    }
}
