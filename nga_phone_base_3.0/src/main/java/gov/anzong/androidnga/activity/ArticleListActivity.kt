package gov.anzong.androidnga.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.os.BundleCompat
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.Lifecycle
import gov.anzong.androidnga.R
import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.ui.compose.*
import sp.phone.common.PhoneConfiguration
import sp.phone.common.UserManagerImpl
import sp.phone.mvp.viewmodel.ArticleShareViewModel
import sp.phone.param.ArticleListParam
import sp.phone.param.ParamKey
import sp.phone.task.BookmarkTask
import sp.phone.theme.ThemeManager
import sp.phone.ui.fragment.ArticleListFragment
import sp.phone.util.FunctionUtils

/**
 * 帖子详情：参数为 extras 中的 [ParamKey.KEY_PARAM]、单独的 extras 字段或 read.php 形式的链接。
 * 普通主题为分页阅读器，每页是一个 [ArticleListFragment]；搜索回复（searchPost）只显示单页。
 */
class ArticleListActivity : BaseActivity() {
    private lateinit var request: ArticleListParam
    private val state = ArticleReaderState()
    private val share: ArticleShareViewModel by viewModels()
    private val post = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        request = requestParam() ?: run { finish(); return }
        request.title?.let(::setTitle)
        val args = Bundle().apply { putParcelable(ParamKey.KEY_PARAM, request) }
        if (request.searchPost != 0) {
            setContent { NgaTheme { AndroidFragment<ArticleListFragment>(Modifier.fillMaxSize(), arguments = args) } }
            return
        }
        state.title = request.title ?: "帖子详情"
        state.pages = request.page.coerceAtLeast(1)
        setContent {
            NgaTheme {
                ArticleReaderScreen(state, request.page.coerceAtLeast(1), PhoneConfiguration.getInstance().isLeftHandMode,
                    onBack = ::finish, onRefresh = ::refresh, onReply = ::reply,
                    onAction = ::handleAction, onPage = { request.page = it }) { page, active ->
                    val pageArgs = remember(page) { Bundle().apply {
                        putParcelable(ParamKey.KEY_PARAM, (request.clone() as ArticleListParam).also { it.page = page + 1 })
                    } }
                    AndroidFragment<ArticleListFragment>(arguments = pageArgs, maxLifecycle = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED) { fragment ->
                        state.floor?.takeIf { it / 20 == page }?.let { floor -> fragment.scrollToFloor(floor); state.floor = null }
                    }
                }
            }
        }
        share.readerControls.observe(this) { controls ->
            if (controls.page == state.currentPage + 1) state.controlsExpanded = controls.expanded
        }
        share.replyCount.observe(this) { count -> state.rows = count; state.pages = articlePageCount(count); updateActions() }
        share.topicId.observe(this) { request.tid = it }
        share.title.observe(this) { state.title = it }
        updateActions()
    }

    private fun requestParam(): ArticleListParam? {
        val url = intent.dataString
        val extras = intent.extras
        return when {
            url != null -> Uri.parse(url).let { uri ->
                ArticleListParam().apply {
                    tid = queryInt(uri, "tid")
                    pid = queryInt(uri, "pid")
                    authorId = queryInt(uri, "authorid")
                    page = queryInt(uri, "page")
                    searchPost = queryInt(uri, ParamKey.KEY_SEARCH_POST)
                }
            }
            extras != null -> BundleCompat.getParcelable(extras, ParamKey.KEY_PARAM, ArticleListParam::class.java) ?: ArticleListParam().apply {
                tid = extras.getInt(ParamKey.KEY_TID, 0)
                pid = extras.getInt(ParamKey.KEY_PID, 0)
                authorId = extras.getInt(ParamKey.KEY_AUTHOR_ID, 0)
                searchPost = extras.getInt(ParamKey.KEY_SEARCH_POST, 0)
                title = extras.getString(ParamKey.KEY_TITLE)
            }
            else -> null
        }
    }

    private fun queryInt(uri: Uri, name: String) = uri.getQueryParameter(name)?.toIntOrNull() ?: 0

    private fun refresh() { share.setRefreshPage(state.currentPage + 1) }

    private fun reply() {
        val config = PhoneConfiguration.getInstance()
        val intent = Intent(this, if (UserManagerImpl.getInstance().activeUser != null) config.postActivityClass else config.loginActivityClass)
            .putExtra("prefix", "").putExtra("tid", request.tid.toString()).putExtra("action", "reply")
        post.launch(intent)
    }

    private fun currentUrl() = Utils.getNGAHost() + "read.php?" + if (request.pid != 0) "pid=${request.pid}" else "tid=${request.tid}"

    private fun updateActions() {
        val menu = PopupMenu(this, View(this)).menu
        menuInflater.inflate(R.menu.article_list_option_menu, menu)
        menu.findItem(R.id.menu_goto_floor).isVisible = state.rows > 0
        val theme = ThemeManager.getInstance()
        menu.findItem(R.id.menu_nightmode).isVisible = !theme.isNightModeFollowSystem && !theme.isNightMode
        menu.findItem(R.id.menu_daymode).isVisible = !theme.isNightModeFollowSystem && theme.isNightMode
        menu.findItem(R.id.menu_download).isVisible = request.pid == 0 && request.topicInfo != null
        state.actions = (0 until menu.size()).map { menu.getItem(it) }.filter { it.isVisible }.map { HomeAction(it.itemId, it.title.toString()) }
    }

    private fun handleAction(id: Int) {
        when (id) {
            R.id.menu_add_bookmark -> BookmarkTask().execute(request.tid.toString())
            R.id.menu_goto_floor -> state.showJump = true
            R.id.menu_share -> FunctionUtils.share(this, getString(R.string.share), "${state.title}\n${currentUrl()}")
            R.id.menu_copy_url -> {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("帖子链接", currentUrl()))
                ToastUtils.info("链接已复制。")
            }
            R.id.menu_nightmode -> { ThemeManager.getInstance().setNightMode(true); recreate() }
            R.id.menu_daymode -> { ThemeManager.getInstance().setNightMode(false); recreate() }
            R.id.menu_download -> share.setCachePage(state.currentPage + 1)
            R.id.menu_open_by_browser -> startActivity(Intent(this, WebViewActivity::class.java).putExtra("url", currentUrl()).putExtra("title", state.title))
        }
    }

    override fun usesComposeContent() = true
}
