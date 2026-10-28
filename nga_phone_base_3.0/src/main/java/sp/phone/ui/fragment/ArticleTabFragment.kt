package sp.phone.ui.fragment

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.BundleCompat
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.Lifecycle
import gov.anzong.androidnga.R
import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.activity.WebViewActivity
import gov.anzong.androidnga.ui.compose.*
import sp.phone.common.UserManagerImpl
import sp.phone.mvp.viewmodel.ArticleShareViewModel
import sp.phone.param.ArticleListParam
import sp.phone.param.ParamKey
import sp.phone.task.BookmarkTask
import sp.phone.theme.ThemeManager
import sp.phone.util.FunctionUtils

class ArticleTabFragment : BaseEventFragment() {
    private lateinit var request: ArticleListParam
    private val state = ArticleReaderState()
    private val share get() = getActivityViewModelProvider()[ArticleShareViewModel::class.java]
    private val post = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) refresh()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        request = BundleCompat.getParcelable(requireArguments(), ParamKey.KEY_PARAM, ArticleListParam::class.java)!!
        state.title = request.title ?: "帖子详情"
        state.pages = request.page.coerceAtLeast(1)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.compose_article_reader
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NgaTheme {
                    ArticleReaderScreen(state, request.page.coerceAtLeast(1), mConfig.isLeftHandMode,
                        onBack = { requireActivity().finish() }, onRefresh = ::refresh, onReply = ::reply,
                        onAction = ::handleAction, onPage = { request.page = it }) { page, active ->
                        val args = remember(page) { Bundle().apply {
                            putParcelable(ParamKey.KEY_PARAM, (request.clone() as ArticleListParam).also { it.page = page + 1 })
                        } }
                        AndroidFragment<ArticleListFragment>(arguments = args, maxLifecycle = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED) { fragment ->
                            state.floor?.takeIf { it / 20 == page }?.let { floor -> fragment.scrollToFloor(floor); state.floor = null }
                        }
                    }
                }
            }
        }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        share.readerControls.observe(viewLifecycleOwner) { controls ->
            if (controls.page == state.currentPage + 1) state.controlsExpanded = controls.expanded
        }
        share.replyCount.observe(viewLifecycleOwner) { count -> state.rows = count; state.pages = articlePageCount(count); updateActions() }
        share.topicId.observe(viewLifecycleOwner) { request.tid = it }
        share.title.observe(viewLifecycleOwner) { state.title = it }
        updateActions()
    }
    private fun refresh() { share.setRefreshPage(state.currentPage + 1) }
    private fun reply() {
        val intent = Intent(requireContext(), if (UserManagerImpl.getInstance().activeUser != null) mConfig.postActivityClass else mConfig.loginActivityClass)
            .putExtra("prefix", "").putExtra("tid", request.tid.toString()).putExtra("action", "reply")
        post.launch(intent)
    }
    private fun currentUrl() = Utils.getNGAHost() + "read.php?" + if (request.pid != 0) "pid=${request.pid}" else "tid=${request.tid}"
    private fun updateActions() {
        val menu = PopupMenu(requireContext(), View(requireContext())).menu
        requireActivity().menuInflater.inflate(R.menu.article_list_option_menu, menu)
        menu.findItem(R.id.menu_goto_floor).isVisible = state.rows > 0
        val theme = ThemeManager.getInstance()
        menu.findItem(R.id.menu_nightmode).isVisible = !theme.isNightModeFollowSystem && !theme.isNightMode
        menu.findItem(R.id.menu_daymode).isVisible = !theme.isNightModeFollowSystem && theme.isNightMode
        menu.findItem(R.id.menu_download).isVisible = request.pid == 0 && request.topicInfo != null
        state.actions = (0 until menu.size()).map { menu.getItem(it) }.filter { it.isVisible }.map { HomeAction(it.itemId, it.title.toString()) }
    }
    private fun handleAction(id: Int) {
        when (id) {
            R.id.menu_add_bookmark -> BookmarkTask.execute(request.tid)
            R.id.menu_goto_floor -> state.showJump = true
            R.id.menu_share -> FunctionUtils.share(requireContext(), getString(R.string.share), "${state.title}\n${currentUrl()}")
            R.id.menu_copy_url -> {
                (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("帖子链接", currentUrl()))
                showToast("链接已复制。")
            }
            R.id.menu_nightmode -> { ThemeManager.getInstance().setNightMode(true); requireActivity().recreate() }
            R.id.menu_daymode -> { ThemeManager.getInstance().setNightMode(false); requireActivity().recreate() }
            R.id.menu_download -> share.setCachePage(state.currentPage + 1)
            R.id.menu_open_by_browser -> startActivity(Intent(requireContext(), WebViewActivity::class.java).putExtra("url", currentUrl()).putExtra("title", state.title))
        }
    }
}
