package gov.anzong.androidnga.ui.fragment

import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.LaunchedEffect
import sp.phone.mvp.viewmodel.ForumFavoritesViewModel
import sp.phone.common.UserManagerImpl
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.R
import gov.anzong.androidnga.activity.SubBoardActivity
import gov.anzong.androidnga.activity.TopicListActivity
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.ui.compose.*
import sp.phone.mvp.model.entity.Board
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.mvp.model.entity.TopicListInfo
import gov.anzong.androidnga.mvvm.viewmodel.TopicListViewModel
import sp.phone.param.ParamKey
import sp.phone.param.TopicListParam
import sp.phone.param.TopicTitleHelper
import sp.phone.ui.fragment.BaseFragment
import gov.anzong.androidnga.arouter.TopicNavigation
import sp.phone.util.ActivityUtils

/** 主题列表：Compose 绘制，请求由 [TopicListViewModel] 负责，已加载的条目保存在 [FeedStore]。 */
class ComposeTopicListFragment : BaseFragment() {
    class FeedStore : ViewModel() {
        val ui = TopicFeedState()
        var entries = emptyList<ThreadPageInfo>()
        var info: TopicListInfo? = null
        var page = 0
        var lastFirst: TopicListInfo? = null
        var lastNext: TopicListInfo? = null
    }
    private lateinit var store: FeedStore
    private lateinit var viewModel: TopicListViewModel
    private lateinit var favorites: ForumFavoritesViewModel
    private lateinit var param: TopicListParam

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = ViewModelProvider(this)
        store = provider[FeedStore::class.java]
        viewModel = provider[TopicListViewModel::class.java]
        favorites = provider[ForumFavoritesViewModel::class.java]
        param = viewModel.param
        store.ui.favorite = param.favor != 0
        store.ui.canPost = param.fid != 0 && param.favor == 0 && param.key.isNullOrBlank() && param.authorId == 0 && param.searchPost == 0
        store.ui.title = when {
            param.favor != 0 -> "收藏的主题"
            !param.key.isNullOrBlank() -> "搜索：${param.key}"
            !param.author.isNullOrBlank() -> "${param.author}的${if (param.searchPost > 0) "回复" else "主题"}"
            param.recommend == 1 -> "${param.title.orEmpty()} - 精华区"
            param.twentyfour == 1 -> "${param.title.orEmpty()} - 24小时热帖"
            else -> param.title ?: "主题列表"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            id = R.id.compose_topic_feed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NgaTheme {
                    LaunchedEffect(favorites.snapshot) { updateActions() }
                    TopicFeedScreen(store.ui, onBack = { requireActivity().finish() }, onRefresh = ::refresh,
                        onNext = ::nextPage, onTopic = { index ->
                            store.entries.getOrNull(index)?.let { TopicNavigation.open(requireContext(), it, param) }
                        }, onDelete = { index -> store.entries.getOrNull(index)?.let(viewModel::removeFavorite) },
                        onPost = {
                            AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_POST)
                                .withInt(ParamKey.KEY_FID, param.fid)
                                .withString(ParamKey.KEY_STID, if (param.stid != 0) param.stid.toString() else null)
                                .withString(ParamKey.KEY_ACTION, "new").navigation(requireContext())
                        }, onAction = ::handleAction)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 页面重建时 LiveData 会重发最后一次结果，已处理过的结果按对象判断跳过
        viewModel.firstPage.observe(viewLifecycleOwner) { result ->
            if (result != null && result !== store.lastFirst) {
                store.lastFirst = result
                store.entries = emptyList()
                store.page = 0
                accept(result)
                store.ui.refreshGeneration++
            }
        }
        viewModel.nextPage.observe(viewLifecycleOwner) { result ->
            if (result != null && result !== store.lastNext) {
                store.lastNext = result
                accept(result)
            }
        }
        viewModel.refreshing.observe(viewLifecycleOwner) { store.ui.refreshing = it == true }
        viewModel.error.observe(viewLifecycleOwner) { error -> if (error != null) { store.ui.error = error; viewModel.errorShown() } }
        viewModel.removedTopic.observe(viewLifecycleOwner) { removed ->
            if (removed != null) { store.entries = store.entries.filterNot { it == removed }; publishRows() }
        }
        updateActions()
        viewModel.start()
    }

