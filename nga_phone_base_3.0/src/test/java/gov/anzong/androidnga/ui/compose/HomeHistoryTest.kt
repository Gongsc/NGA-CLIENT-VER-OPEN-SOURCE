package gov.anzong.androidnga.ui.compose

import com.alibaba.fastjson.JSON
import org.junit.Assert.*
import org.junit.Test
import sp.phone.mvp.model.entity.ThreadPageInfo
import java.time.LocalDate
import java.time.ZoneId

class HomeHistoryTest {
    @Test fun oldHistoryHasNoInventedVisitDateAndKeepsResumePage() {
        val old = JSON.parseObject("{\"tid\":42,\"page\":3,\"subject\":\"离线主题\"}", ThreadPageInfo::class.java)
        assertEquals("更早", historyDay(old.historyVisitedAt))
        assertEquals(3, old.page)
    }

    @Test fun visitDateSurvivesSerializationAndGroupsByLocalDay() {
        val today = LocalDate.of(2026, 10, 27)
        fun stamp(day: LocalDate) = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val topic = ThreadPageInfo().apply { tid = 42; historyVisitedAt = stamp(today); page = 4 }
        val restored = JSON.parseObject(JSON.toJSONString(topic), ThreadPageInfo::class.java)
        assertEquals("今天", historyDay(restored.historyVisitedAt, today))
        assertEquals("昨天", historyDay(stamp(today.minusDays(1)), today))
        assertEquals("2026-10-25", historyDay(stamp(today.minusDays(2)), today))
        assertEquals(4, restored.page)
    }
}
