package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.mvvm.viewmodel.SettingsListsViewModel.Account
import gov.anzong.androidnga.mvvm.viewmodel.SettingsListsViewModel.BlockedUser
import gov.anzong.androidnga.mvvm.viewmodel.SettingsListsViewModel.Keyword

/** 带返回按钮的二级页面框架。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSubPage(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {},
                            content: @Composable (PaddingValues) -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text(title) }, navigationIcon = { ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack) },
            actions = actions, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }, content = content)
}

@Composable
private fun EmptyHint(text: String, padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}

private val rowColors @Composable get() = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)

/** 关键词屏蔽：标题含已启用关键词的主题不在主题列表中显示。 */
@Composable
fun KeywordFilterScreen(keywords: List<Keyword>, onAdd: (String) -> Boolean, onToggle: (Int) -> Unit,
                        onRemove: (Int) -> Unit, onBack: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    var duplicate by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<Int?>(null) }
    fun add() {
        if (input.isBlank()) return
        if (onAdd(input)) { input = ""; duplicate = false } else duplicate = true
    }
    SettingsSubPage("关键词屏蔽", onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag("keyword_filter_screen")) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it; duplicate = false }, Modifier.weight(1f), singleLine = true,
                    label = { Text("新关键词") }, isError = duplicate,
                    supportingText = { Text(if (duplicate) "这个关键词已经添加过了" else "标题包含已启用关键词的主题不会显示") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions { add() })
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = ::add, enabled = input.isNotBlank()) { Text("添加") }
            }
            if (keywords.isEmpty()) {
                EmptyHint("还没有屏蔽关键词", PaddingValues())
            } else LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(keywords, key = { index, item -> "$index:${item.text}" }) { index, keyword ->
                    ListItem(headlineContent = { Text(keyword.text) },
                        supportingContent = { Text(if (keyword.enabled) "已启用" else "已停用") },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(keyword.enabled, onCheckedChange = { onToggle(index) })
                                ActionIcon(Icons.Default.Delete, "删除关键词${keyword.text}", { removing = index })
                            }
                        }, colors = rowColors)
                }
            }
        }
    }
    removing?.let { index ->
        ConfirmDialog("删除关键词", "是否删除“${keywords.getOrNull(index)?.text.orEmpty()}”？", "删除",
            { onRemove(index) }, { removing = null })
    }
}

/** 用户黑名单：被屏蔽用户的楼层不显示内容。 */
@Composable
fun BlacklistScreen(users: List<BlockedUser>, onOpen: (BlockedUser) -> Unit, onUnblock: (BlockedUser) -> Unit,
                    onClear: () -> Unit, onBack: () -> Unit) {
    var clearing by remember { mutableStateOf(false) }
    SettingsSubPage("用户黑名单", onBack, actions = {
        TextButton(onClick = { clearing = true }, enabled = users.isNotEmpty()) { Text("清空") }
    }) { padding ->
        if (users.isEmpty()) {
            EmptyHint("黑名单为空\n可以在帖子楼层的菜单中屏蔽用户", padding)
        } else LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("blacklist_screen")) {
            itemsIndexed(users, key = { _, user -> user.uid }) { _, user ->
                ListItem(headlineContent = { Text(user.name) }, supportingContent = { Text("UID ${user.uid}") },
                    trailingContent = { TextButton(onClick = { onUnblock(user) }) { Text("移除") } },
                    modifier = Modifier.clickable { onOpen(user) }, colors = rowColors)
            }
        }
    }
    if (clearing) ConfirmDialog("清空黑名单", "是否移除全部 ${users.size} 个用户？", "清空", onClear) { clearing = false }
}

/** 账号管理：切换当前账号、调整顺序、查看资料、移除与添加账号。 */
@Composable
fun AccountsScreen(accounts: List<Account>, avatarUrl: (Account) -> String?, onActivate: (Int) -> Unit,
                   onMove: (from: Int, to: Int) -> Unit, onOpen: (Account) -> Unit, onRemove: (Int) -> Unit,
                   onAdd: () -> Unit, onBack: () -> Unit) {
    var removing by remember { mutableStateOf<Int?>(null) }
    SettingsSubPage("账号管理", onBack, actions = { ActionIcon(Icons.Default.Add, "添加账号", onAdd) }) { padding ->
        if (accounts.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("还没有登录的账号", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAdd) { Text("添加账号") }
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("accounts_screen")) {
            itemsIndexed(accounts, key = { _, account -> account.uid }) { index, account ->
                var menu by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(account.name) },
                    supportingContent = { Text(if (account.active) "UID ${account.uid} · 当前账号" else "UID ${account.uid}") },
                    leadingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(account.active, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            AccountAvatar(HomeAccount(account.uid, account.name, avatarUrl(account)), 40.dp)
                        }
                    },
                    trailingContent = {
                        Box {
                            ActionIcon(Icons.Default.MoreVert, "${account.name}的操作", { menu = true })
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem({ Text("查看资料") }, { menu = false; onOpen(account) })
                                if (index > 0) DropdownMenuItem({ Text("上移") }, { menu = false; onMove(index, index - 1) })
                                if (index < accounts.lastIndex) DropdownMenuItem({ Text("下移") }, { menu = false; onMove(index, index + 1) })
                                DropdownMenuItem({ Text("移除账号") }, { menu = false; removing = index })
                            }
                        }
                    },
                    modifier = Modifier.clickable(role = Role.RadioButton, onClickLabel = "设为当前账号") { onActivate(index) },
                    colors = rowColors)
            }
        }
    }
    removing?.let { index ->
        ConfirmDialog("移除账号", "是否移除“${accounts.getOrNull(index)?.name.orEmpty()}”？之后需要重新登录。", "移除",
            { onRemove(index) }, { removing = null })
    }
}
