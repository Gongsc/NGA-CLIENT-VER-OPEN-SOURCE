package gov.anzong.androidnga.ui.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.network.favorites.ForumFavoriteBoard
import gov.anzong.androidnga.network.favorites.ForumFavoritesSnapshot

class HomeFavoritesTest {
    @get:Rule val compose = createComposeRule()
    private fun home() = HomeState().apply {
        categories = listOf(HomeCategory("我的收藏", true, listOf(BoardSection("", listOf(BoardTile(7, 0, "本机版块"))))))
    }

    @Test fun signedOutUserCanStartLoginAndSync() {
        var login = 0
        compose.setContent { NgaTheme {
            HomeScreen(home(), emptyList(), {}, {}, {}, {}, {}, onSyncFavorites = { login++ }, onRemove = {})
        } }
        compose.onNodeWithContentDescription("登录并同步论坛收藏").performClick()
        compose.runOnIdle { assertEquals(1, login) }
        compose.onNodeWithText("本机版块").assertDoesNotExist()
    }

    @Test fun onlyForumFavoritesAreShownAndCanBeRemoved() {
        var selected: BoardTile? = null
        var removed: BoardTile? = null
        val favorites = ForumFavoritesSnapshot("fixture", listOf(ForumFavoriteBoard(-7, 42, "论坛合集")))
        compose.setContent { NgaTheme {
            HomeScreen(home(), emptyList(), {}, { selected = it }, {}, {}, {},
                onRemove = { removed = it }, favorites = favorites)
        } }
        compose.onNodeWithText("本机版块").assertDoesNotExist()
        compose.onNodeWithText("论坛合集").performClick()
        compose.runOnIdle { assertEquals(-7, selected?.fid); assertEquals(42, selected?.stid) }
        compose.onNodeWithContentDescription("首页操作").performClick()
        compose.onNodeWithText("管理收藏").performClick()
        compose.onAllNodesWithContentDescription("取消论坛收藏：论坛合集").assertCountEquals(1)
        compose.onNodeWithContentDescription("取消论坛收藏：论坛合集").performClick()
        compose.runOnIdle { assertEquals("论坛合集", removed?.name) }
        compose.onNodeWithText("论坛合集").assertIsDisplayed()
        compose.onNodeWithText("前移").assertDoesNotExist()
        compose.onNodeWithText("本机收藏").assertDoesNotExist()
    }

    @Test fun syncFailureKeepsCachedFavoritesAndManualRetryRemainsAvailable() {
        var retries = 0
        val favorites = mutableStateOf(ForumFavoritesSnapshot("fixture",
            listOf(ForumFavoriteBoard(7, 0, "已缓存论坛版块")), error = "网络不可用"))
        compose.setContent { NgaTheme {
            HomeScreen(home(), emptyList(), {}, {}, {}, {}, {}, onSyncFavorites = { retries++ },
                onRemove = {}, favorites = favorites.value)
        } }
        compose.onNodeWithText("网络不可用").assertIsDisplayed()
        compose.onNodeWithText("已缓存论坛版块").assertIsDisplayed()
        compose.onNodeWithContentDescription("同步论坛收藏").performClick()
        compose.runOnIdle { assertEquals(1, retries); favorites.value = favorites.value.copy(loading = true, error = null) }
        compose.onNodeWithContentDescription("同步论坛收藏").assertIsNotEnabled()
    }
    @Test fun boardMenuDisablesFavoriteActionUntilPendingRequestCompletes() {
        val feed = TopicFeedState().apply { title = "版块"; actions = listOf(HomeAction(1, "收藏到论坛", enabled = false)) }
        var clicks = 0
        compose.setContent { NgaTheme {
            TopicFeedScreen(feed, {}, {}, {}, {}, {}, {}, { clicks++ })
        } }
        compose.onNodeWithContentDescription("更多操作").performClick()
        compose.onNodeWithText("收藏到论坛").assertIsNotEnabled()
        compose.runOnIdle { feed.actions = listOf(HomeAction(1, "取消论坛收藏")) }
        compose.onNodeWithText("取消论坛收藏").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

}
