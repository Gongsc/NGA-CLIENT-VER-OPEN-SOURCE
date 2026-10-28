package sp.phone.ui.fragment

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.mvvm.viewmodel.BoardViewModel
import gov.anzong.androidnga.mvvm.viewmodel.HomeInboxViewModel
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.arouter.TopicNavigation
import sp.phone.common.TopicHistoryManager
import sp.phone.common.PhoneConfiguration
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.param.ParamKey
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import sp.phone.mvp.viewmodel.ForumFavoritesViewModel
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import gov.anzong.androidnga.R
import gov.anzong.androidnga.activity.ForumListActivity
import gov.anzong.androidnga.activity.MainActivity
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.ui.compose.*
import sp.phone.common.User
import sp.phone.common.UserManagerImpl
import sp.phone.util.FunctionUtils
import sp.phone.mvp.model.entity.Board
import gov.anzong.androidnga.coroutines.UiEvent
import sp.phone.ui.fragment.dialog.AddBoardDialogFragment
import sp.phone.util.ActivityUtils

/** 首页：界面由 Compose 绘制，版块收藏与账号由 [BoardViewModel] 处理，论坛收藏与消息各有 ViewModel。 */
class NavigationDrawerFragment : BaseEventFragment() {
    private val state = HomeState()
    private lateinit var inbox: HomeInboxViewModel
    private var history by mutableStateOf(emptyList<ThreadPageInfo>(), neverEqualPolicy())
    private lateinit var favorites: ForumFavoritesViewModel
    private lateinit var boards: BoardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerUiEvents()
        inbox = ViewModelProvider(this)[HomeInboxViewModel::class.java]
        favorites = ViewModelProvider(this)[ForumFavoritesViewModel::class.java]
        boards = ViewModelProvider(this)[BoardViewModel::class.java]
        boards.avatarUpdated.observe(this) { uid -> if (UserManagerImpl.getInstance().activeUser?.userId == uid) updateHeaderView() }
        state.selected = savedInstanceState?.getInt("selected_category") ?: boards.defaultCategory
        refreshState()
    }

    override fun onUiEvent(event: UiEvent) {
        if (event.what == UiEvent.EVENT_SHOW_TOPIC_LIST) showTopicList(event.obj as Board)
        else super.onUiEvent(event)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            id = R.id.compose_home
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val actions = navigationActions()
            setContent {
                NgaTheme {
                    HomeScreen(state, actions, ::handleAction,
                        onBoard = { showTopicList(it.toBoard()) },
                        onSearch = { (requireActivity() as MainActivity).handleNavigation(R.id.menu_search) },
                        onProfile = {
                            val user = UserManagerImpl.getInstance().activeUser
                            if (user == null) jumpToLogin() else startUserProfile(user.userId)
                        },
                        onSwitchUser = ::switchToNextUser,
                        onSyncFavorites = {
                            if (UserManagerImpl.getInstance().hasValidUser()) favorites.refresh(force = true) else jumpToLogin()
                        },
                        onRemove = { board ->
                            AlertDialog.Builder(requireContext()).setMessage("是否取消论坛收藏“${board.name}”？")
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(android.R.string.ok) { _, _ -> favorites.change(
                                    sp.phone.http.favorites.ForumFavoriteBoard(board.fid, board.stid, board.name, board.head), false)
                                }.show()
                        },
                        favorites = favorites.snapshot,
                        historyContent = { onControls ->
                            HomeHistoryScreen(history, ::openHistory,
                                onRemove = { TopicHistoryManager.getInstance().removeTopicHistory(it); refreshHistory() },
                                onClear = { TopicHistoryManager.getInstance().removeAllTopicHistory(); refreshHistory() },
                                onControlsExpandedChange = onControls)
                        },
                        messagesContent = { onControls ->
                            HomeMessagesScreen(state.account, inbox, state.replyCount, ::jumpToLogin,
                                onReply = { reply ->
                                    val tid = reply.tidStr?.toIntOrNull()
                                    val pid = reply.pidStr?.toIntOrNull()
                                    if (tid != null && pid != null) startActivity(Intent(requireContext(), PhoneConfiguration.getInstance().articleActivityClass)
                                        .putExtra(ParamKey.KEY_TID, tid).putExtra(ParamKey.KEY_PID, pid)
                                        .putExtra(ParamKey.KEY_TITLE, reply.title).putExtra(ParamKey.KEY_SEARCH_POST, 1))
                                },
                                onMessage = { message -> startActivity(Intent(requireContext(), PhoneConfiguration.getInstance().messageDetialActivity).putExtra("mid", message.mid)) },
                                onCompose = { AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_MESSAGE_POST).withString("action", "new").navigation(requireContext()) },
                                onRead = {
                                    state.replyCount = 0
                                    requireContext().getSharedPreferences(requireContext().packageName + "_preferences", android.content.Context.MODE_PRIVATE)
                                        .edit().putInt(PreferenceKey.KEY_REPLY_COUNT, 0).apply()
                                }, onControlsExpandedChange = onControls)
                        })
                }
            }
        }
    }

    private fun refreshHistory() { history = TopicHistoryManager.getInstance().topicHistoryList.toList() }

    private fun openHistory(topic: ThreadPageInfo) = TopicNavigation.openHistory(requireContext(), topic)

    private fun navigationActions(): List<HomeAction> {
        val menu = PopupMenu(requireContext(), View(requireContext())).menu
        requireActivity().menuInflater.inflate(R.menu.main_navigation_menu, menu)
        requireActivity().menuInflater.inflate(R.menu.main_option_menu, menu)
        fun flatten(source: Menu): List<HomeAction> = (0 until source.size()).flatMap { index ->
            val item = source.getItem(index)
            if (item.hasSubMenu()) flatten(item.subMenu!!) else listOf(HomeAction(item.itemId, item.title.toString()))
        }
        return flatten(menu).distinctBy { it.id }
    }

    private fun handleAction(id: Int) {
        when (id) {
            R.id.menu_add -> startActivity(Intent(requireContext(), ForumListActivity::class.java))
            R.id.menu_add_id -> AddBoardDialogFragment()
                .setOnAddBookmarkListener(::addBoard)
                .show(childFragmentManager)
            R.id.menu_login -> jumpToLogin()
            else -> (requireActivity() as MainActivity).handleNavigation(id)
        }
    }

    private fun refreshState() {
        state.categories = boards.categories.map { category ->
            val sections = category.subCategoryList?.map { BoardSection(it.name.orEmpty(), it.boardList.map(Board::toTile)) }
                ?: listOf(BoardSection("", category.boardList.map(Board::toTile)))
            HomeCategory(category.name.orEmpty(), category.isBookmarkCategory, sections)
        }
        state.selected = state.selected.coerceIn(0, (state.categories.size - 1).coerceAtLeast(0))
        updateHeaderView()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
        refreshHistory()
        favorites.refresh()
        state.replyCount = requireContext().getSharedPreferences(requireContext().packageName + "_preferences", android.content.Context.MODE_PRIVATE).getInt(PreferenceKey.KEY_REPLY_COUNT, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected_category", state.selected)
        super.onSaveInstanceState(outState)
    }

    private fun updateHeaderView() {
        val users = UserManagerImpl.getInstance()
        val active = users.activeUser
        val account = active?.toHomeAccount()
        if (state.account?.uid != account?.uid) state.replyCount = 0
        state.account = account
        inbox.selectAccount(account?.uid.orEmpty())
        boards.fetchMissingAvatar(active)
        state.otherAccounts = users.userList.orEmpty().filter { it.userId != active?.userId }.map { it.toHomeAccount() }
    }
    private fun User.toHomeAccount() = HomeAccount(userId, nickName.orEmpty(), FunctionUtils.parseAvatarUrl(avatarUrl))
    /** 登录成功后回到首个页签并强制同步当前账号的论坛收藏。 */
    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            state.selected = 0
            refreshState()
            favorites.refresh(force = true)
        }
    }

    private fun jumpToLogin() {
        loginLauncher.launch(AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_LOGIN).intent(requireContext()))
    }
    private fun switchToNextUser() {
        val user = boards.switchToNextAccount() ?: return jumpToLogin()
        updateHeaderView()
        favorites.refresh()
        ToastUtils.showToast("切换账户成功,当前账户名:" + user.nickName)
    }

    private fun addBoard(fid: String, stid: String) {
        when (val result = boards.addBoard(fid, stid)) {
            BoardViewModel.AddBoardResult.Added -> refreshState()
            BoardViewModel.AddBoardResult.Exists -> ToastUtils.showToast("该版面已存在")
            BoardViewModel.AddBoardResult.NeedLogin -> jumpToLogin()
            is BoardViewModel.AddBoardResult.Invalid -> ToastUtils.error(result.message)
        }
    }

    private fun showTopicList(board: Board) {
        AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST)
            .withInt(ParamKey.KEY_FID, board.fid)
            .withInt(ParamKey.KEY_STID, board.stid)
            .withString(ParamKey.KEY_TITLE, board.name)
            .withString(ParamKey.BOARD_HEAD, board.boardHead)
            .navigation(requireContext())
    }

    private fun startUserProfile(uid: String) {
        AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_PROFILE)
            .withString("mode", "uid")
            .withString("uid", uid)
            .navigation(requireContext())
    }

}

/** 从个人资料响应中取头像原始值；解析失败返回空串，由调用方保留默认头像。 */
internal fun parseProfileAvatar(js: String): String {
    val body = js.removePrefix("window.script_muti_get_var_store=").replace("/*\$js\$*/", "")
        .let { if (it.contains("/*error fill content")) it.substringBefore("/*error fill content") else it }
    val avatar = runCatching {
        NgaJson.parseObject(body)?.getJSONObject("data")?.getJSONObject("0")?.getString(PreferenceKey.PREFERENCE_AVATAR)
    }.getOrNull().orEmpty()
    val url = FunctionUtils.parseAvatarUrl(avatar).orEmpty()
    return if (url.startsWith("//")) "https:$url" else url
}
