package sp.phone.ui.fragment

import androidx.compose.runtime.*
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.mvvm.viewmodel.HomeInboxViewModel
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.arouter.ARouterConstants
import sp.phone.common.TopicHistoryManager
import sp.phone.common.PhoneConfiguration
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.param.ArticleListParam
import sp.phone.param.ParamKey
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import gov.anzong.androidnga.coroutines.launchWithCallback
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
import gov.anzong.androidnga.Utils
import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.contract.BoardContract
import sp.phone.mvp.model.entity.Board
import sp.phone.mvp.presenter.BoardPresenter
import sp.phone.rxjava.RxEvent
import sp.phone.ui.fragment.dialog.AddBoardDialogFragment
import sp.phone.util.ActivityUtils

/** Compose owns the UI; the existing presenter continues to own forum/account actions. */
class NavigationDrawerFragment : BaseMvpFragment<BoardPresenter>(), BoardContract.View {
    private val state = HomeState()
    private lateinit var inbox: HomeInboxViewModel
    private var history by mutableStateOf(emptyList<ThreadPageInfo>(), neverEqualPolicy())
    private lateinit var favorites: ForumFavoritesViewModel
    private val avatarRequests = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerRxBus()
        inbox = ViewModelProvider(this)[HomeInboxViewModel::class.java]
        favorites = ViewModelProvider(this)[ForumFavoritesViewModel::class.java]
        state.selected = savedInstanceState?.getInt("selected_category")
            ?: if (mPresenter.bookmarkCategory.size() == 0 && !UserManagerImpl.getInstance().hasValidUser()) 1 else 0
        refreshState()
    }

    override fun onCreatePresenter() = BoardPresenter()

    override fun accept(event: RxEvent) {
        if (event.what == RxEvent.EVENT_SHOW_TOPIC_LIST) mPresenter.showTopicList(event.obj as Board)
        else super.accept(event)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            id = R.id.compose_home
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            val actions = navigationActions()
            setContent {
                NgaTheme {
                    HomeScreen(state, actions, ::handleAction,
                        onBoard = { mPresenter.showTopicList(it.toBoard()) },
                        onSearch = { (requireActivity() as MainActivity).handleNavigation(R.id.menu_search) },
                        onProfile = {
                            val user = UserManagerImpl.getInstance().activeUser
                            if (user == null) jumpToLogin() else mPresenter.startUserProfile(user.userId)
                        },
                        onSwitchUser = { mPresenter.toggleUser(UserManagerImpl.getInstance().userList) },
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

    private fun openHistory(topic: ThreadPageInfo) {
        val param = ArticleListParam().apply {
            tid = topic.tid; page = topic.page.coerceAtLeast(1); title = topic.subject
            topicInfo = NgaJson.toJSONString(topic)
        }
        startActivity(Intent(requireContext(), PhoneConfiguration.getInstance().articleActivityClass).putExtra(ParamKey.KEY_PARAM, param))
    }

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
                .setOnAddBookmarkListener { fid, stid -> mPresenter.addBoard(fid, stid) }
                .show(childFragmentManager)
            R.id.menu_login -> jumpToLogin()
            else -> (requireActivity() as MainActivity).handleNavigation(id)
        }
    }

    private fun refreshState() {
        state.categories = mPresenter.boardCategories.map { category ->
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

    override fun notifyDataSetChanged() = refreshState()
    override fun getCurrentItem() = state.selected
    override fun updateHeaderView() {
        val users = UserManagerImpl.getInstance()
        val active = users.activeUser
        val account = active?.toHomeAccount()
        if (state.account?.uid != account?.uid) state.replyCount = 0
        state.account = account
        inbox.selectAccount(account?.uid.orEmpty())
        fetchMissingAvatar(active)
        state.otherAccounts = users.userList.orEmpty().filter { it.userId != active?.userId }.map { it.toHomeAccount() }
    }
    /** 账号头像只在浏览到自己的帖子时才会记录；缺失时从个人资料补取一次并写回账号。 */
    private fun fetchMissingAvatar(user: User?) {
        val uid = user?.userId?.takeIf { it.isNotEmpty() } ?: return
        if (!user.avatarUrl.isNullOrBlank() || !avatarRequests.add(uid)) return
        val users = UserManagerImpl.getInstance()
        val cached = users.getAvatarUrl(uid)
        if (cached.isNotBlank()) { applyAvatar(uid, cached); return }
        val headers = mapOf("Cookie" to users.getCookie(user).orEmpty(),
            "Referer" to Utils.getNGAHost() + "nuke.php?func=ucp&lite=jsx&uid=$uid")
        // 随页面销毁取消，与原先在 onDestroy 中释放订阅一致。
        lifecycleScope.launchWithCallback({ url -> if (url.isNotBlank()) applyAvatar(uid, url) }, { avatarRequests.remove(uid) }) {
            parseProfileAvatar(NgaApi.get().get(Utils.getNGAHost() + "nuke.php?__lib=ucp&__act=get&lite=js&noprefix&uid=$uid", headers))
        }
    }

    private fun applyAvatar(uid: String, url: String) {
        val users = UserManagerImpl.getInstance()
        users.putAvatarUrl(uid, url)
        uid.toIntOrNull()?.let { users.setAvatarUrl(it, url) }
        if (users.activeUser?.userId == uid) updateHeaderView()
    }


    private fun User.toHomeAccount() = HomeAccount(userId, nickName.orEmpty(), FunctionUtils.parseAvatarUrl(avatarUrl))
    override fun jumpToLogin() = mPresenter.startLogin()
    override fun switchToNextUser(): Int {
        val users = UserManagerImpl.getInstance()
        val next = (users.activeUserIndex + 1) % users.userSize.coerceAtLeast(1)
        users.setActiveUser(next)
        updateHeaderView()
        favorites.refresh()
        return next
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ActivityUtils.REQUEST_CODE_LOGIN && resultCode == Activity.RESULT_OK) {
            state.selected = 0
            refreshState()
            favorites.refresh(force = true)
        } else if (requestCode == ActivityUtils.REQUEST_CODE_SETTING) refreshState()
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
