package gov.anzong.androidnga.ui.compose

import org.junit.Assert.*
import org.junit.Test
import gov.anzong.androidnga.network.bean.ThreadRowInfo

class ArticleEntriesTest {
    private fun row(floor: Int) = ThreadRowInfo().apply { tid=12; lou=floor; pid=if(floor==0) 0 else 100+floor }
    @Test fun hotSectionFollowsOpeningPostAndKeepsNormalReplies() {
        val entries=articleEntries(listOf(row(0),row(1),row(15)), listOf(row(15),row(15)))
        assertEquals(listOf("floor:12:0:0","hot_header","hot:12:115:15","floor:12:101:1","floor:12:115:15"),entries.map{it.key})
        assertEquals(4,entries.indexOfFirst{!it.hot && it.row?.lou==15})
    }
    @Test fun pagesWithoutOpeningPostAndTopicsWithoutHotRepliesHaveNoSection() {
        assertEquals(1,articleEntries(listOf(row(20)),listOf(row(15))).size)
        assertEquals(2,articleEntries(listOf(row(0),row(1)),emptyList()).size)
    }
}
