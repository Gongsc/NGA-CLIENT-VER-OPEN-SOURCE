package gov.anzong.androidnga.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.BuildConfig
import gov.anzong.androidnga.debug.Debugger
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.util.*
import kotlinx.coroutines.launch
import sp.phone.ui.fragment.dialog.VersionUpgradeDialogFragment
import sp.phone.util.FunctionUtils

class AboutActivity : BaseActivity() {
    override fun usesComposeContent() = true

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        setContent {
            NgaTheme {
                Scaffold(topBar = {
                    TopAppBar(title = { Text("关于") }, navigationIcon = {
                        IconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                    })
                }) { padding ->
                    AboutScreen(Modifier.padding(padding), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                        checkUpdate = { checkLatestRelease(BuildConfig.VERSION_NAME) },
                        onReleaseNotes = { VersionUpgradeDialogFragment().show(supportFragmentManager, null) },
                        onToggleDebug = { Debugger.toggleDebugMode() },
                        openUrl = ::openInBrowser,
                        openDocument = { startActivity(Intent(this, WebViewerActivity::class.java).putExtra("path", it)) })
                }
            }
        }
    }

    private fun openInBrowser(url: String) {
        try {
            FunctionUtils.openUrlByDefaultBrowser(this, url)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "未找到可打开链接的浏览器", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AboutScreen(
    modifier: Modifier, versionName: String, versionCode: Int,
    checkUpdate: suspend () -> UpdateResult, onReleaseNotes: () -> Unit, onToggleDebug: () -> Unit,
    openUrl: (String) -> Unit, openDocument: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateResult?>(null) }
    val runCheck: () -> Unit = {
        if (!checking) {
            checking = true
            scope.launch {
                result = checkUpdate()
                checking = false
            }
        }
    }
    LazyColumn(modifier.fillMaxSize().testTag("about_list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ElevatedCard {
                AboutItem("NGA 客户端", "版本 $versionName（$versionCode）· 查看更新内容", onReleaseNotes, onLongClick = onToggleDebug)
                AboutItem("检查更新", if (checking) "正在从 GitHub 获取最新版本…" else "从 GitHub 发布页获取最新版本", runCheck,
                    trailing = if (checking) ({ CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }) else null)
            }
        }
        item {
            ElevatedCard {
                AboutItem("项目主页", "github.com/Gongsc/NGA-CLIENT-VER-OPEN-SOURCE", { openUrl(PROJECT_URL) })
                AboutItem("问题反馈", "在 GitHub Issues 提交问题与建议", { openUrl(ISSUES_URL) })
                AboutItem("开源许可", "GNU GPL v2", { openDocument("file:///android_asset/LICENSE.TXT") })
                AboutItem("第三方许可", "依赖库的开源许可", { openDocument("file:///android_asset/OSLICENSE.TXT") })
            }
        }
        item {
            ElevatedCard {
                Text("致谢", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp))
                ListItem(headlineContent = { Text("原开源版开发团队") }, supportingContent = {
                    Text("代码：[@竹井詩織里]/[@cfan8]/[@jjimmys]/[@Moandor]/[@Elrond]/[@Justwen]\n美工：[@那个惩戒骑]/[@从来不卖萌]\n感谢 [@force0119]")
                })
            }
        }
    }
    result?.let { UpdateResultDialog(it, versionName, openUrl) { result = null } }
}

@Composable
private fun UpdateResultDialog(result: UpdateResult, versionName: String, openUrl: (String) -> Unit, onDismiss: () -> Unit) {
    when (result) {
        is UpdateResult.Available -> AlertDialog(onDismissRequest = onDismiss,
            title = { Text("发现新版本 ${result.release.version}") },
            text = {
                Text(result.release.notes.ifBlank { "当前版本 $versionName，可前往 GitHub 下载新版本。" }.take(1500),
                    Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()))
            },
            confirmButton = { TextButton(onClick = { onDismiss(); openUrl(result.release.url) }) { Text("前往下载") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("稍后") } })
        is UpdateResult.UpToDate -> AlertDialog(onDismissRequest = onDismiss,
            title = { Text(if (result.latest == null) "暂无发布版本" else "已是最新版本") },
            text = {
                Text(if (result.latest == null) "GitHub 上还没有发布版本，当前版本 $versionName。"
                    else "当前版本 $versionName，GitHub 最新发布为 ${result.latest}。")
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
            dismissButton = { TextButton(onClick = { onDismiss(); openUrl(RELEASES_URL) }) { Text("查看发布页") } })
        is UpdateResult.Failed -> AlertDialog(onDismissRequest = onDismiss,
            title = { Text("检查更新失败") },
            text = { Text(result.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
            dismissButton = { TextButton(onClick = { onDismiss(); openUrl(RELEASES_URL) }) { Text("查看发布页") } })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AboutItem(title: String, detail: String, click: () -> Unit, onLongClick: (() -> Unit)? = null,
                      trailing: (@Composable () -> Unit)? = null) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(detail) }, trailingContent = trailing,
        modifier = Modifier.combinedClickable(onClick = click, onLongClick = onLongClick))
}
