package gov.anzong.androidnga.ui.compose

import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import gov.anzong.androidnga.mvvm.viewmodel.ProfileViewModel
import sp.phone.common.PhoneConfiguration
import sp.phone.http.bean.AdminForumsData
import sp.phone.http.bean.ProfileData
import sp.phone.task.ProfileStatus
import sp.phone.util.ImageUtils

/** 资料页菜单项；可见性由资料是否加载、是否本人决定。 */
enum class ProfileAction(val label: String) {
    SearchTopics("搜索发帖"), SearchReplies("搜索回复"), SendMessage("发送短消息"), ModifyAvatar("修改头像"),
    ToggleBlock("屏蔽此人"), CopyUrl("复制链接"), OpenInBrowser("通过浏览器打开"),
}

internal fun profileActions(loaded: Boolean, currentUser: Boolean): List<ProfileAction> = ProfileAction.entries.filter {
    when (it) {
        ProfileAction.SearchTopics, ProfileAction.SearchReplies, ProfileAction.ToggleBlock -> loaded
        ProfileAction.SendMessage -> loaded && !currentUser
        ProfileAction.ModifyAvatar -> loaded && currentUser
        ProfileAction.CopyUrl, ProfileAction.OpenInBrowser -> true
    }
}

/**
 * 用户资料：头像与状态、基本信息、声望、管理版块和签名；签名仍由 WebView 渲染论坛代码。
 * [signatureHtml] 为解码后的签名网页，为空表示没有签名。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(profile: ProfileData?, signatureHtml: String?, loading: Boolean, error: String?, currentUser: Boolean,
                  blocked: Boolean, onAction: (ProfileAction) -> Unit, onEditSignature: () -> Unit,
                  onOpenForum: (AdminForumsData) -> Unit, onRefresh: () -> Unit, onBack: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text(profile?.userName ?: "用户资料") },
            navigationIcon = { ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack) },
            actions = {
                Box {
                    ActionIcon(Icons.Default.MoreVert, "更多操作", { menu = true })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        profileActions(profile != null, currentUser).forEach { action ->
                            val label = if (action == ProfileAction.ToggleBlock && blocked) "取消屏蔽此人" else action.label
                            DropdownMenuItem(text = { Text(label) }, onClick = { menu = false; onAction(action) })
                        }
                    }
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { padding ->
        PullToRefreshBox(isRefreshing = loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("profile"),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRefresh, enabled = !loading) { Text("重试") }
                }
                if (profile == null) return@Column
                ProfileHeader(profile, currentUser)
                if (profile.buffs.isNotEmpty()) ProfileSection("状态") {
                    profile.buffs.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
                ProfileSection("基本信息") {
                    InfoRow("用户组", profile.memberGroup)
                    InfoRow("发帖数", profile.postCount)
                    InfoRow("注册时间", profile.registerDate)
                    InfoRow("邮箱", profile.emailAddress)
                    InfoRow("电话", profile.phoneNumber)
                    val (gold, silver, copper) = ProfileViewModel.money(profile.money)
                    InfoRow("金钱", "$gold 金 $silver 银 $copper 铜")
                }
                ProfileSection("声望") {
                    InfoRow("威望", ProfileViewModel.fame(profile.frame))
                    profile.reputationEntryList.orEmpty().forEach { InfoRow(it.name.orEmpty(), it.data) }
                }
                ProfileSection("管理版块") {
                    val forums = profile.adminForums.orEmpty()
                    if (forums.isEmpty()) Text("无管理版块", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        forums.forEach { forum -> AssistChip(onClick = { onOpenForum(forum) }, label = { Text(forum.forumName.orEmpty()) }) }
                    }
                }
                ProfileSection("签名", action = if (currentUser) ({ TextButton(onClick = onEditSignature) { Text("修改签名") } }) else null) {
                    if (signatureHtml.isNullOrBlank()) Text("暂无签名", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else ArticleHtml(signatureHtml, emptyList(), background = MaterialTheme.colorScheme.surfaceContainerLow)
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: ProfileData, currentUser: Boolean) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val avatar = sp.phone.util.FunctionUtils.parseAvatarUrl(profile.avatarUrl)
            Box(Modifier.size(72.dp).clip(CircleShape)) {
                AndroidView(factory = { ImageView(it).apply { importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO } },
                    modifier = Modifier.fillMaxSize(), update = { image ->
                        ImageUtils.loadRoundCornerAvatar(image, avatar, !PhoneConfiguration.getInstance().isAvatarLoadEnabled)
                    })
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.userName.orEmpty(), style = MaterialTheme.typography.titleLarge)
                Text("用户ID：${profile.uid.orEmpty()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // 与网页一致：他人的激活状态只在被禁用、锁定或注销时显示
                profile.active?.takeIf { currentUser || it.level == ProfileStatus.Level.BANNED }?.let { active ->
                    val color = when (active.level) {
                        ProfileStatus.Level.NORMAL -> Color(0xFF3D9F0E)
                        ProfileStatus.Level.WARNING -> MaterialTheme.colorScheme.tertiary
                        ProfileStatus.Level.BANNED -> MaterialTheme.colorScheme.error
                    }
                    Text(listOf(active.label, active.description).filter { it.isNotBlank() }.joinToString(" · "), color = color,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                profile.mutedTime?.takeIf { profile.isMuted }?.let {
                    Text(it, color = Color(0xFFFF4500), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(title: String, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.width(72.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value?.takeIf { it.isNotBlank() } ?: "—", style = MaterialTheme.typography.bodyMedium)
    }
}
