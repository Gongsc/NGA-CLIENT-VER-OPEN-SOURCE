package sp.phone.http.post

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import sp.phone.mvp.model.entity.SubBoard
import sp.phone.param.MessagePostParam

/** 短消息、投票、头像与 nuke.php 结果解析；合成响应，不访问真实论坛。 */
class ForumWriteClientsTest {

    private fun json(body: String, code: Int = 200) = Reply(body, code, "application/json; charset=utf-8")

    private fun assertRejected(expected: String, block: () -> Unit) {
        try {
            block()
            fail("应当失败：$expected")
        } catch (e: NgaWriteClient.PostException) {
            assertEquals(expected, e.message)
        }
    }

    private fun replyParam() = MessagePostParam("reply", 9876, "").apply { postContent = "短消息回复" }

    @Test fun messageReplyFollowsSngaProtocol() {
        val forum = FakeForum(json("""{"data":{"0":"发送完毕 ..."},"time":1}"""))

        assertEquals("发送完毕 ...", MessagePostClient(forum.writer()).send(replyParam()))

        val request = forum.requests.single()
        assertEquals("POST", request.method)
        assertEquals("/nuke.php", request.url.encodedPath)
        assertEquals("message", request.url.queryParameter("__lib"))
        assertEquals("message", request.url.queryParameter("__act"))
        assertEquals("reply", request.url.queryParameter("act"))
        assertEquals("11", request.url.queryParameter("__output"))
        assertEquals("UTF8", request.url.queryParameter("__inchst"))
        assertEquals(NgaWriteClient.USER_AGENT, request.header("User-Agent"))
        val fields = form(forum.bodies.single())
        assertEquals("9876", fields["mid"])
        assertEquals("短消息回复", fields["content"])
        assertEquals("123", fields["access_uid"])
        assertEquals("secret-token", fields["access_token"])
        assertFalse(fields.containsKey("to"))
        assertFalse(fields.containsKey("subject"))
        assertTrue("正文按 UTF-8 编码", forum.bodies.single().contains("content=%E7%9F%AD%E6%B6%88%E6%81%AF"))
    }

    @Test fun newMessageSendsRecipientsAndSubject() {
        val forum = FakeForum(json("""{"data":["发送完毕 ..."]}"""))
        val param = MessagePostParam("new", 0, "").apply {
            recipient = "用户甲，用户乙"
            postSubject = "标题"
            postContent = "正文"
        }

        MessagePostClient(forum.writer()).send(param)

        assertEquals("new", forum.requests.single().url.queryParameter("act"))
        val fields = form(forum.bodies.single())
        assertEquals("用户甲,用户乙", fields["to"])
        assertEquals("标题", fields["subject"])
        assertFalse(fields.containsKey("mid"))
    }

    @Test fun messageErrorShowsForumReason() {
        val forum = FakeForum(json("""{"error":{"0":"收件人不存在"},"time":1}"""))

        assertRejected("收件人不存在") { MessagePostClient(forum.writer()).send(replyParam()) }
    }

    @Test fun legacyScriptPrefixedResultIsParsed() {
        assertEquals("操作成功 你可能需要重新登录以显示新的头像",
            NgaWriteClient.parseAction("window.script_muti_get_var_store={\"data\":{\"0\":\"操作成功 你可能需要重新登录以显示新的头像\"}}"))
    }

    @Test fun nonZeroCodeIsFailureAndLoginCodeRequiresLogin() {
        try {
            NgaWriteClient.parseAction("""{"code":5,"msg":"请先登录"}""")
            fail()
        } catch (e: NgaWriteClient.PostException) {
            assertEquals("请先登录", e.message)
            assertTrue(e.requiresLogin)
        }
        assertEquals("ok", NgaWriteClient.parseAction("""{"code":0,"msg":"ok"}"""))
    }

    @Test fun failureWordsInDataAreFailures() {
        assertRejected("操作失败 权限不足") { NgaWriteClient.parseAction("""{"data":["操作失败 权限不足"]}""") }
    }

    @Test fun mentionLimitErrorStillCountsAsSent() {
        assertEquals("@提醒每24小时不能超过50个", NgaWriteClient.parseAction("""{"error":["@提醒每24小时不能超过50个"]}"""))
    }

    @Test fun unrecognizedActionResultIsAmbiguous() {
        assertRejected(NgaWriteClient.AMBIGUOUS) { NgaWriteClient.parseAction("<html></html>") }
    }

    @Test fun pollVoteFollowsSngaProtocol() {
        val forum = FakeForum(json("""{"data":["投票成功"]}"""))

        val result = ForumActionClient(forum.writer()).vote("__lib=vote&raw=3&lite=js&__act=vote&tid=123&voteid=4,5")

        assertTrue(result.success)
        assertEquals("投票成功", result.message)
        val request = forum.requests.single()
        assertEquals("/nuke.php", request.url.encodedPath)
        assertEquals("https://bbs.nga.cn/read.php?tid=123", request.header("Referer"))
        val fields = form(forum.bodies.single())
        assertEquals("vote", fields["__lib"])
        assertEquals("vote", fields["__act"])
        assertEquals("123", fields["tid"])
        assertEquals("4,5", fields["voteid"])
        assertEquals("3", fields["raw"])
        assertNull("投票不再使用 lite=js", fields["lite"])
    }

