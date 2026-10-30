package gov.anzong.androidnga.task

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import gov.anzong.androidnga.coroutines.FakeNgaApi
import gov.anzong.androidnga.coroutines.NgaDispatchers
import gov.anzong.androidnga.http.OnHttpCallBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import gov.anzong.androidnga.network.post.FakeForum
import gov.anzong.androidnga.network.post.ForumActionClient
import gov.anzong.androidnga.network.post.NgaWriteClient
import gov.anzong.androidnga.network.post.Reply
import gov.anzong.androidnga.network.post.form
import gov.anzong.androidnga.mvvm.model.param.SignPostParam

/** 后台任务改用协程后的行为：请求内容、回调文字与失败处理。响应均为合成数据。 */
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

    @Test fun notificationsParseOffMainAndClearUsesDeleteEndpoint() {
        val reply = "{\"0\":1,\"1\":\"7\",\"2\":\"示例\",\"5\":\"主题\",\"6\":\"100\",\"7\":\"200\",\"9\":\"1\"}"
        val api = FakeNgaApi { "{\"data\":{\"0\":{\"0\":[$reply],\"1\":[{\"0\":\"5\",\"2\":\"发信人\"}],\"unread\":1}}}" }
        val task = ForumNotificationTask(null, { api }, { "https://bbs.nga.cn/" })
        val replies = mutableListOf<Int>()
        val all = mutableListOf<Int>()
        task.queryRecentReply(object : OnHttpCallBack<List<gov.anzong.androidnga.mvvm.model.entity.RecentReplyInfo>> {
            override fun onSuccess(data: List<gov.anzong.androidnga.mvvm.model.entity.RecentReplyInfo>) { replies += data.size }
        })
        task.queryNotification(object : OnHttpCallBack<List<gov.anzong.androidnga.mvvm.model.entity.NotificationInfo>> {
            override fun onSuccess(data: List<gov.anzong.androidnga.mvvm.model.entity.NotificationInfo>) { all += data.size }
        })
        assertEquals(listOf(1), replies)
        assertEquals(listOf(2), all)
        assertEquals(
            listOf("GET https://bbs.nga.cn/nuke.php?__lib=noti&__output=8&__act=get_all", "GET https://bbs.nga.cn/nuke.php?__lib=noti&__output=8&__act=get_all"),
            api.requests,
        )
    }

    private fun json(body: String) = Reply(body, contentType = "application/json; charset=utf-8")

    @Test fun likeBookmarkAndReportReportForumResultOrReason() {
        val forum = FakeForum(
            json("{\"data\":{\"0\":\"操作成功\",\"1\":1}}"),
            json("{\"error\":{\"0\":\"你在217秒后方可举报\"},\"time\":1}"),
            json("{\"data\":{\"0\":\"已加入收藏\"}}"),
            Reply("<html>无法解析</html>", contentType = "text/html; charset=utf-8"),
        )
        val client = { ForumActionClient(forum.writer()) }
        val recorder = Recorder<String>()
        LikeTask(client).execute(12, 34, LikeTask.OPPOSE, recorder)
        ReportTask(client).execute(12, 34, "原因", recorder)
        BookmarkTask(client).execute("12", "34", recorder)
        BookmarkTask(client).execute("12", null, recorder)
        assertEquals(listOf("success:操作成功", "error:你在217秒后方可举报", "success:已加入收藏", "error:" + NgaWriteClient.AMBIGUOUS),
            recorder.events)
        assertEquals(listOf("topic_recommend", "log_post", "topic_favor", "topic_favor"), forum.bodies.map { form(it)["__lib"] })
        assertEquals("-1", form(forum.bodies[0])["value"])
        assertEquals(null, form(forum.bodies[3])["pid"])
        val report = forum.requests[1]
        assertEquals("/nuke.php", report.url.encodedPath)
        assertEquals("UTF8", report.url.queryParameter("__inchst"))
        assertEquals("https://bbs.nga.cn/read.php?tid=12", report.header("Referer"))
        assertEquals(NgaWriteClient.USER_AGENT, report.header("User-Agent"))
        assertEquals("原因", form(forum.bodies[1])["info"])
        assertEquals("secret-token", form(forum.bodies[1])["access_token"])
    }

    @Test fun signatureSubmitsOnWorkerAndThrowsForumReason() = runTest(dispatcher) {
        val forum = FakeForum(json("{\"data\":{\"0\":\"操作成功\"}}"), json("{\"error\":{\"0\":\"签名过长\"}}"))
        val task = SignPostTask { ForumActionClient(forum.writer()) }
        val param = SignPostParam().apply { uid = "9"; sign = "签名 a" }
        assertEquals("操作成功", task.submit(param))
        try {
            task.submit(param)
            fail("应当失败")
        } catch (e: NgaWriteClient.PostException) {
            assertEquals("签名过长", e.message)
        }
        val fields = form(forum.bodies.first())
        assertEquals("set_sign", fields["__lib"])
        assertEquals("9", fields["uid"])
        assertEquals("签名 a", fields["sign"])
    }

    @Test fun clearNotificationsGoesThroughWriteClient() {
        val forum = FakeForum(json("{\"data\":{\"0\":\"操作成功\"}}"))
        ForumNotificationTask(null, { FakeNgaApi() }, { "https://bbs.nga.cn/" }, { ForumActionClient(forum.writer()) }).clearAllNotification()
        val fields = form(forum.bodies.single())
        assertEquals(listOf("noti", "del"), listOf(fields["__lib"], fields["__act"]))
        assertEquals("123", fields["access_uid"])
    }
}
