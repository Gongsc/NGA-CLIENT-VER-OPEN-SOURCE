package sp.phone.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import gov.anzong.androidnga.ui.compose.*
import sp.phone.view.webview.LoginWebView
import sp.phone.common.UserManagerImpl

class LoginWebFragment : BaseFragment() {
    private var web: WebView? = null
    private var restoredWebState: Bundle? = null
    private val loginUrl = "https://ngabbs.com/nuke.php?__lib=login&__act=account&login"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoredWebState = savedInstanceState?.getBundle("web_state")
    }

    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = gov.anzong.androidnga.R.id.compose_login
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NgaTheme {
                    var progress by remember { mutableIntStateOf(0) }
                    var error by remember { mutableStateOf<String?>(null) }
                    BackHandler { if (!onBackPressed()) requireActivity().finish() }
                    Scaffold(topBar = {
                        TopAppBar(title = { Text("账号登录") }, navigationIcon = {
                            IconButton(onClick = { if (!onBackPressed()) requireActivity().finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                        }, actions = {
                            TextButton(onClick = {
                                val session = web?.url?.takeIf(::isNgaLoginUrl)?.let { parseLoginSession(CookieManager.getInstance().getCookie(it)) }
                                if (session == null) error = "尚未检测到完整登录信息，请完成网页登录后重试。"
                                else {
                                    CookieManager.getInstance().flush()
                                    UserManagerImpl.getInstance().addUser(session.uid, session.cid, session.userName)
                                    requireActivity().setResult(Activity.RESULT_OK)
                                    requireActivity().finish()
                                }
                            }) { Text("完成登录") }
                        })
                    }) { padding ->
                        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
                            Text("请通过 NGA 官方网页完成登录。当前不支持 QQ 和微博登录。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                            if (progress in 1..99) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                            error?.let { message ->
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                    Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                                    ActionIcon(ActionIcons.Refresh, "重试", { error = null; web?.reload() })
                                }
                            }
                            AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context ->
                                LoginWebView(context).apply {
                                    web = this
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress }
                                    }
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                            // 登录与 Cookie 同步均使用嵌套页面；子框架不应启动外部应用。
                                            if (!request.isForMainFrame) return false
                                            val target = request.url.toString()
                                            if (isNgaLoginUrl(target)) return false
                                            if (request.url.scheme == "https") runCatching { startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                                            return true
                                        }
                                        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                            error = null
                                        }
                                        override fun onPageFinished(view: WebView, url: String?) {
                                            (view as LoginWebView).fitLoginFrame()
                                        }
                                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                            if (isLoginDocument(request)) error = "登录服务暂时不可用（HTTP ${response.statusCode}），请稍后重试。"
                                        }
                                        private fun isLoginDocument(request: WebResourceRequest) =
                                            request.isForMainFrame || (isNgaLoginUrl(request.url.toString()) && request.url.path == "/nuke/account_copy.html")

                                        override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                                            if (isLoginDocument(request)) error = "登录页面加载失败（${failure.errorCode}），请检查网络后重试。"
                                        }
                                    }
                                    if (restoredWebState?.let { restoreState(it) } == null) loadUrl(loginUrl)
                                    restoredWebState = null
                                }
                            }, onRelease = { view ->
                                view.stopLoading(); view.webChromeClient = null; view.destroy()
                                if (web === view) web = null
                            })
                        }
                    }
                }
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        web?.let { view -> Bundle().also { view.saveState(it); outState.putBundle("web_state", it) } }
        super.onSaveInstanceState(outState)
    }
    override fun onBackPressed(): Boolean = web?.let { if (it.canGoBack()) { it.goBack(); true } else false } ?: false
    override fun onResume() { super.onResume(); web?.onResume() }
    override fun onPause() { web?.onPause(); super.onPause() }
}
