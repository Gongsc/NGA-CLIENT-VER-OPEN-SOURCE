package gov.anzong.androidnga.ui.compose

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView

/**
 * 网页浏览外壳：标题栏、加载进度与刷新，网页内容为调用方持有的 [webView]。
 * [menu] 为更多操作菜单中的项目（文字到动作），为空时不显示菜单按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPageScreen(title: String, progress: Int, webView: WebView?, onRefresh: () -> Unit, onBack: () -> Unit,
                  menu: List<Pair<String, () -> Unit>> = emptyList()) {
    var expanded by remember { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        Column {
            TopAppBar(title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack) },
                actions = {
                    ActionIcon(Icons.Default.Refresh, "刷新", onRefresh)
                    if (menu.isNotEmpty()) Box {
                        ActionIcon(Icons.Default.MoreVert, "更多操作", { expanded = true })
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            menu.forEach { (label, action) -> DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; action() }) }
                        }
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
            if (progress in 0..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().testTag("web_progress"))
        }
    }) { padding ->
        if (webView != null) AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize().padding(padding).testTag("web_page"))
    }
}
