package gov.anzong.androidnga.ui.dialog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumUrlTargetTest {
    private val host = "https://bbs.nga.cn/"

    @Test fun boardAddressesKeepFidOrStid() {
        assertEquals(UrlTarget.Board("https://bbs.nga.cn/thread.php?fid=-7"), forumUrlTarget(" https://ngabbs.com/thread.php?fid=-7&page=2 ", host))
        assertEquals(UrlTarget.Board("https://bbs.nga.cn/thread.php?stid=123"), forumUrlTarget("http://nga.178.com/thread.php?stid=123", host))
    }

    @Test fun topicAddressesNormalizeTidAndPid() {
        assertEquals(UrlTarget.Topic("https://bbs.nga.cn/read.php?tid=42"), forumUrlTarget("https://bbs.nga.cn/read.php?tid=42&page=3", host))
        assertEquals(UrlTarget.Topic("https://bbs.nga.cn/read.php?pid=9&tid=42"), forumUrlTarget("https://bbs.nga.cn/read.php?tid=42&pid=9", host))
        assertEquals(UrlTarget.Topic("https://bbs.nga.cn/read.php?pid=9&tid=42"), forumUrlTarget("https://bbs.nga.cn/read.php?pid=9&tid=42", host))
        assertEquals(UrlTarget.Topic("https://bbs.nga.cn/read.php?pid=9"), forumUrlTarget("https://bbs.nga.cn/read.php?pid=9", host))
    }

    @Test fun otherInputIsRejectedWithReason() {
        assertEquals(UrlTarget.Invalid("请输入URL地址"), forumUrlTarget("  ", host))
        assertTrue(forumUrlTarget("https://example.com/", host) is UrlTarget.Invalid)
    }
}
