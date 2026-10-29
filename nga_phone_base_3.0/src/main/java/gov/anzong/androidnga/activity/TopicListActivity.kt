package gov.anzong.androidnga.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.BundleCompat
import androidx.lifecycle.ViewModel
import gov.anzong.androidnga.R
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.arouter.TopicNavigation
import gov.anzong.androidnga.mvvm.viewmodel.TopicListViewModel
import gov.anzong.androidnga.ui.compose.*
import sp.phone.common.UserManagerImpl
import sp.phone.mvp.model.BoardModel
import sp.phone.mvp.model.entity.Board
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.mvp.model.entity.TopicListInfo
import sp.phone.mvp.viewmodel.ForumFavoritesViewModel
import sp.phone.param.ParamKey
import sp.phone.param.TopicListParam
import sp.phone.param.TopicTitleHelper
import sp.phone.util.ActivityUtils
import sp.phone.util.StringUtils

/**
 * 主题列表：版块、搜索、收藏、精华区与 24 小时热帖。参数为 extras 中的 [ParamKey.KEY_PARAM]、
 * 单独的 extras 字段或 thread.php 形式的链接。请求由 [TopicListViewModel] 负责，已加载的条目保存在 [FeedStore]。
 */
class TopicListActivity : BaseActivity() {

    class FeedStore : ViewModel() {
        val ui = TopicFeedState()
        var entries = emptyList<ThreadPageInfo>()
        var info: TopicListInfo? = null
        var page = 0
        var lastFirst: TopicListInfo? = null
        var lastNext: TopicListInfo? = null
    }

    private val store: FeedStore by viewModels()
    private val viewModel: TopicListViewModel by viewModels()
    private val favorites: ForumFavoritesViewModel by viewModels()
    private lateinit var param: TopicListParam