    @Test fun pollSettleIsAccepted() {
        val forum = FakeForum(json("""{"data":["操作成功"]}"""))

        assertTrue(ForumActionClient(forum.writer()).vote("__lib=vote&raw=3&lite=js&__act=settle&tid=123&voteid=7").success)
        assertEquals("settle", form(forum.bodies.single())["__act"])
    }

    @Test fun pageScriptCannotSubmitOtherActions() {
        val forum = FakeForum()
        val client = ForumActionClient(forum.writer())

        listOf(
            "__lib=message&__act=message&act=new&to=x&content=y",
            "__lib=vote&__act=delete&tid=1&voteid=1",
            "__lib=vote&__act=vote&tid=abc&voteid=1",
            "__lib=vote&__act=vote&tid=1&voteid=1;drop",
            "__lib=vote&__act=vote&tid=1&voteid=%ZZ",
        ).forEach { query ->
            val result = client.vote(query)
            assertFalse(query, result.success)
            assertEquals("不支持的操作", result.message)
        }
        assertTrue("被拒绝的参数不发起请求", forum.requests.isEmpty())
    }

    @Test fun avatarUsesCurrentDomainOverHttps() {
        val forum = FakeForum(Reply("window.script_muti_get_var_store={\"data\":{\"0\":\"操作成功 你可能需要重新登录以显示新的头像\"}}",
            contentType = "text/html; charset=gbk"))

        val result = ForumActionClient(forum.writer()).setAvatar("https://img.nga.cn/avatar/1.jpg")

        assertTrue(result.success)
        val request = forum.requests.single()
        assertEquals("https://bbs.nga.cn/nuke.php?__inchst=UTF8", request.url.toString())
        val fields = form(forum.bodies.single())
        assertEquals("avatar", fields["func"])
        assertEquals("https://img.nga.cn/avatar/1.jpg", fields["icon"])
        assertEquals("js", fields["lite"])
        assertFalse(forum.bodies.single().contains("__ngaClientChecksum"))
    }

    @Test fun avatarFailureIsReportedNotTreatedAsSuccess() {
        val forum = FakeForum(json("""{"error":{"0":"头像地址不合法"}}"""))

        val result = ForumActionClient(forum.writer()).setAvatar("https://example.com/a.jpg")

        assertFalse(result.success)
        assertEquals("头像地址不合法", result.message)
    }

    @Test fun networkFailureAfterSendingIsAmbiguous() {
        val forum = FakeForum().apply { failNext = true }

        val result = ForumActionClient(forum.writer()).setAvatar("https://img.nga.cn/a.jpg")

        assertFalse(result.success)
        assertEquals(NgaWriteClient.AMBIGUOUS_NETWORK, result.message)
    }

    @Test fun subBoardSwitchPostsUserOptionFormToCurrentHost() {
        val forum = FakeForum(json("""{"data":{"0":"操作成功"}}"""), json("""{"data":{"0":"操作成功"}}"""))
        val client = ForumActionClient(forum.writer())
        val board = SubBoard().apply { type = 0; fid = 7; parentFidStr = "-7" }
        val inverted = SubBoard().apply { type = 1; tidStr = "42"; parentFidStr = "-7" }

        assertEquals("已屏蔽子版块，刷新后生效", client.setSubBoardVisible(board, false))
        assertEquals("已取消屏蔽，刷新后生效", client.setSubBoardVisible(inverted, true))

        val request = forum.requests.first()
        assertEquals("POST", request.method)
        assertEquals("bbs.nga.cn", request.url.host)
        assertEquals("/nuke.php", request.url.encodedPath)
        assertEquals("UTF8", request.url.queryParameter("__inchst"))
        assertEquals("https://bbs.nga.cn/thread.php?fid=-7", request.header("Referer"))
        assertEquals(NgaWriteClient.USER_AGENT, request.header("User-Agent"))
        val fields = form(forum.bodies[0])
        assertEquals("user_option", fields["__lib"])
        assertEquals("set", fields["__act"])
        assertEquals("0", fields["type"])
        assertEquals("-7", fields["fid"])
        assertEquals("7", fields["del"])
        assertFalse(fields.containsKey("add"))
        assertEquals("123", fields["access_uid"])
        assertEquals("secret-token", fields["access_token"])
        // type=1 的子版块以 tid 标识，显示时发 del。
        val second = form(forum.bodies[1])
        assertEquals("1", second["type"])
        assertEquals("42", second["del"])
    }

    @Test fun subBoardSwitchMapsActionsAndRejectsUnclearResults() {
        fun action(type: Int, visible: Boolean) =
            ForumActionClient.subBoardFields(SubBoard().apply { this.type = type; fid = 7; tidStr = "42"; parentFidStr = "-7" }, visible).last()
        assertEquals("add" to "7", action(0, true))
        assertEquals("del" to "7", action(0, false))
        assertEquals("del" to "42", action(1, true))
        assertEquals("add" to "42", action(1, false))

        val board = SubBoard().apply { fid = 7; parentFidStr = "-7" }
        assertRejected("没有权限") {
            ForumActionClient(FakeForum(json("""{"error":["没有权限"]}""")).writer()).setSubBoardVisible(board, true)
        }
        assertRejected(NgaWriteClient.AMBIGUOUS) {
            ForumActionClient(FakeForum(Reply("<html></html>", 200, "text/html")).writer()).setSubBoardVisible(board, true)
        }
    }
}
