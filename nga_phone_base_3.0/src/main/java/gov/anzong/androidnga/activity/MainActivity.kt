package gov.anzong.androidnga.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import gov.anzong.androidnga.NgaClientApp
import gov.anzong.androidnga.R
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.arouter.TopicNavigation
import gov.anzong.androidnga.base.util.ThemeUtils
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.mvvm.viewmodel.BoardViewModel
import gov.anzong.androidnga.mvvm.viewmodel.HomeInboxViewModel
import gov.anzong.androidnga.ui.compose.*
import sp.phone.common.PhoneConfiguration
import sp.phone.common.TopicHistoryManager
import sp.phone.common.User
import sp.phone.common.UserManagerImpl
import sp.phone.http.favorites.ForumFavoriteBoard
import sp.phone.mvp.model.entity.Board
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.mvp.viewmodel.ForumFavoritesViewModel
import sp.phone.param.ParamKey
import sp.phone.theme.ThemeManager
import sp.phone.ui.fragment.dialog.AddBoardDialogFragment
import sp.phone.ui.fragment.dialog.UrlInputDialogFragment
import sp.phone.ui.fragment.dialog.VersionUpgradeDialogFragment
import sp.phone.util.FunctionUtils

/** 首页：界面由 Compose 绘制，版块收藏与账号由 [BoardViewModel] 处理，论坛收藏与消息各有 ViewModel。 */
class MainActivity : BaseActivity() {
    private val state = HomeState()
    private val inbox: HomeInboxViewModel by viewModels()
    private val favorites: ForumFavoritesViewModel by viewModels()
    private val boards: BoardViewModel by viewModels()
    private var history by mutableStateOf(emptyList<ThreadPageInfo>(), neverEqualPolicy())
    private var nightMode = false

