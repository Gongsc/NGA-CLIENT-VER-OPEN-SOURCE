package sp.phone.task

import gov.anzong.androidnga.base.json.NgaJson
import org.junit.Assert.*
import org.junit.Test

/** 资料页账号状态规则，与论坛网页一致；响应均为合成数据。 */
class ProfileStatusTest {

    @Test fun bitFlagsTakePriorityOverVerified() {
        assertEquals("已激活", ProfileStatus.active(-1, 1048576L)!!.label)
        assertEquals("关联", ProfileStatus.active(null, 536870912L)!!.label)
        assertEquals("未激活", ProfileStatus.active(null, 512L)!!.label)
        assertEquals("注销", ProfileStatus.active(null, 16777216L or 1048576L)!!.label)
        assertEquals("NUKED", ProfileStatus.active(null, 4194304L or 1048576L)!!.label)
        val lock = ProfileStatus.active(null, 12582912L)!!
        assertEquals("锁定", lock.label)
        assertEquals(ProfileStatus.Level.BANNED, lock.level)
        assertTrue(ProfileStatus.active(null, 6291456L)!!.description.contains("修改密码"))
        assertTrue(ProfileStatus.active(null, 8388608L)!!.description.contains("重置密码"))
        assertTrue(ProfileStatus.active(null, 10485760L)!!.description.contains("人工解锁"))
    }

    @Test fun verifiedValuesAreActivationStatesNotMutes() {
        assertEquals("NUKED", ProfileStatus.active(-1, null)!!.label)
        for (value in listOf(-2, -3, -4, -5, -9)) assertEquals("锁定", ProfileStatus.active(value, null)!!.label)
        assertEquals("已使用激活码激活", ProfileStatus.active(1, null)!!.description)
        assertEquals("已绑定微信激活", ProfileStatus.active(5, null)!!.description)
        assertEquals("未激活", ProfileStatus.active(0, null)!!.label)
        assertNull(ProfileStatus.active(null, null))
    }

    @Test fun muteComesOnlyFromMuteTimeOrAreaMute() {
        val format = { time: Long -> "T$time" }
        assertNull(ProfileStatus.muteText(0, 0, format))
        assertEquals("禁言至 T100", ProfileStatus.muteText(100, 200, format))
        assertEquals("在某些区域禁言至 T200", ProfileStatus.muteText(0, 200, format))
    }

    @Test fun buffsBecomePlainDescriptions() {
        val buffs = NgaJson.parseObject("""{"0":{"0":105,"9":"<b>改名卡</b>效果<br/>剩余 3 天"},"1":{"0":7,"9":"  "},"2":{"0":1}}""")
        assertEquals(listOf("改名卡效果 剩余 3 天"), ProfileStatus.buffDescriptions(buffs))
        assertEquals(emptyList<String>(), ProfileStatus.buffDescriptions(null))
    }
}
