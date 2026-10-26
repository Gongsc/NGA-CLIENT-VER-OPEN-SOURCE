package sp.phone.http.favorites

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.*
import org.junit.Test

class ForumFavoritesTest {
    private val first = ForumFavoriteBoard(-7, 0, "论坛版块", "123")
    private val second = ForumFavoriteBoard(0, 42, "合集")

    private class MemoryCache : ForumFavoritesCache {
        val entries = mutableMapOf<String, List<ForumFavoriteBoard>>()
        override fun read(uid: String) = entries[uid].orEmpty()
        override fun write(uid: String, boards: List<ForumFavoriteBoard>) { entries[uid] = boards }
    }

    @Test fun parsesServerObjectCollectionsAndKeepsNegativeIds() {
        val result = ForumFavoritesParser.parseResponse("""/*${'$'}js${'$'}*/{"data":{"0":{"12":{"fid":"-7","name":"论坛版块","topped_topic":123},"3":{"fid":0,"stid":"42","name":"合集"}}}}""")
        assertEquals(listOf(first, second), result)
    }

    @Test fun acceptsArrayCollectionsAndExplicitEmptyCollections() {
        assertEquals(listOf(first.copy(head = null)), ForumFavoritesParser.parseResponse("""{"data":[[{"fid":-7,"name":"论坛版块"}]]}"""))
        assertTrue(ForumFavoritesParser.parseResponse("""{"data":{"0":{}}}""").isEmpty())
        assertTrue(ForumFavoritesParser.parseResponse("""{"data":{"0":[]}}""").isEmpty())
    }

    @Test fun deduplicatesByForumOrCollectionIdentity() {
        val result = ForumFavoritesParser.parseResponse("""{"data":{"0":[{"fid":7,"name":"版块"},{"fid":7,"name":"重复"},{"fid":7,"stid":42,"name":"合集"}]}}""")
        assertEquals(2, result.size)
        assertEquals(42, result[1].stid)
    }

    @Test fun businessErrorsAndMalformedDataCannotBeMistakenForEmptyFavorites() {
        for (response in listOf("""{"error":{"0":"请先登录"}}""", """{"data":{"__MESSAGE":{"1":"权限不足"}}}""", """{"data":{}}""", """{"data":{"0":{"0":{"fid":7}}}}""", "<html>错误</html>")) {
            assertTrue(response, runCatching { ForumFavoritesParser.parseResponse(response) }.isFailure)
        }
    }

    @Test fun cacheRoundTripPreservesCollectionsAndTopicHeader() {
        val original = listOf(first, second)
        assertEquals(original, ForumFavoritesParser.parseCache(ForumFavoritesParser.serialize(original)))
    }

    @Test fun accountSwitchRestoresOnlyThatAccountsCacheAndIgnoresOldResponses() {
        val cache = MemoryCache().apply { entries["A"] = listOf(first); entries["B"] = listOf(second) }
        val requests = mutableMapOf<String, PublishSubject<List<ForumFavoriteBoard>>>()
        val sync = ForumFavoriteSync({ cookie -> PublishSubject.create<List<ForumFavoriteBoard>>().also { requests[cookie] = it } }, cache, {})
        sync.refresh("A", "cookie-A")
        assertEquals(listOf(first), sync.state.boards)
        sync.refresh("B", "cookie-B")
        assertEquals(listOf(second), sync.state.boards)
        requests.getValue("cookie-A").onNext(emptyList())
        assertEquals("B", sync.state.accountId)
        assertEquals(listOf(second), sync.state.boards)
        assertEquals(listOf(first), cache.entries["A"])
        requests.getValue("cookie-B").onNext(listOf(second, first))
        assertEquals(listOf(second, first), cache.entries["B"])
    }

    @Test fun failureRetainsLastKnownFavoritesAndAllowsRetry() {
        val cache = MemoryCache().apply { entries["A"] = listOf(first) }
        var attempt = 0
        val sync = ForumFavoriteSync({ if (++attempt == 1) Observable.error(IllegalStateException("网络不可用")) else Observable.just(listOf(second)) }, cache, {})
        sync.refresh("A", "cookie-A")
        assertEquals(listOf(first), sync.state.boards)
        assertEquals("网络不可用", sync.state.error)
        assertFalse(sync.state.loading)
        sync.refresh("A", "cookie-A", force = true)
        assertEquals(listOf(second), sync.state.boards)
        assertNull(sync.state.error)
    }

    @Test fun successfulEmptyResponseClearsOnlyCurrentAccountsServerCache() {
        val cache = MemoryCache().apply { entries["A"] = listOf(first); entries["B"] = listOf(second) }
        val sync = ForumFavoriteSync({ Observable.just(emptyList()) }, cache, {})
        sync.refresh("A", "cookie-A")
        assertTrue(cache.entries.getValue("A").isEmpty())
        assertEquals(listOf(second), cache.entries["B"])
    }

    @Test fun logoutClearsDisplayedServerFavoritesAndCancelsPendingSync() {
        val response = PublishSubject.create<List<ForumFavoriteBoard>>()
        val cache = MemoryCache().apply { entries["A"] = listOf(first) }
        val sync = ForumFavoriteSync({ response }, cache, {})
        sync.refresh("A", "cookie-A")
        sync.refresh("", "")
        response.onNext(listOf(second))
        assertEquals(ForumFavoritesSnapshot(), sync.state)
        assertEquals(listOf(first), cache.entries["A"])
    }

    @Test fun manualRefreshBypassesAutomaticThrottle() {
        var count = 0
        var now = 1000L
        val sync = ForumFavoriteSync({ count++; Observable.just(listOf(first)) }, MemoryCache(), {}, { now })
        sync.refresh("A", "cookie-A")
        sync.refresh("A", "cookie-A")
        assertEquals(1, count)
        sync.refresh("A", "cookie-A", force = true)
        assertEquals(2, count)
        now += 60_001
        sync.refresh("A", "cookie-A")
        assertEquals(3, count)
    }

    @Test fun refreshedSessionForSameAccountCancelsOldSession() {
        val old = PublishSubject.create<List<ForumFavoriteBoard>>()
        val fresh = PublishSubject.create<List<ForumFavoriteBoard>>()
        val cache = MemoryCache()
        val sync = ForumFavoriteSync({ if (it == "old") old else fresh }, cache, {})
        sync.refresh("A", "old")
        sync.refresh("A", "fresh")
        old.onNext(listOf(first))
        assertTrue(sync.state.boards.isEmpty())
        fresh.onNext(listOf(second))
        assertEquals(listOf(second), cache.entries["A"])
    }
}
