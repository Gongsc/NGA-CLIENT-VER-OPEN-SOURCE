package gov.anzong.androidnga.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sp.phone.http.favorites.ForumFavoritesSnapshot

class HomeState {
    var categories by mutableStateOf(emptyList<HomeCategory>())
    var selected by mutableStateOf(0)
    var userName by mutableStateOf("未登录")
    var userCount by mutableStateOf(0)
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
    favorites: ForumFavoritesSnapshot = ForumFavoritesSnapshot()
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val appearance = LocalAppearance.current
    val category = state.categories.getOrNull(state.selected)
    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
    ModalNavigationDrawer(drawerState = drawer, drawerContent = {
        ModalDrawerSheet {
            LazyColumn(Modifier.widthIn(max = 360.dp).testTag("home_navigation")) {
                item {
                    Column(Modifier.padding(24.dp)) {
                        Text("NGA", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = onProfile) { Text(state.userName) }
                        if (state.userCount > 1) TextButton(onClick = onSwitchUser) { Text("切换账号（${state.userCount}）") }
                        if (state.replyCount > 0) Text("新回复：${state.replyCount}")
                    }
                    HorizontalDivider()
                }
                homeNavigationGroups(actions).forEach { group ->
                    item("heading_${group.title}") {
                        Text(group.title, modifier = Modifier.padding(start = 28.dp, top = 20.dp, bottom = 8.dp).semantics { heading() },
                            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(group.actions, key = { it.id }) { action ->
                        NavigationDrawerItem(label = { Text(action.label) }, selected = false,
                            onClick = { if(action.enabled) {scope.launch { drawer.close() }; onAction(action.id)} },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
            TopAppBar(title = { Text("NGA", style = MaterialTheme.typography.headlineSmall) }, navigationIcon = {
                ActionIcon(Icons.Default.Menu, "打开导航", { scope.launch { drawer.open() } })
            }, actions = {
                ActionIcon(Icons.Default.Search, "搜索", onSearch)
                if (category?.bookmark == true) ActionIcon(ActionIcons.Refresh,
                    if (favorites.accountId.isEmpty()) "登录并同步论坛收藏" else "同步论坛收藏", onSyncFavorites,
                    enabled = !favorites.loading && !favorites.mutating)
                if (category?.bookmark == true && favorites.accountId.isNotEmpty()) Box {
                    ActionIcon(Icons.Default.MoreVert, "首页操作", { moreOpen = true })
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        if (category?.bookmark == true && favorites.accountId.isNotEmpty()) DropdownMenuItem(
                            text = { Text(if (editing) "完成管理" else "管理收藏") }, onClick = { editing = !editing; moreOpen = false })
                    }
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (state.categories.isNotEmpty()) {
                    PrimaryScrollableTabRow(containerColor = MaterialTheme.colorScheme.background, selectedTabIndex = state.selected.coerceIn(state.categories.indices), edgePadding = 8.dp) {
                        state.categories.forEachIndexed { index, item ->
                            Tab(selected = state.selected == index, onClick = { state.selected = index; editing = false }, text = { Text(item.name) })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 64.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(if (category?.bookmark == true) "论坛收藏 · ${favorites.boards.size}" else "版块目录",
                        Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (editing) ActionIcon(Icons.Default.Check, "完成管理", { editing = false })
                    DensityToggle(appearance.compact, appearance::chooseCompact)
                }
                if (category?.bookmark == true) {
                    if (favorites.loading || favorites.mutating) LinearProgressIndicator(Modifier.fillMaxWidth())
                    favorites.error?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                if (category != null) {
                    key(state.selected) {
                        val sections = if (category.bookmark) listOf(BoardSection("", favorites.boards.map {
                            BoardTile(it.fid, it.stid, it.name, it.head)
                        })) else category.sections
                        BoardGrid(sections, onBoard, editing = editing && category.bookmark, onRemove = onRemove,
                            enabled = !favorites.loading && !favorites.mutating,
                            emptyMessage = if (category.bookmark) {
                                if (favorites.accountId.isEmpty()) "登录后可查看论坛收藏" else "暂无论坛收藏，可在版块菜单中收藏"
                            } else "暂无版块")
                    }
                }
            }
        }
    }
}