    private fun accept(result: TopicListInfo) {
        store.info = result
        val incoming = result.threadPageList.orEmpty().filter { param.searchPost == 0 || it.replyInfo != null }
        val merged = (store.entries + incoming).distinctBy { "${it.tid}:${it.pid}" }
        store.ui.hasMore = incoming.isNotEmpty() && (store.page == 0 || merged.size > store.entries.size)
        store.entries = merged
        store.page++
        store.ui.error = null
        if (param.title.isNullOrBlank() && !result.name.isNullOrBlank()) {
            param.title = result.name
            store.ui.title = result.name
        }
        publishRows()
        updateActions()
    }

    private fun publishRows() {
        store.ui.rows = store.entries.mapIndexed { index, entry ->
            entry.position = index
            TopicRow("${entry.tid}:${entry.pid}", TopicTitleHelper.handleTitleFormat(entry).toComposeText(),
                entry.author.orEmpty(), entry.lastPoster.orEmpty(), entry.replies, entry.replyInfo?.content)
        }
    }

    private fun refresh() {
        if (store.ui.refreshing) return
        store.ui.error = null
        viewModel.refresh()
    }

    private fun nextPage() {
        if (store.ui.refreshing || !store.ui.hasMore) return
        store.ui.error = null
        viewModel.loadNextPage(store.page + 1)
    }

    override fun onResume() {
        super.onResume()
        favorites.refresh()
    }

    private fun updateActions() {
        val menu = PopupMenu(requireContext(), View(requireContext())).menu
        requireActivity().menuInflater.inflate(R.menu.topic_list_menu, menu)
        val bookmarked = viewModel.isBookmarkBoard
        menu.findItem(R.id.menu_add_bookmark).isVisible = !bookmarked && (param.fid != 0 || param.stid != 0)
        menu.findItem(R.id.menu_remove_bookmark).isVisible = bookmarked
        val available = !favorites.snapshot.loading && !favorites.snapshot.mutating
        menu.findItem(R.id.menu_add_bookmark).isEnabled = available
        menu.findItem(R.id.menu_remove_bookmark).isEnabled = available
        menu.findItem(R.id.menu_board_head).isVisible = param.boardHead != null
        menu.findItem(R.id.menu_sub_board).isVisible = !store.info?.subBoardList.isNullOrEmpty()
        store.ui.actions = (0 until menu.size()).map { menu.getItem(it) }.filter { it.isVisible }
            .map { HomeAction(it.itemId, it.title.toString(), it.isEnabled) }
    }

    private fun handleAction(id: Int) {
        if ((id == R.id.menu_add_bookmark || id == R.id.menu_remove_bookmark) && !UserManagerImpl.getInstance().hasValidUser()) {
            AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_LOGIN).navigation(requireContext())
            return
        }
        when (id) {
            R.id.menu_add_bookmark -> {
                viewModel.addBookmarkBoard(Board(param.fid, param.stid, param.title ?: store.ui.title).also { it.boardHead = param.boardHead })
                updateActions()
            }
            R.id.menu_remove_bookmark -> { viewModel.removeBookmarkBoard(); updateActions() }
            R.id.menu_history -> ActivityUtils.startHistoryTopicActivity(requireContext())
            R.id.menu_board_head -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_CONTENT)
                .withInt(ParamKey.KEY_TID, param.boardHead.toInt())
                .withString(ParamKey.KEY_TITLE, "${param.title} - 版头")
                .navigation(requireContext())
            R.id.menu_sub_board -> subBoardLauncher.launch(Intent(requireContext(), SubBoardActivity::class.java).apply {
                putExtra(ParamKey.KEY_TITLE, param.title)
                putExtra(ParamKey.KEY_FID, param.fid)
                putParcelableArrayListExtra("subBoard", store.info?.subBoardList)
            })
            else -> {
                val menu = PopupMenu(requireContext(), View(requireContext())).menu
                requireActivity().menuInflater.inflate(R.menu.topic_list_menu, menu)
                (requireActivity() as TopicListActivity).onOptionsItemSelected(menu.findItem(id))
            }
        }
    }

    /** 子版块页订阅状态变化时返回 RESULT_OK，刷新列表。 */
    private val subBoardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) refresh()
    }
}
