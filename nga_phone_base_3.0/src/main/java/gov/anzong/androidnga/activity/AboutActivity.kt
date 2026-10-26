package gov.anzong.androidnga.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.BuildConfig
import gov.anzong.androidnga.debug.Debugger
import gov.anzong.androidnga.ui.compose.NgaTheme
import sp.phone.ui.fragment.dialog.VersionUpgradeDialogFragment
import sp.phone.util.FunctionUtils

class AboutActivity : BaseActivity() {
    override fun usesComposeContent() = true

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        setContent {
            NgaTheme {
                Scaffold(topBar = {
                    TopAppBar(title = { Text("关于 NGA 客户端") }, navigationIcon = {
                        IconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                    })
                }) { padding ->
                    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            ElevatedCard {
                                AboutItem("NGA 玩家社区", "版本 ${BuildConfig.VERSION_NAME}") { VersionUpgradeDialogFragment().show(supportFragmentManager, null) }
                                AboutItem("开源许可", "GNU GPL v2") { openDocument("file:///android_asset/OSLICENSE.TXT") }
                                AboutItem("检测更新", "查看 GitHub 发布版本") { openDocument("https://github.com/Justwen/NGA-CLIENT-VER-OPEN-SOURCE/releases") }
                            }
                        }
                        item {
                            ElevatedCard {
                                Text("开发团队", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                                ListItem(headlineContent = { Text("代码") }, supportingContent = { Text("[@竹井詩織里]/[@cfan8]/[@jjimmys]\n[@Moandor]/[@Elrond]/[@Justwen]") },
                                    modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { Debugger.toggleDebugMode() }))
                                AboutItem("美工", "[@那个惩戒骑]/[@从来不卖萌]") {}
                                AboutItem("GitHub", "问题与建议") { FunctionUtils.openUrlByDefaultBrowser(this@AboutActivity, "https://github.com/Justwen/NGA-CLIENT-VER-OPEN-SOURCE/issues") }
                            }
                        }
                        item {
                            ElevatedCard {
                                Text("感谢 [@force0119]", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                                AboutItem("客户端交流 QQ 群", "1065310118 · 点击复制") { FunctionUtils.copyToClipboard(this@AboutActivity, "1065310118") }
                                AboutItem("问题反馈 QQ 群", "1077054628 · 点击复制") { FunctionUtils.copyToClipboard(this@AboutActivity, "1077054628") }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openDocument(path: String) {
        startActivity(Intent(this, WebViewerActivity::class.java).putExtra("path", path))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AboutItem(title: String, detail: String, click: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(detail) },
        modifier = Modifier.combinedClickable(onClick = click))
}
