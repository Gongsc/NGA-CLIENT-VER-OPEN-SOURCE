package sp.phone.ui.fragment

import gov.anzong.androidnga.base.util.ToastUtils
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import gov.anzong.androidnga.R
import gov.anzong.androidnga.activity.ArticleListActivity
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.ui.compose.*
import sp.phone.common.PhoneConfiguration
import sp.phone.common.UserManagerImpl
import sp.phone.http.bean.ThreadData
import sp.phone.http.bean.ThreadRowInfo
import gov.anzong.androidnga.mvvm.model.quoteBlock
import gov.anzong.androidnga.mvvm.viewmodel.ArticleListViewModel
import sp.phone.mvp.viewmodel.ArticleShareViewModel
import sp.phone.param.ArticleListParam
import sp.phone.param.ParamKey
import gov.anzong.androidnga.coroutines.UiEvent
import sp.phone.task.BookmarkTask
import sp.phone.task.LikeTask
import sp.phone.ui.fragment.dialog.AvatarDialogFragment
import sp.phone.ui.fragment.dialog.BaseDialogFragment
import sp.phone.ui.fragment.dialog.PostCommentDialogFragment
import sp.phone.util.FunctionUtils
import sp.phone.util.StringUtils

/** 主题内容的一页：Compose 绘制，加载与缓存由 [ArticleListViewModel] 负责。 */
open class ArticleListFragment : BaseEventFragment() {
    lateinit var viewModel: ArticleListViewModel
        private set
    protected val request: ArticleListParam get() = viewModel.request
    private val state = ArticlePageState()
    private val post = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) loadPage()
    }
    private val share get() = getActivityViewModelProvider()[ArticleShareViewModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[ArticleListViewModel::class.java]
        registerUiEvents()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.compose_article_page
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NgaTheme {
                    if (request.searchPost != 0) {
                        Scaffold(topBar = {
                            TopAppBar(title = { Text(request.title ?: "回复详情") }, navigationIcon = {
                                IconButton(onClick = { requireActivity().finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                            }, actions = { TextButton(onClick = {
                                startActivity(Intent(requireContext(), ArticleListActivity::class.java).putExtra(ParamKey.KEY_TID, request.tid))
                            }) { Text("完整主题") } })
                        }) { padding ->
                            Box(Modifier.padding(padding)) { PageContent() }
                        }
                    } else PageContent()
                }
            }
        }

    @androidx.compose.runtime.Composable
    private fun PageContent() {
        ArticlePageScreen(state, ::loadPage, onQuote = ::quote,
            onProfile = { row ->
                if (!row.getISANONYMOUS()) AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_PROFILE)
                    .withString("mode", "uid").withString("uid", row.authorid.toString()).navigation(requireContext())
            }, onAvatar = { row ->
                if (!row.getISANONYMOUS()) BaseDialogFragment.show(parentFragmentManager, Bundle().apply {
                    putString("name", row.author)
                    putString("url", FunctionUtils.parseAvatarUrl(row.js_escap_avatar))
                }, AvatarDialogFragment::class.java)
            }, actions = ::rowActions, onAction = ::handleRowAction,
            bottomContentPadding = if (request.searchPost == 0) readerActionsBottomPadding() else 0.dp,
            onControlsExpandedChange = { expanded ->
                if (request.searchPost == 0) share.setReaderControls(request.page, expanded)
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        parentFragmentManager.setFragmentResultListener("article_comment_posted_${request.page}", viewLifecycleOwner) { _, result ->
            if (result.getInt("tid") == request.tid) loadPage()
        }
        share.refreshPage.observe(viewLifecycleOwner) { if (it == request.page) loadPage() }
        share.cachePage.observe(viewLifecycleOwner) { if (it == request.page) viewModel.saveCache() }
        share.topicOwner.observe(viewLifecycleOwner) { state.owner = it }
        viewModel.loading.observe(viewLifecycleOwner) { state.loading = it }
        viewModel.error.observe(viewLifecycleOwner) { state.error = it }
        // 页面重建时 LiveData 重发已加载的内容，无需重新请求
        viewModel.data.observe(viewLifecycleOwner) { data -> if (data != null) setData(data) }
        super.onViewCreated(view, savedInstanceState)
        viewModel.start()
    }

    fun loadPage() = viewModel.load()

    fun setData(data: ThreadData?) {
        state.rows = data?.rowList.orEmpty().toList()
        state.hotReplies = data?.hotReplyList.orEmpty().toList()
        state.error = null
        if (data != null) {
            val tid = data.threadInfo?.tid?.takeIf { it > 0 } ?: state.rows.firstOrNull()?.tid
            if (tid != null && tid > 0) { request.tid = tid; share.setTopicId(tid) }
            share.setReplyCount(data.get__ROWS())
            if (request.title.isNullOrEmpty()) data.threadInfo?.subject?.let { title ->
                request.title = title
                requireActivity().title = title
                share.setTitle(title)
            }
            state.rows.firstOrNull { it.lou == 0 }?.let { share.setTopicOwner(it.author) }
        }
    }
    fun hideLoadingView() { state.loading = false }
    private fun startPostActivity(intent: Intent) {
        intent.setClass(requireContext(), if (UserManagerImpl.getInstance().activeUser != null) PhoneConfiguration.getInstance().postActivityClass else PhoneConfiguration.getInstance().loginActivityClass)
        post.launch(intent)
    }
    /** 引用回复：主楼与有 pid 的楼层带上引用内容并提及作者。 */
    private fun quote(row: ThreadRowInfo) {
        val tid = if (request.tid != 0) request.tid else row.tid
        val quoted = row.pid != 0 || row.lou == 0
        val intent = Intent().putExtra("prefix", if (quoted) StringUtils.removeBrTag(quoteBlock(request, row)) else "")
            .putExtra("tid", tid.toString()).putExtra("action", "reply")
        if (quoted && !row.author.isNullOrEmpty()) intent.putExtra("mention", row.author)
        startPostActivity(intent)
    }

    /** 贴条：回复楼层（有 pid）时引用该楼。 */
    private fun showPostCommentDialog(row: ThreadRowInfo) {
        val prefix = if (row.pid != 0) StringUtils.removeBrTag(quoteBlock(request, row)) + "\n" else ""
        val arguments = Bundle().apply {
            putInt("pid", row.pid)
            putInt("fid", row.fid)
            putInt("tid", if (request.tid != 0) request.tid else row.tid)
            putString("prefix", prefix)
            putInt("page", request.page)
        }
        PostCommentDialogFragment().apply { this.arguments = arguments }.show(parentFragmentManager)
    }
    override fun onUiEvent(event: UiEvent) {
        if (event.what == UiEvent.EVENT_ARTICLE_GO_FLOOR && event.arg + 1 == request.page && event.obj != null) {
            state.targetFloor = (request.page - 1) * 20 + (event.obj as Int)
            state.scrollRequest++
        } else super.onUiEvent(event)
    }
    fun scrollToFloor(floor: Int) { state.targetFloor = floor; state.scrollRequest++ }

    private fun rowActions(row: ThreadRowInfo): List<HomeAction> {
        val menu = PopupMenu(requireContext(), View(requireContext())).menu
        requireActivity().menuInflater.inflate(if (request.pid == 0) R.menu.article_list_context_menu else R.menu.article_list_context_menu_with_tid, menu)
        menu.findItem(R.id.menu_vote)?.isVisible = !row.vote.isNullOrBlank()
        menu.findItem(R.id.menu_edit)?.isVisible = UserManagerImpl.getInstance().activeUser?.userId == row.authorid.toString()
        menu.findItem(R.id.menu_ban_this_one)?.setTitle(if (row.get_isInBlackList()) R.string.cancel_ban_thisone else R.string.ban_thisone)
        return (0 until menu.size()).map { menu.getItem(it) }.filter { it.isVisible }.map { HomeAction(it.itemId, it.title.toString()) }
    }
    private fun handleRowAction(row: ThreadRowInfo, id: Int) {
        when (id) {
            R.id.menu_edit -> {
                if (FunctionUtils.isComment(row)) ToastUtils.info(R.string.cannot_eidt_comment)
                else startPostActivity(Intent().putExtra(ParamKey.KEY_PID, row.pid.toString()).putExtra(ParamKey.KEY_TID, row.tid.toString())
                    .putExtra("title", StringUtils.unEscapeHtml(row.subject.orEmpty())).putExtra("action", "modify")
                    .putExtra("prefix", StringUtils.unEscapeHtml(StringUtils.removeBrTag(row.content.orEmpty()))))
            }
            R.id.menu_post_comment -> showPostCommentDialog(row)
            R.id.menu_report -> FunctionUtils.handleReport(row, request.tid, parentFragmentManager)
            R.id.menu_signature -> if (!row.getISANONYMOUS()) FunctionUtils.Create_Signature_Dialog(row, requireContext(), requireView())
            R.id.menu_vote -> FunctionUtils.createVoteDialog(row, requireContext(), requireView(), mToast)
            R.id.menu_ban_this_one -> { showToast(viewModel.toggleBlacklist(row)); loadPage() }
            R.id.menu_show_this_person_only -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_CONTENT)
                .withInt(ParamKey.KEY_TID, row.tid).withInt(ParamKey.KEY_AUTHOR_ID, row.authorid).navigation(requireContext())
            R.id.menu_support -> LikeTask().execute(row.tid, row.pid, LikeTask.SUPPORT, ToastUtils::success)
            R.id.menu_oppose -> LikeTask().execute(row.tid, row.pid, LikeTask.OPPOSE, ToastUtils::success)
            R.id.menu_favorite -> BookmarkTask.execute(row.tid.toString(), row.pid.toString())
        }
    }
}
