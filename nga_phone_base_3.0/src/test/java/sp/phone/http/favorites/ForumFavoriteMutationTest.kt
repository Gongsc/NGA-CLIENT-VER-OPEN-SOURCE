package sp.phone.http.favorites

import gov.anzong.androidnga.coroutines.PendingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForumFavoriteMutationTest {
    private val main = UnconfinedTestDispatcher()
    @Before fun setMain() = Dispatchers.setMain(main)
    @After fun resetMain() = Dispatchers.resetMain()
    private val board = ForumFavoriteBoard(-7, 0, "版块")
    private class Cache : ForumFavoritesCache {
        val data = mutableMapOf<String, List<ForumFavoriteBoard>>()
        override fun read(uid: String) = data[uid].orEmpty()
        override fun write(uid: String, boards: List<ForumFavoriteBoard>) { data[uid] = boards }
    }

    @Test fun requestUsesCorrectActionAndCollectionId() {
        assertEquals(mapOf("action" to "add", "fid" to "-7"), ForumFavoritesParser.mutationFields(board, true))
        assertEquals(mapOf("action" to "del", "fid" to "42"), ForumFavoritesParser.mutationFields(board.copy(stid = 42), false))
        assertTrue(runCatching { ForumFavoritesParser.mutationFields(ForumFavoriteBoard(0, 0, ""), true) }.isFailure)
    }

    @Test fun acknowledgementsAreRequiredAndBusinessErrorsAreRejected() {
        ForumFavoritesParser.parseMutationResponse("""{"data":{"0":"操作成功"}}""")
        ForumFavoritesParser.parseMutationResponse("""{"data":{"__MESSAGE":{"0":"操作成功！"}}}""")
        for (source in listOf("{}", "<html>网络错误</html>", """{"error":{"0":"收藏失败"}}""", """{"data":{"__MESSAGE":{"1":"未登录"}}}""")) {
            assertTrue(runCatching { ForumFavoritesParser.parseMutationResponse(source) }.isFailure)
        }
    }

    @Test fun addUpdatesCacheOnlyAfterServerAcknowledges() {
        val ack = PendingResponse<Unit>()
        val cache = Cache()
        var done = 0
        val sync = ForumFavoriteSync({ emptyList() }, cache, {}, write = { cookie, item, add ->
            assertEquals("cookie-A", cookie); assertEquals(board, item); assertTrue(add); ack.await()
        })
        sync.change("A", "cookie-A", board, true, { done++ }, { fail(it) })
        assertTrue(sync.state.boards.isEmpty())
        assertNull(cache.data["A"])
        assertTrue(sync.state.mutating)
        ack.complete(Unit)
        assertEquals(listOf(board), cache.data["A"])
        assertEquals(listOf(board), sync.state.boards)
        assertFalse(sync.state.mutating)
        assertEquals(1, done)
    }

    @Test fun removeFailurePreservesFavoriteAndAllowsRetry() {
        val cache = Cache().apply { data["A"] = listOf(board) }
        var count = 0
        var message: String? = null
        val sync = ForumFavoriteSync({ listOf(board) }, cache, {}, write = { _, _, add ->
            assertFalse(add)
            if (++count == 1) throw IllegalStateException("无权限") else Unit
        })
        sync.change("A", "cookie-A", board, false, {}, { message = it })
        assertEquals("无权限", message)
        assertEquals(listOf(board), cache.data["A"])
        sync.change("A", "cookie-A", board, false, {}, { fail(it) })
        assertTrue(cache.data.getValue("A").isEmpty())
        assertTrue(sync.state.boards.isEmpty())
    }

    @Test fun accountSwitchCannotApplyPendingMutationToAnotherAccount() {
        val ack = PendingResponse<Unit>()
        val cache = Cache()
        var callbacks = 0
        val sync = ForumFavoriteSync({ emptyList() }, cache, {}, write = { _, _, _ -> ack.await() })
        sync.change("A", "cookie-A", board, true, { callbacks++ }, { callbacks++ })
        sync.refresh("B", "cookie-B")
        ack.complete(Unit)
        assertEquals("B", sync.state.accountId)
        assertTrue(sync.state.boards.isEmpty())
        assertNull(cache.data["A"])
        assertEquals(0, callbacks)
    }

    @Test fun pendingMutationBlocksDuplicateWritesAndConflictingRefresh() {
        val ack = PendingResponse<Unit>()
        var writes = 0
        var reads = 0
        var errors = 0
        val sync = ForumFavoriteSync({ reads++; emptyList() }, Cache(), {}, write = { _, _, _ -> writes++; ack.await() })
        sync.change("A", "cookie-A", board, true, {}, { fail(it) })
        sync.refresh("A", "cookie-A", force = true)
        sync.change("A", "cookie-A", board, false, {}, { errors++ })
        assertEquals(1, writes)
        assertEquals(0, reads)
        assertEquals(1, errors)
        ack.complete(Unit)
        assertEquals(listOf(board), sync.state.boards)
    }

    @Test fun signedOutAndInvalidBoardsDoNotReachServer() {
        var writes = 0
        var errors = 0
        val sync = ForumFavoriteSync({ emptyList() }, Cache(), {}, write = { _, _, _ -> writes++; Unit })
        sync.change("", "", board, true, { fail() }, { errors++ })
        sync.change("A", "cookie-A", ForumFavoriteBoard(0, 0, ""), true, { fail() }, { errors++ })
        assertEquals(0, writes)
        assertEquals(2, errors)
    }
}
