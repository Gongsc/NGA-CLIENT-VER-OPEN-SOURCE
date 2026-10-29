package sp.phone.task

import org.junit.Assert.*
import org.junit.Test

/** 资料响应解析中的禁言、激活与状态；响应为合成数据。解析依赖 Android 类，需在设备上运行。 */
class ProfileParserTest {

    @Test fun parsedProfileWithBuffsIsNotMuted() {
        val profile = ProfileParser.parse("""window.script_muti_get_var_store={"data":{"0":{"uid":42,"username":"测试","group":"学徒",
            "bit":1048576,"muteTime":0,"buffs":{"0":{"0":105,"9":"改名卡"}},"money":123456,"fame":15,"posts":10,"regdate":0}},"time":1}""")
        assertFalse(profile.isMuted)
        assertNull(profile.mutedTime)
        assertEquals(listOf("改名卡"), profile.buffs)
        assertEquals("已激活", profile.active!!.label)
    }

    @Test fun parsedProfileWithMuteTimeIsMuted() {
        val profile = ProfileParser.parse("""{"data":{"0":{"uid":42,"username":"测试","verified":-3,"muteTime":1800000000}},"time":1}""")
        assertTrue(profile.isMuted)
        assertTrue(profile.mutedTime!!.startsWith("禁言至 "))
        assertEquals("锁定", profile.active!!.label)
    }
}
