package gov.anzong.androidnga.ui.compose

import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import gov.anzong.androidnga.R
import sp.phone.common.PhoneConfiguration
import sp.phone.util.ImageUtils

@Composable
internal fun HomeDrawerContent(
    state: HomeState, actions: List<HomeAction>, onAction: (Int) -> Unit,
    onProfile: () -> Unit, onSwitchUser: () -> Unit
) {
    val account = state.account
    val groups = homeNavigationGroups(actions, loggedIn = account != null)
    val accountGroups = groups.filter { it.tier == HomeNavigationTier.ACCOUNT }
    val login = { onAction(R.id.menu_login) }
    LazyColumn(Modifier.widthIn(max = 360.dp).testTag("home_navigation")) {
        item("account_header") { AccountHeader(state, onProfile, onSwitchUser, login) }
        if (account == null || accountGroups.isNotEmpty()) {
            item("tier_account") { TierHeading("账号", Icons.Default.Person, MaterialTheme.colorScheme.primary) }
            item("account_card") {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        if (account == null) DrawerEntry("消息、我的主题与收藏需登录", Icons.Default.Lock, login, muted = true)
                        accountGroups.forEach { group ->
                            if (group.title.isNotEmpty()) GroupHeading(group.title)
                            group.actions.forEach { action ->
                                DrawerEntry(action.label, drawerIcon(action.id), { if (action.enabled) onAction(action.id) },
                                    badge = if (action.id == R.id.menu_gun) state.replyCount else 0)
                            }
                        }
                    }
                }
            }
        }
        groups.filter { it.tier == HomeNavigationTier.TOOLS }.forEach { group ->
            item("tier_${group.title}") { TierHeading(group.title, Icons.Default.Build, MaterialTheme.colorScheme.onSurfaceVariant) }
            items(group.actions, key = { it.id }) { action ->
                Box(Modifier.padding(horizontal = 4.dp)) {
                    DrawerEntry(action.label, drawerIcon(action.id), { if (action.enabled) onAction(action.id) })
                }
            }
        }
        groups.filter { it.tier == HomeNavigationTier.APP }.forEach { group ->
            item("tier_${group.title}") {
                Column(Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    GroupHeading(group.title, Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
                    group.actions.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { action -> AppTile(action, drawerIcon(action.id), Modifier.weight(1f)) { if (action.enabled) onAction(action.id) } }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun AccountHeader(state: HomeState, onProfile: () -> Unit, onSwitchUser: () -> Unit, onLogin: () -> Unit) {
    val account = state.account
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 4.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .clickable(onClickLabel = if (account == null) "登录" else "查看个人资料") { if (account == null) onLogin() else onProfile() }
            .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AccountAvatar(account, 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account?.name ?: "未登录", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(account?.let { "UID ${it.uid}" } ?: "登录后同步收藏与消息",
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            if (account != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
        }
        if (account == null) {
            Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) { Text("登录 NGA 账号") }
        } else Row(Modifier.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.otherAccounts.isNotEmpty()) TextButton(onClick = onSwitchUser) {
                val stacked = state.otherAccounts.take(3)
                Box(Modifier.width((24 + 16 * (stacked.size - 1)).dp)) {
                    stacked.forEachIndexed { index, other ->
                        AccountAvatar(other, 24.dp, Modifier.offset(x = (16 * index).dp).border(2.dp, scheme.surface, CircleShape))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("切换账号（${state.otherAccounts.size + 1}）")
            }
            TextButton(onClick = onLogin) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加账号")
            }
        }
    }
}

@Composable
private fun AccountAvatar(account: HomeAccount?, size: Dp, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = Modifier.size(size).then(modifier).clip(CircleShape)
    when {
        account == null -> Surface(shape, color = scheme.outlineVariant, contentColor = scheme.onSurfaceVariant) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, Modifier.size(size / 2)) }
        }
        account.avatarUrl.isNullOrBlank() -> Surface(shape, color = scheme.primary, contentColor = scheme.onPrimary) {
            Box(contentAlignment = Alignment.Center) {
                Text(accountInitial(account.name), style = if (size >= 40.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelSmall)
            }
        }
        else -> AndroidView(factory = { ImageView(it).apply { importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO } },
            modifier = shape, update = { image ->
                ImageUtils.loadRoundCornerAvatar(image, account.avatarUrl, !PhoneConfiguration.getInstance().isAvatarLoadEnabled)
            })
    }
}

@Composable
private fun TierHeading(title: String, icon: ImageVector, color: Color) {
    Row(Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp).semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(6.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun GroupHeading(title: String, modifier: Modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 2.dp)) {
    Text(title, modifier = modifier.semantics { heading() }, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DrawerEntry(label: String, icon: ImageVector, onClick: () -> Unit, badge: Int = 0, muted: Boolean = false) {
    NavigationDrawerItem(
        label = { Text(label, color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified) },
        selected = false, onClick = onClick,
        icon = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        badge = if (badge > 0) ({ Badge { Text(if (badge > 99) "99+" else badge.toString()) } }) else null,
        modifier = Modifier.padding(horizontal = 6.dp).height(48.dp)
    )
}

@Composable
private fun AppTile(action: HomeAction, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 64.dp), shape = RoundedCornerShape(12.dp),
        color = Color.Transparent, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(vertical = 8.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(action.label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

private fun drawerIcon(id: Int): ImageVector = when (id) {
    R.id.menu_gun -> Icons.Default.Notifications
    R.id.menu_msg -> Icons.Default.Email
    R.id.menu_post -> Icons.AutoMirrored.Filled.List
    R.id.menu_reply -> Icons.Default.Edit
    R.id.menu_bookmark -> Icons.Default.Star
    R.id.menu_download -> ActionIcons.Download
    R.id.menu_add -> Icons.Default.Search
    R.id.menu_add_id -> Icons.Default.Add
    R.id.menu_forward -> ActionIcons.Link
    R.id.menu_search -> Icons.Default.Person
    R.id.menu_appearance -> ActionIcons.Appearance
    R.id.menu_setting -> Icons.Default.Settings
    R.id.menu_about -> Icons.Default.Info
    else -> Icons.Default.Build
}