    /** 子版块页订阅状态变化时返回 RESULT_OK，刷新列表。 */
    private val subBoardLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val request = requestParam() ?: run { finish(); return }
        // TopicListViewModel 经 SavedStateHandle 从 extras 读取参数
        intent.putExtra(ParamKey.KEY_PARAM, request)
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
        setContent {
            NgaTheme {
                LaunchedEffect(favorites.snapshot) { updateActions() }
                TopicFeedScreen(store.ui, onBack = ::finish, onRefresh = ::refresh,
                    onNext = ::nextPage, onTopic = { index ->
                        store.entries.getOrNull(index)?.let { TopicNavigation.open(this, it, param) }
                    }, onDelete = { index -> store.entries.getOrNull(index)?.let(viewModel::removeFavorite) },
                    onPost = {
                        AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_POST)
                            .withInt(ParamKey.KEY_FID, param.fid)
                            .withString(ParamKey.KEY_STID, if (param.stid != 0) param.stid.toString() else null)
                            .withString(ParamKey.KEY_ACTION, "new").navigation(this)
                    }, onAction = ::handleAction)
            }
        }
        // 页面重建时 LiveData 会重发最后一次结果，已处理过的结果按对象判断跳过
        viewModel.firstPage.observe(this) { result ->
            if (result != null && result !== store.lastFirst) {
                store.lastFirst = result
                store.entries = emptyList()
                store.page = 0
                accept(result)
                store.ui.refreshGeneration++
            }
        }
        viewModel.nextPage.observe(this) { result ->
            if (result != null && result !== store.lastNext) {
                store.lastNext = result
                accept(result)
            }
        }
        viewModel.refreshing.observe(this) { store.ui.refreshing = it == true }
        viewModel.error.observe(this) { error -> if (error != null) { store.ui.error = error; viewModel.errorShown() } }
        viewModel.removedTopic.observe(this) { removed ->
            if (removed != null) { store.entries = store.entries.filterNot { it == removed }; publishRows() }
        }
        updateActions()
        viewModel.start()
    }

    override fun onResume() {
        super.onResume()
        if (::param.isInitialized) favorites.refresh()
    }

    private fun requestParam(): TopicListParam? {
        val url = intent.dataString
        val extras = intent.extras
        val request = when {
            url != null -> TopicListParam().apply {
                authorId = StringUtils.getUrlParameter(url, "authorid")
                searchPost = StringUtils.getUrlParameter(url, "searchpost")
                favor = StringUtils.getUrlParameter(url, "favor")
                key = StringUtils.getStringBetween(url, 0, "key=", "&").result
                author = StringUtils.getStringBetween(url, 0, "author=", "&").result
                fidGroup = StringUtils.getStringBetween(url, 0, "fidgroup=", "&").result
                content = StringUtils.getUrlParameter(url, "content")
                fid = StringUtils.getUrlParameter(url, "fid")
                stid = StringUtils.getUrlParameter(url, "stid")
            }
            extras != null -> BundleCompat.getParcelable(extras, ParamKey.KEY_PARAM, TopicListParam::class.java) ?: TopicListParam().apply {
                fid = extras.getInt(ParamKey.KEY_FID, 0)
                authorId = extras.getInt(ParamKey.KEY_AUTHOR_ID, 0)
                content = extras.getInt(ParamKey.KEY_CONTENT, 0)
                searchPost = extras.getInt(ParamKey.KEY_SEARCH_POST, 0)
                favor = extras.getInt(ParamKey.KEY_FAVOR, 0)
                key = extras.getString(ParamKey.KEY_KEY)
                author = extras.getString(ParamKey.KEY_AUTHOR)
                fidGroup = extras.getString(ParamKey.KEY_FID_GROUP)
                title = extras.getString(ParamKey.KEY_TITLE)
                recommend = extras.getInt(ParamKey.KEY_RECOMMEND, 0)
                twentyfour = extras.getInt(ParamKey.KEY_TWENTYFOUR, 0)
                stid = extras.getInt(ParamKey.KEY_STID, 0)
                boardHead = extras.getString(ParamKey.BOARD_HEAD, null)
            }
            else -> null
        } ?: return null
        if (request.title.isNullOrEmpty()) request.title = BoardModel.getInstance().getBoardName(request.fid, request.stid)
        return request
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

    private fun updateActions() {
        val menu = PopupMenu(this, View(this)).menu
        menuInflater.inflate(R.menu.topic_list_menu, menu)
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
            AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_LOGIN).navigation(this)
            return
        }
        when (id) {
            R.id.menu_add_bookmark -> {
                viewModel.addBookmarkBoard(Board(param.fid, param.stid, param.title ?: store.ui.title).also { it.boardHead = param.boardHead })
                updateActions()
            }
            R.id.menu_remove_bookmark -> { viewModel.removeBookmarkBoard(); updateActions() }
            R.id.menu_history -> ActivityUtils.startHistoryTopicActivity(this)
            R.id.menu_board_head -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_CONTENT)
                .withInt(ParamKey.KEY_TID, param.boardHead.toInt())
                .withString(ParamKey.KEY_TITLE, "${param.title} - 版头")
                .navigation(this)
            R.id.menu_sub_board -> subBoardLauncher.launch(Intent(this, SubBoardActivity::class.java).apply {
                putExtra(ParamKey.KEY_TITLE, param.title)
                putExtra(ParamKey.KEY_FID, param.fid)
                putParcelableArrayListExtra("subBoard", store.info?.subBoardList)
            })
            R.id.menu_favorite -> ActivityUtils.startFavoriteTopicActivity(this)
            R.id.menu_recommend -> ActivityUtils.startRecommendTopicActivity(this,
                Intent().putExtra(ParamKey.KEY_PARAM, (param.clone() as TopicListParam).apply { recommend = 1 }))
            R.id.menu_twenty_four -> ActivityUtils.startTwentyFourActivity(this,
                Intent().putExtra(ParamKey.KEY_PARAM, (param.clone() as TopicListParam).apply { twentyfour = 1 }))
            R.id.menu_search -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_SEARCH).withInt("fid", param.fid).navigation(this)
        }
    }

    override fun usesComposeContent() = true
}
