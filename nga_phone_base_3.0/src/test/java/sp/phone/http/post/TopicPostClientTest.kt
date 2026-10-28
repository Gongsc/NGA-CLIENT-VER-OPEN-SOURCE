package sp.phone.http.post

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import sp.phone.param.PostParam
import java.io.IOException
import java.net.URLDecoder

/** 合成响应，不访问真实论坛。 */
class TopicPostClientTest {

    private class Reply(val body: String, val code: Int = 200, val contentType: String = "application/xml; charset=utf-8")

    private class FakeForum(vararg replies: Reply) : Interceptor {
        private val pending = ArrayDeque(replies.toList())
        val requests = mutableListOf<Request>()
        val bodies = mutableListOf<String>()
        var failNext = false

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            requests += request
            bodies += Buffer().also { request.body?.writeTo(it) }.readUtf8()
            if (failNext) throw IOException("connection reset")
            val reply = pending.removeFirst()
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(reply.code)
                .message("OK")
                .body(reply.body.toByteArray(charset(if (reply.contentType.contains("gbk")) "GBK" else "UTF-8"))
                    .toResponseBody(reply.contentType.toMediaType()))
                .build()
        }
    }

    private fun client(forum: FakeForum, loggedIn: Boolean = true) = TopicPostClient(
        OkHttpClient.Builder().addInterceptor(forum).build(),
        "https://bbs.nga.cn/",
    ) { if (loggedIn) TopicPostClient.Credentials("123", "secret-token") else null }

    private fun form(body: String): Map<String, String> = body.split("&").filter { it.isNotEmpty() }.associate {
        val (name, value) = it.split("=", limit = 2)
        name to URLDecoder.decode(value, "UTF-8")
    }

    private fun reply(content: String = "测试回复") = PostParam("47239186", "", content).apply {
        postAction = "reply"
        postFid = -7
        postPid = "876078281"
    }

    private val preflight = Reply("""<?xml version="1.0" encoding="UTF-8"?>
        <root><content></content><auth>reply-token</auth><attach_url>/nuke.php?__lib=upload</attach_url></root>""")

    private val posted = Reply("""<?xml version="1.0" encoding="UTF-8"?>
        <root><__MESSAGE><item>0</item><item>发帖完毕</item><item>200</item></__MESSAGE></root>""")

    private fun message(text: String) = Reply("<root><__MESSAGE><item>1</item><item>$text</item><item>403</item></__MESSAGE></root>")

    private fun assertRejected(expected: String, block: () -> Unit) {
        try {
            block()
            fail("应当失败：$expected")
        } catch (e: TopicPostClient.PostException) {
            assertEquals(expected, e.message)
        }
    }

    @Test fun replyPreflightsThenSubmitsWithAuthAndCredentials() {
        val forum = FakeForum(preflight, posted)
        val param = reply()

        assertEquals("发帖完毕", client(forum).submit(param))

        assertEquals(2, forum.requests.size)
        forum.requests.forEach { request ->
            assertEquals("POST", request.method)
            assertEquals("/post.php", request.url.encodedPath)
            assertEquals("reply", request.url.queryParameter("action"))
            assertEquals("47239186", request.url.queryParameter("tid"))
            assertEquals("876078281", request.url.queryParameter("pid"))
            assertEquals("xml", request.url.queryParameter("lite"))
            assertEquals("UTF8", request.url.queryParameter("__inchst"))
            // 回复由 tid 确定版面，不提交缺省的 -7
            assertNull(request.url.queryParameter("fid"))
            assertEquals(TopicPostClient.USER_AGENT, request.header("User-Agent"))
            assertEquals(TopicPostClient.USER_AGENT, request.header("X-User-Agent"))
            assertEquals("ngaPassportUid=123; ngaPassportCid=secret-token", request.header("Cookie"))
            assertEquals("application/x-www-form-urlencoded; charset=UTF-8", request.body?.contentType().toString())
        }
        val preflightForm = form(forum.bodies[0])
        assertEquals(mapOf("access_uid" to "123", "access_token" to "secret-token"), preflightForm)

        val submitForm = form(forum.bodies[1])
        assertEquals("reply-token", submitForm["auth"])
        assertEquals("2", submitForm["step"])
        assertEquals("测试回复", submitForm["post_content"])
        assertEquals("876078281", submitForm["pid"])
        assertEquals("123", submitForm["access_uid"])
        assertEquals("secret-token", submitForm["access_token"])
        assertFalse(submitForm.containsKey("post_subject"))
        assertFalse(submitForm.containsKey("anony"))
        assertTrue("正文按 UTF-8 编码", forum.bodies[1].contains("post_content=%E6%B5%8B%E8%AF%95%E5%9B%9E%E5%A4%8D"))
        assertEquals(forum.requests[1].url.toString(), forum.requests[1].header("Referer"))
        assertEquals("reply-token", param.authCode)
    }

    @Test fun existingAuthSkipsPreflight() {
        val forum = FakeForum(posted)
        val param = reply().apply { authCode = "cached-token" }

        client(forum).submit(param)

        assertEquals(1, forum.requests.size)
        assertEquals("cached-token", form(forum.bodies[0])["auth"])
    }

    @Test fun newTopicSendsForumSubjectAnonymityAndAttachments() {
        val forum = FakeForum(posted)
        val param = PostParam(null, "标题 & 符号", "正文").apply {
            postAction = "new"
            postFid = -7
            stid = "456"
            authCode = "new-token"
            isAnonymous = true
            appendAttachment("mon_202610/27/abc.jpg", "check1")
        }

        client(forum).submit(param)

        val url = forum.requests[0].url
        assertEquals("new", url.queryParameter("action"))
        assertEquals("-7", url.queryParameter("fid"))
        assertEquals("456", url.queryParameter("stid"))
        assertNull(url.queryParameter("tid"))
        val submitForm = form(forum.bodies[0])
        assertEquals("标题 & 符号", submitForm["post_subject"])
        assertEquals("1", submitForm["anony"])
        // 附件字段已编码，原样提交，不再二次编码
        assertTrue(forum.bodies[0].contains("attachments=%09mon_202610/27/abc.jpg"))
        assertTrue(forum.bodies[0].contains("attachments_check=%09check1"))
    }

    @Test fun preflightRejectionStopsBeforeSubmitting() {
        val forum = FakeForum(message("你没有权限在此版面回复"))

        assertRejected("你没有权限在此版面回复") { client(forum).submit(reply()) }
        assertEquals(1, forum.requests.size)
    }

    @Test fun preflightWithoutFormIsAnError() {
        assertRejected("无法读取发帖页面，请稍后重试") { client(FakeForum(Reply("<html>维护中</html>"))).prepare(reply()) }
    }

    @Test fun submitRejectionShowsForumMessage() {
        val forum = FakeForum(preflight, message("发帖间隔过短，请稍后再试"))

        assertRejected("发帖间隔过短，请稍后再试") { client(forum).submit(reply()) }
    }

    @Test fun loginMessageIsMarkedAsLoginRequired() {
        try {
            client(FakeForum(message("你必须先登录论坛"))).prepare(reply())
            fail()
        } catch (e: TopicPostClient.PostException) {
            assertTrue(e.requiresLogin)
        }
    }

    @Test fun unauthorizedStatusRequiresLogin() {
        assertRejected(TopicPostClient.LOGIN_REQUIRED) { client(FakeForum(Reply("", code = 401))).prepare(reply()) }
    }

    @Test fun forbiddenJsonErrorShowsItsMessage() {
        val locked = Reply("""{"error":["11:此帖子被锁定"],"data":{"__MESSAGE":[11,"此帖子被锁定",null,403]}}""",
            code = 403, contentType = "application/json; charset=utf-8")
        assertRejected("此帖子被锁定") { client(FakeForum(locked)).prepare(reply()) }
    }

    @Test fun mentionLimitStillCountsAsPosted() {
        val forum = FakeForum(preflight, message("@提醒每24小时不能超过50个"))

        assertEquals("@提醒每24小时不能超过50个", client(forum).submit(reply()))
    }

    @Test fun pidInResultCountsAsPosted() {
        val forum = FakeForum(preflight, Reply("<root><pid>99</pid></root>"))

        assertEquals("发帖完毕", client(forum).submit(reply()))
    }

    @Test fun unrecognizedResultIsNotTreatedAsSuccess() {
        val forum = FakeForum(preflight, Reply("<root></root>"))

        assertRejected(TopicPostClient.AMBIGUOUS) { client(forum).submit(reply()) }
    }

    @Test fun networkErrorWhileSubmittingIsAmbiguous() {
        val forum = FakeForum(preflight)
        val param = reply()
        client(forum).prepare(param).also { param.authCode = it }
        forum.failNext = true

        assertRejected(TopicPostClient.AMBIGUOUS_NETWORK) { client(forum).submit(param) }
    }

    @Test fun gbkResponseIsDecoded() {
        val forum = FakeForum(preflight, Reply("<root><__MESSAGE><item>0</item><item>发帖完毕 返回主题</item></__MESSAGE></root>",
            contentType = "text/xml; charset=gbk"))

        assertEquals("发帖完毕 返回主题", client(forum).submit(reply()))
    }

    @Test fun loggedOutRequestsCarryNoCredentials() {
        val forum = FakeForum(preflight)

        client(forum, loggedIn = false).prepare(reply())

        assertNull(forum.requests[0].header("Cookie"))
        assertEquals("", forum.bodies[0])
    }
}