    /** 登录成功后回到首个页签并强制同步当前账号的论坛收藏。 */
    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            state.selected = 0
            refreshState()
            favorites.refresh(force = true)
        }
    }

    /** 设置页返回 RESULT_OK 表示外观等设置已变化，需要重建页面。 */
    private val settingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        ThemeUtils.init(this)
        checkPermission()
        if (application is NgaClientApp && NgaClientApp.isNewVersion()) VersionUpgradeDialogFragment().show(supportFragmentManager, null)
        nightMode = ThemeManager.getInstance().isNightMode
        setTitle(R.string.start_title)
        if (!isTaskRoot) {
            finish()
            return
        }
        supportFragmentManager.setFragmentResultListener(AddBoardDialogFragment.RESULT_KEY, this) { _, result ->
            addBoard(result.getString("fid").orEmpty(), result.getString("stid").orEmpty())
        }
        boards.avatarUpdated.observe(this) { uid -> if (UserManagerImpl.getInstance().activeUser?.userId == uid) updateHeaderView() }
        state.selected = savedInstanceState?.getInt("selected_category") ?: boards.defaultCategory
        refreshState()
        val actions = navigationActions()
        setContent {
            NgaTheme {
                var removing by remember { mutableStateOf<BoardTile?>(null) }
                removing?.let { board ->
                    AlertDialog(onDismissRequest = { removing = null }, text = { Text("是否取消论坛收藏“${board.name}”？") },
                        confirmButton = {
                            TextButton(onClick = {
                                removing = null
                                favorites.change(ForumFavoriteBoard(board.fid, board.stid, board.name, board.head), false)
                            }) { Text("确定") }
                        },
                        dismissButton = { TextButton(onClick = { removing = null }) { Text("取消") } })
                }
                HomeScreen(state, actions, ::handleAction,
                    onBoard = { showTopicList(it.toBoard()) },
                    onSearch = { handleNavigation(R.id.menu_search) },
                    onProfile = {
                        val user = UserManagerImpl.getInstance().activeUser
                        if (user == null) jumpToLogin() else startUserProfile(user.userId)
                    },
                    onSwitchUser = ::switchToNextUser,
                    onSyncFavorites = {
                        if (UserManagerImpl.getInstance().hasValidUser()) favorites.refresh(force = true) else jumpToLogin()
                    },
                    onRemove = { removing = it },
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
                                if (tid != null && pid != null) startActivity(Intent(this, PhoneConfiguration.getInstance().articleActivityClass)
                                    .putExtra(ParamKey.KEY_TID, tid).putExtra(ParamKey.KEY_PID, pid)
                                    .putExtra(ParamKey.KEY_TITLE, reply.title).putExtra(ParamKey.KEY_SEARCH_POST, 1))
                            },
                            onMessage = { message -> startActivity(Intent(this, PhoneConfiguration.getInstance().messageDetialActivity).putExtra("mid", message.mid)) },
                            onCompose = { AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_MESSAGE_POST).withString("action", "new").navigation(this) },
                            onRead = {
                                state.replyCount = 0
                                preferences().edit().putInt(PreferenceKey.KEY_REPLY_COUNT, 0).apply()
                            }, onControlsExpandedChange = onControls)
                    })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (nightMode != ThemeManager.getInstance().isNightMode) {
            finish()
            startActivity(intent)
            return
        }
        if (isFinishing) return
        refreshState()
        refreshHistory()
        favorites.refresh()
        state.replyCount = preferences().getInt(PreferenceKey.KEY_REPLY_COUNT, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected_category", state.selected)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu) = false

    private fun preferences() = getSharedPreferences(packageName + "_preferences", Context.MODE_PRIVATE)

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun navigationActions(): List<HomeAction> {
        val menu = PopupMenu(this, View(this)).menu
        menuInflater.inflate(R.menu.main_navigation_menu, menu)
        menuInflater.inflate(R.menu.main_option_menu, menu)
        fun flatten(source: Menu): List<HomeAction> = (0 until source.size()).flatMap { index ->
            val item = source.getItem(index)
            if (item.hasSubMenu()) flatten(item.subMenu!!) else listOf(HomeAction(item.itemId, item.title.toString()))
        }
        return flatten(menu).distinctBy { it.id }
    }

    private fun handleAction(id: Int) {
        when (id) {
            R.id.menu_add -> startActivity(Intent(this, ForumListActivity::class.java))
            R.id.menu_add_id -> AddBoardDialogFragment().show(supportFragmentManager)
            R.id.menu_login -> jumpToLogin()
            else -> handleNavigation(id)
        }
    }

    private fun handleNavigation(id: Int) {
        when (id) {
            R.id.menu_appearance -> startActivity(Intent(this, AppearanceActivity::class.java))
            R.id.menu_setting -> settingLauncher.launch(Intent(this, SettingsActivity::class.java))
            R.id.menu_bookmark -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST).withInt(ParamKey.KEY_FAVOR, 1).navigation(this)
            R.id.menu_msg -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_MESSAGE_LIST).navigation(this)
            R.id.menu_post -> startOwnTopics(replies = false)
            R.id.menu_reply -> startOwnTopics(replies = true)
            R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.menu_search -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_SEARCH).navigation(this)
            R.id.menu_forward -> UrlInputDialogFragment().show(supportFragmentManager)
            R.id.menu_gun -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_NOTIFICATION).navigation(this)
            R.id.menu_download -> startActivity(Intent(this, TopicCacheActivity::class.java))
        }
    }

    /** 当前账号的主题或回复；未登录时 uid 为 0。 */
    private fun startOwnTopics(replies: Boolean) {
        val user = UserManagerImpl.getInstance().activeUser
        val request = AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST)
            .withInt(ParamKey.KEY_AUTHOR_ID, user?.userId?.toIntOrNull() ?: 0)
            .withString(ParamKey.KEY_AUTHOR, user?.nickName.orEmpty())
        if (replies) request.withInt(ParamKey.KEY_SEARCH_POST, 1)
        request.navigation(this)
    }

    private fun refreshHistory() { history = TopicHistoryManager.getInstance().topicHistoryList.toList() }

    private fun openHistory(topic: ThreadPageInfo) = TopicNavigation.openHistory(this, topic)

    private fun refreshState() {
        state.categories = boards.categories.map { category ->
            val sections = category.subCategoryList?.map { BoardSection(it.name.orEmpty(), it.boardList.map(Board::toTile)) }
                ?: listOf(BoardSection("", category.boardList.map(Board::toTile)))
            HomeCategory(category.name.orEmpty(), category.isBookmarkCategory, sections)
        }
        state.selected = state.selected.coerceIn(0, (state.categories.size - 1).coerceAtLeast(0))
        updateHeaderView()
    }

    private fun updateHeaderView() {
        val users = UserManagerImpl.getInstance()
        val active = users.activeUser
        val account = active?.toHomeAccount()
        if (state.account?.uid != account?.uid) state.replyCount = 0
        state.account = account
        inbox.selectAccount(account?.uid.orEmpty())
        boards.refreshAccountAvatar(active)
        state.otherAccounts = users.userList.orEmpty().filter { it.userId != active?.userId }.map { it.toHomeAccount() }
    }

    private fun User.toHomeAccount() = HomeAccount(userId, nickName.orEmpty(), FunctionUtils.parseAvatarUrl(avatarUrl))

    private fun jumpToLogin() {
        loginLauncher.launch(AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_LOGIN).intent(this))
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
            .navigation(this)
    }

    private fun startUserProfile(uid: String) {
        AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_PROFILE)
            .withString("mode", "uid")
            .withString("uid", uid)
            .navigation(this)
    }

    override fun usesComposeContent() = true
}
