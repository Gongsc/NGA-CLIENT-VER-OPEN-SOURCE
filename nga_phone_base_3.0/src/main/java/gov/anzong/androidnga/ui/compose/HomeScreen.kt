package gov.anzong.androidnga.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
    favorites: ForumFavoritesSnapshot = ForumFavoritesSnapshot()
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var editing by rememberSaveable { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    val category = state.categories.getOrNull(state.selected)
    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
    ModalNavigationDrawer(drawerState = drawer, drawerContent = {
        ModalDrawerSheet {
            HomeDrawerContent(state, actions,
                onAction = { id -> scope.launch { drawer.close() }; onAction(id) },
                onProfile = { scope.launch { drawer.close() }; onProfile() },
                onSwitchUser = onSwitchUser)
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
