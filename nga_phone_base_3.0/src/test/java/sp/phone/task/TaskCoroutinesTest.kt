package sp.phone.task

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import gov.anzong.androidnga.coroutines.FakeNgaApi
import gov.anzong.androidnga.coroutines.NgaDispatchers
import gov.anzong.androidnga.coroutines.PendingResponse
import gov.anzong.androidnga.http.OnHttpCallBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sp.phone.mvp.model.entity.SubBoard
import sp.phone.param.SignPostParam

/** 后台任务改用协程后的行为：请求内容、回调文字、失败处理与取消。响应均为合成数据。 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskCoroutinesTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        NgaDispatchers.io = dispatcher
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        NgaDispatchers.io = Dispatchers.IO
    }

    private class Recorder<T> : OnHttpCallBack<T> {
        val events = mutableListOf<String>()
        override fun onSuccess(data: T) { events += "success:$data" }
        override fun onError(text: String?) { events += "error:$text" }
    }

    private class Page : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }

    @Test fun reportDistinguishesServerErrorDataAndFailures() {
        val api = FakeNgaApi()
        val task = ReportTask { api }
        val recorder = Recorder<String>()
        for (response in listOf(
            "{\"error\":{\"0\":\"你在217秒后方可举报\"},\"time\":1}",
            "{\"data\":{\"0\":\"操作成功\"},\"time\":1}",
            "{\"time\":1}",
        )) {
            api.respond = { response }
            task.pos(mapOf("__lib" to "log_post"), mapOf("info" to "原因"), recorder)
        }
        api.respond = { throw IllegalStateException("网络不可用") }
        task.pos(emptyMap(), emptyMap(), recorder)
        api.respond = { "<html>无法解析</html>" }
        task.pos(emptyMap(), emptyMap(), recorder)
        assertEquals("POST nuke.php {__lib=log_post} {info=原因}", api.requests.first())
        assertEquals(4, recorder.events.size)
        assertEquals(listOf("error:你在217秒后方可举报", "success:操作成功", "error:网络不可用"), recorder.events.take(3))
        assertTrue(recorder.events[3].startsWith("error:"))
    }

    @Test fun signPostReportsResultAndIgnoresRepeatWhileRunning() {
        val pending = PendingResponse<String>()
        val api = FakeNgaApi { pending.await() }
        val task = SignPostTask { api }
        val recorder = Recorder<String>()
        val param = SignPostParam().apply { uid = "9"; sign = "签名 a" }
        task.execute(param, recorder)
        assertTrue(task.isRunning())
        task.execute(param, recorder)
        assertEquals(1, api.requests.size)
        assertTrue(api.requests.single().contains("sign=%C7%A9%C3%FB+a"))
        assertTrue(api.requests.single().contains("uid=9"))
        pending.complete("{\"0\":\"操作成功\"}")
        assertFalse(task.isRunning())
        api.respond = { "签名过长" }
        task.execute(param, recorder)
        assertEquals(listOf("success:修改成功！", "error:签名过长"), recorder.events)
    }

    @Test fun signPostCancelDropsCallback() {
        val pending = PendingResponse<String>()
        val task = SignPostTask { FakeNgaApi { pending.await() } }
        val recorder = Recorder<String>()
        task.execute(SignPostParam().apply { uid = "9"; sign = "a" }, recorder)
        task.cancel()
        pending.complete("操作成功")
        assertTrue(pending.cancelled)
        assertFalse(task.isRunning())
        assertTrue(recorder.events.isEmpty())
    }

    @Test fun subBoardActionsKeepInvertedTypesAndMessages() {
        val api = FakeNgaApi { "操作成功" }
        val task = SubscribeSubBoardTask(null) { api }
        val board = SubBoard().apply { type = 0; fid = 7; parentFidStr = "-7" }
        val inverted = SubBoard().apply { type = 1; tidStr = "42"; parentFidStr = "-7" }
        val recorder = Recorder<String>()
        task.subscribe(board, recorder)
        task.unsubscribe(inverted, recorder)
        api.respond = { "没有权限" }
        task.subscribe(board, recorder)
        api.respond = { throw IllegalStateException("超时") }
        task.unsubscribe(board, recorder)
        assertEquals(
            listOf(
                "POST http://bbs.ngacn.cc/nuke.php?__lib=user_option&__act=set&raw=3&type=0&__output=8&fid=-7&add=7",
                "POST http://bbs.ngacn.cc/nuke.php?__lib=user_option&__act=set&raw=3&type=1&__output=8&fid=-7&add=42",
                "POST http://bbs.ngacn.cc/nuke.php?__lib=user_option&__act=set&raw=3&type=0&__output=8&fid=-7&add=7",
                "POST http://bbs.ngacn.cc/nuke.php?__lib=user_option&__act=set&raw=3&type=0&__output=8&fid=-7&del=7",
            ),
            api.requests,
        )
        assertEquals(
            listOf("success:取消屏蔽成功，请刷新界面！", "success:屏蔽子板块成功，请刷新界面！", "error:没有权限", "error:超时"),
            recorder.events,
        )
    }

    @Test fun subBoardRequestIsCancelledWithPage() {
        val pending = PendingResponse<String>()
        val page = Page()
        val recorder = Recorder<String>()
        SubscribeSubBoardTask(page) { FakeNgaApi { pending.await() } }
            .subscribe(SubBoard().apply { fid = 7; parentFidStr = "-7" }, recorder)
        page.registry.currentState = Lifecycle.State.DESTROYED
        pending.complete("操作成功")
        assertTrue(pending.cancelled)
        assertTrue(recorder.events.isEmpty())
    }

    @Test fun notificationsParseOffMainAndClearUsesDeleteEndpoint() {
        val reply = "{\"0\":1,\"1\":\"7\",\"2\":\"示例\",\"5\":\"主题\",\"6\":\"100\",\"7\":\"200\",\"9\":\"1\"}"
        val api = FakeNgaApi { "{\"data\":{\"0\":{\"0\":[$reply],\"1\":[{\"0\":\"5\",\"2\":\"发信人\"}],\"unread\":1}}}" }
        val task = ForumNotificationTask(null, { api }, { "https://bbs.nga.cn/" })
        val replies = mutableListOf<Int>()
        val all = mutableListOf<Int>()
        task.queryRecentReply(object : OnHttpCallBack<List<sp.phone.mvp.model.entity.RecentReplyInfo>> {
            override fun onSuccess(data: List<sp.phone.mvp.model.entity.RecentReplyInfo>) { replies += data.size }
        })
        task.queryNotification(object : OnHttpCallBack<List<sp.phone.mvp.model.entity.NotificationInfo>> {
            override fun onSuccess(data: List<sp.phone.mvp.model.entity.NotificationInfo>) { all += data.size }
        })
        assertEquals(listOf(1), replies)
        assertEquals(listOf(2), all)
        assertEquals(
            listOf("GET https://bbs.nga.cn/nuke.php?__lib=noti&__output=8&__act=get_all", "GET https://bbs.nga.cn/nuke.php?__lib=noti&__output=8&__act=get_all"),
            api.requests,
        )
    }

    @Test fun likeSendsVoteAndReportsServerText() {
        val api = FakeNgaApi { "{\"data\":{\"0\":\"操作成功\"}}" }
        val results = mutableListOf<String>()
        LikeTask { api }.execute(12, 34, LikeTask.OPPOSE) { results += it }
        assertEquals(listOf("操作成功"), results)
        assertEquals(
            "POST nuke.php {__act=add, __lib=topic_recommend, __output=8, pid=34, raw=3, tid=12, value=-1}",
            api.requests.single(),
        )
    }
}
