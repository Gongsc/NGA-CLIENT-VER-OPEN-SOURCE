package gov.anzong.androidnga.ui.compose

import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.activity.SearchActivity
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.mvvm.model.param.ParamKey
import gov.anzong.androidnga.task.SearchBoardTask

fun updatedSearchHistory(history: List<String>, query: String): List<String> =
    if (query.isBlank()) history else listOf(query) + history.filterNot { it == query }

fun installSearchContent(activity: SearchActivity) {
    activity.setContent { NgaTheme { SearchScreen(activity) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(activity: SearchActivity) {
    val preferences = remember { activity.getSharedPreferences(activity.packageName + "_preferences", android.content.Context.MODE_PRIVATE) }
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var currentBoard by rememberSaveable { mutableStateOf(true) }
    var withContent by rememberSaveable { mutableStateOf(false) }
    var userNameMode by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { active = false } }
    val key = when (mode) {
        1 -> PreferenceKey.KEY_SEARCH_HISTORY_BOARD
        2 -> PreferenceKey.KEY_SEARCH_HISTORY_USER
        else -> PreferenceKey.KEY_SEARCH_HISTORY_TOPIC
    }
    var history by remember(key) { mutableStateOf(runCatching {
        NgaJson.parseArray(preferences.getString(key, "[]"), String::class.java)?.toList().orEmpty().distinct()
    }.getOrDefault(emptyList())) }
    val focus = LocalFocusManager.current
    val fid = activity.intent.getIntExtra("fid", 0)
    fun saveHistory(items: List<String>) {
        history = items
        preferences.edit { putString(key, NgaJson.toJSONString(items)) }
    }
    fun search(input: String) {
        var term = input.trim().replace("\n", "")
        error = null
        if (mode == 2 && term.isEmpty()) {
            val user = UserManagerImpl.getInstance().activeUser
            term = if (userNameMode) user?.nickName.orEmpty() else user?.userId.orEmpty()
        }
        if (term.isEmpty()) { error = "请输入搜索内容"; return }
        focus.clearFocus()
        saveHistory(updatedSearchHistory(history, term))
        when (mode) {
            0 -> {
                val route = AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                    .withInt("content", if (withContent) 1 else 0).withString("key", term)
                if (currentBoard && fid != 0) route.withInt("fid", fid)
                route.navigation(activity)
            }
            1 -> {
                loading = true
                SearchBoardTask.execute(term) { board ->
                    if (active && !activity.isFinishing && !activity.isDestroyed) {
                        loading = false
                        if (board == null) error = "没有找到符合条件的版块，或网络连接失败"
                        else AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                            .withInt(ParamKey.KEY_FID, board.fid)
                            .withString(ParamKey.BOARD_HEAD, board.boardHead)
                            .withString(ParamKey.KEY_TITLE, board.name).navigation(activity)
                    }
                }
            }
            else -> {
                val userMode = if (userNameMode) "username" else "uid"
                AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_PROFILE)
                    .withString("mode", userMode).withString(userMode, term).navigation(activity)
            }
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("搜索") }, navigationIcon = {
            IconButton(onClick = { activity.finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            PrimaryTabRow(selectedTabIndex = mode) {
                listOf("主题", "版块", "用户").forEachIndexed { index, title ->
                    Tab(selected = mode == index, enabled = !loading, onClick = { mode = index; error = null }, text = { Text(title) })
                }
            }
            OutlinedTextField(value = query, onValueChange = { query = it; error = null },
                enabled = !loading, singleLine = true,
                label = { Text(if (mode == 2) { if (userNameMode) "用户名" else "用户 UID" } else "搜索关键词") },
                isError = error != null, supportingText = { error?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search(query) }),
                trailingIcon = { IconButton(onClick = { search(query) }, enabled = !loading) { Icon(Icons.Default.Search, "开始搜索") } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
            if (mode == 0) {
                if (fid != 0) ToggleRow("仅搜索当前版块", currentBoard) { currentBoard = it }
                ToggleRow("包含正文", withContent) { withContent = it }
            }
            if (mode == 2) ToggleRow("使用用户名搜索（关闭后使用 UID）", userNameMode) { userNameMode = it }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("搜索历史", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { saveHistory(emptyList()) }, enabled = history.isNotEmpty() && !loading) { Text("清空") }
            }
            LazyColumn(Modifier.weight(1f)) {
                if (history.isEmpty()) item { Text("暂无搜索历史", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(history, key = { it }) { term ->
                    ListItem(headlineContent = { Text(term) },
                        modifier = Modifier.clickable(enabled = !loading) { query = term; search(term) },
                        trailingContent = {
                            IconButton(onClick = { saveHistory(history.filterNot { it == term }) }, enabled = !loading) { Icon(Icons.Default.Close, "删除历史：$term") }
                        })
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
