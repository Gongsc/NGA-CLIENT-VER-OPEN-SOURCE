package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson.JSON
import gov.anzong.androidnga.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sp.phone.common.PhoneConfiguration
import sp.phone.mvp.model.ForumsListModel
import sp.phone.util.HttpUtil

// Literal matching deliberately accepts characters such as [, ( and *.
fun filterBoards(boards: List<BoardTile>, query: String): List<BoardTile> =
    boards.filter { it.name.contains(query.trim(), ignoreCase = true) }

class ForumDirectoryState {
    var boards by mutableStateOf(emptyList<BoardTile>())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
}

class ForumDirectoryViewModel : ViewModel() {
    val state = ForumDirectoryState()
    private var loaded = false

    fun load() {
        if (state.loading || loaded) return
        state.loading = true
        state.error = null
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val json = HttpUtil.getHtml(Utils.getNGAHost() + "app_api.php?__lib=home&__act=category", PhoneConfiguration.getInstance().cookie)
                    JSON.parseObject(json, ForumsListModel::class.java)
                }
                if (result?.result == null) state.error = "版块目录加载失败，请重试"
                else accept(result)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                state.error = "版块目录加载失败，请检查网络后重试"
            } finally {
                state.loading = false
            }
        }
    }

    fun accept(result: ForumsListModel) {
        state.boards = result.result.orEmpty().flatMap { it.groups.orEmpty() }
            .flatMap { it.forums.orEmpty() }
            .filter { !it.name.isNullOrBlank() }
            .map { BoardTile(it.id, it.stid, it.name) }.distinctBy { it.key }
        state.error = null
        loaded = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumDirectoryScreen(state: ForumDirectoryState, onBack: () -> Unit, onRetry: () -> Unit, onBoard: (BoardTile) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val boards = remember(state.boards, query) { filterBoards(state.boards, query) }
    Scaffold(topBar = {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background), title = { Text("版块目录") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
        }, actions = { ActionIcon(ActionIcons.Refresh, "刷新目录", onRetry, enabled = !state.loading) })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("筛选版块") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp))
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    ActionIcon(ActionIcons.Refresh, "重试", onRetry)
                }
                boards.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (query.isBlank()) "暂无版块" else "没有匹配的版块") }
                else -> BoardGrid(listOf(BoardSection("", boards)), onBoard)
            }
        }
    }
}
