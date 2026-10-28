package gov.anzong.androidnga.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import sp.phone.mvp.model.convert.MessageConvertFactory

/** 短消息详情解析；合成响应，不访问论坛。MessageUtil 依赖 Android 资源，放在设备测试中。 */
class MessageDetailParseTest {

    private fun parse(js: String) = MessageConvertFactory().getMessageDetailInfo(js, 1)

    private val users = """"userInfo":{"100":{"uid":100,"username":"用户甲"},"200":{"uid":200,"username":"用户乙"},"__GROUPS":{}}"""

    @Test fun currentLayoutReadsMessagesFromAllmsgs() {
        val js = """window.script_muti_get_var_store={"data":{"0":{"allmsgs":{
            "0":{"subject":"标题","content":"第一条[img]./mon_202610/26/a.png[/img]","from":100,"time":1700000000,"id":1},
            "1":{"subject":"","content":"第二条","from":200,"time":1700000100,"id":2}},
            "length":2,"nextPage":"2","subjectBit":0,$users,"starterUid":"100","allUsers":"100	用户甲	200	用户乙","currentPage":1}}}"""
        val info = parse(js)
        assertNotNull(info)
        info!!
        assertEquals(listOf("第一条", "第二条"), info.messageEntryList.map { it.content.substringBefore("[img]") })
        assertEquals(listOf("用户甲", "用户乙"), info.messageEntryList.map { it.author })
        assertEquals("标题", info._Title)
        assertEquals("用户甲,用户乙", info._Alluser)
        assertEquals(2, info.__nextPage)
        assertTrue(info.messageEntryList[0].content.contains("https://img.nga.cn/attachments/mon_202610/26/a.png"))
    }

    @Test fun legacyLayoutStillParses() {
        val js = """{"data":{"0":{"0":{"subject":"旧标题","content":"旧内容","from":100,"time":1700000000},
            $users,"allUsers":"100	用户甲","currentPage":1,"nextPage":0}}}"""
        val info = parse(js)!!
        assertEquals(listOf("旧内容"), info.messageEntryList.map { it.content })
        assertEquals("旧标题", info._Title)
        assertEquals(0, info.__nextPage)
    }

    @Test fun conversationWithoutMessagesDoesNotCrash() {
        val js = """{"data":{"0":{"allmsgs":{},$users,"allUsers":"","currentPage":1,"nextPage":""}}}"""
        val info = parse(js)!!
        assertTrue(info.messageEntryList.isEmpty())
    }
}
