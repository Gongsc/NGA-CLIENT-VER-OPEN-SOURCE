package sp.phone.ui.fragment

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
import androidx.core.os.BundleCompat
import gov.anzong.androidnga.R
import gov.anzong.androidnga.activity.ArticleListActivity
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.ui.compose.*
import sp.phone.common.PhoneConfiguration
import sp.phone.common.UserManagerImpl
import sp.phone.http.bean.ThreadData
import sp.phone.http.bean.ThreadRowInfo
import sp.phone.mvp.contract.ArticleListContract
import sp.phone.mvp.presenter.ArticleListPresenter
import sp.phone.mvp.viewmodel.ArticleShareViewModel
import sp.phone.param.ArticleListParam
import sp.phone.param.ParamKey
import sp.phone.rxjava.RxEvent
import sp.phone.task.BookmarkTask
import sp.phone.ui.fragment.dialog.AvatarDialogFragment
import sp.phone.ui.fragment.dialog.BaseDialogFragment
import sp.phone.ui.fragment.dialog.PostCommentDialogFragment
import sp.phone.util.FunctionUtils
import sp.phone.util.StringUtils

open class ArticleListFragment : BaseMvpFragment<ArticleListPresenter>(), ArticleListContract.View {
    protected lateinit var request: ArticleListParam
    private val state = ArticlePageState()
    private val post = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) loadPage()
    }
    private val share get() = getActivityViewModelProvider()[ArticleShareViewModel::class.java]

    override fun onCreate(savedInstanceState: Bundle?) {
        request = BundleCompat.getParcelable(requireArguments(), ParamKey.KEY_PARAM, ArticleListParam::class.java)!!
        request.page = request.page.coerceAtLeast(1)
        super.onCreate(savedInstanceState)
        registerRxBus()
    }
    override fun onCreatePresenter() = ArticleListPresenter(request)

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
        ArticlePageScreen(state, ::loadPage, onQuote = { mPresenter.quote(request, it) },
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
        share.cachePage.observe(viewLifecycleOwner) { if (it == request.page) mPresenter.cachePage() }
        share.topicOwner.observe(viewLifecycleOwner) { state.owner = it }
        super.onViewCreated(view, savedInstanceState)
    }

    fun loadPage() {
        state.error = null
        if (!state.loading) {
            if (request.loadCache) mPresenter.loadCachePage() else mPresenter.loadPage(request)
        }
    }
    override fun setData(data: ThreadData?) {
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
    fun setPageError(message: String?) { state.error = message }
    override fun setRefreshing(refreshing: Boolean) { state.loading = refreshing }
    override fun isRefreshing() = state.loading
    override fun hideLoadingView() { state.loading = false }
    override fun startPostActivity(intent: Intent) {
        intent.setClass(requireContext(), if (UserManagerImpl.getInstance().activeUser != null) PhoneConfiguration.getInstance().postActivityClass else PhoneConfiguration.getInstance().loginActivityClass)
        post.launch(intent)
    }
    override fun showPostCommentDialog(prefix: String?, bundle: Bundle) {
        bundle.putString("prefix", prefix)
        bundle.putInt("page", request.page)
        PostCommentDialogFragment().apply { arguments = bundle }.show(parentFragmentManager)
    }
    override fun accept(event: RxEvent) {
        if (event.what == RxEvent.EVENT_ARTICLE_GO_FLOOR && event.arg + 1 == request.page && event.obj != null) {
            state.targetFloor = (request.page - 1) * 20 + (event.obj as Int)
            state.scrollRequest++
        } else super.accept(event)
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
                if (FunctionUtils.isComment(row)) showToast(R.string.cannot_eidt_comment)
                else startPostActivity(Intent().putExtra(ParamKey.KEY_PID, row.pid.toString()).putExtra(ParamKey.KEY_TID, row.tid.toString())
                    .putExtra("title", StringUtils.unEscapeHtml(row.subject.orEmpty())).putExtra("action", "modify")
                    .putExtra("prefix", StringUtils.unEscapeHtml(StringUtils.removeBrTag(row.content.orEmpty()))))
            }
            R.id.menu_post_comment -> mPresenter.postComment(request, row)
            R.id.menu_report -> FunctionUtils.handleReport(row, request.tid, parentFragmentManager)
            R.id.menu_signature -> if (!row.getISANONYMOUS()) FunctionUtils.Create_Signature_Dialog(row, requireContext(), requireView())
            R.id.menu_vote -> FunctionUtils.createVoteDialog(row, requireContext(), requireView(), mToast)
            R.id.menu_ban_this_one -> { mPresenter.banThisSB(row); loadPage() }
            R.id.menu_show_this_person_only -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_CONTENT)
                .withInt(ParamKey.KEY_TID, row.tid).withInt(ParamKey.KEY_AUTHOR_ID, row.authorid).navigation(requireContext())
            R.id.menu_support -> mPresenter.postSupportTask(row.tid, row.pid)
            R.id.menu_oppose -> mPresenter.postOpposeTask(row.tid, row.pid)
            R.id.menu_favorite -> BookmarkTask.execute(row.tid.toString(), row.pid.toString())
        }
    }
}
