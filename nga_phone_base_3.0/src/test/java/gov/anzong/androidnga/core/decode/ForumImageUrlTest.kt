package gov.anzong.androidnga.core.decode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumImageUrlTest {

    @Test
    fun forumAttachmentFormsUseCurrentHttpsHost() {
        val path = "/attachments/mon_202610/26/sample.webp"
        for (input in listOf(
            "./mon_202610/26/sample.webp",
            "mon_202610/26/sample.webp",
            path,
            "http://img.nga.178.com$path",
            "https://img6.nga.178.com$path",
            "//img.nga.cn$path",
            "https://img.ngacn.cc$path",
        )) {
            assertEquals("https://img.nga.cn$path", ForumImageUrl.normalize(input))
        }
    }

    @Test
    fun externalHostsArePreservedAndUnsafeOrSpoofedAddressesAreNotPromoted() {
        assertEquals("https://example.org/a.jpg?q=1&b=2", ForumImageUrl.normalize("https://example.org/a.jpg?q=1&amp;b=2"))
        assertEquals(
            "https://img.nga.178.com.example.org/a.jpg",
            ForumImageUrl.normalize("https://img.nga.178.com.example.org/a.jpg"),
        )
        assertNull(ForumImageUrl.normalize("javascript:bad()"))
        assertNull(ForumImageUrl.normalize("https://name:secret@img.nga.cn/a.jpg"))
    }

    @Test
    fun thumbnailGalleryUsesOriginalWithoutChangingQuery() {
        assertEquals("https://img.nga.cn/a.webp?q=1", ForumImageUrl.original("https://img.nga.cn/a.webp.thumb.jpg?q=1"))
        assertEquals("https://img.nga.cn/a.jpg", ForumImageUrl.original("https://img.nga.cn/a.jpg"))
    }

    @Test
    fun deadStaticHostsMoveToWorkingHost() {
        val path = "/ngabbs/post/smile/pt00.png"
        for (input in listOf(
            "http://img4.nga.178.com$path",
            "https://img4.nga.178.com$path",
            "http://img4.ngacn.cc$path",
            "http://img4.nga.cn$path",
        )) {
            assertEquals("https://img4.nga.cn$path", ForumImageUrl.normalize(input))
        }
    }

    @Test
    fun forumPathOnlyForForumImageHosts() {
        assertEquals("/attachments/mon_1/a.png", ForumImageUrl.forumPath("http://img6.nga.178.com/attachments/mon_1/a.png"))
        assertEquals("/ngabbs/post/smile/pt00.png", ForumImageUrl.forumPath("https://img4.nga.cn/ngabbs/post/smile/pt00.png"))
        assertNull(ForumImageUrl.forumPath("https://example.org/attachments/mon_1/a.png"))
        assertNull(ForumImageUrl.forumPath("https://img.nga.178.com.example.org/a.png"))
        assertNull(ForumImageUrl.forumPath(null))
    }

    @Test
    fun embeddedEmoticonsUseBundledImagesWhateverTheHost() {
        val decoder = ForumImageDecoder()
        for (url in listOf(
            "http://img4.nga.178.com/ngabbs/post/smile/pt00.png",
            "https://img4.nga.cn/ngabbs/post/smile/pt00.png",
            "http://img.nga.178.com/attachments/mon_201209/14/-47218_5052bc4cc6331.png",
            "./mon_201209/14/-47218_5052bc4cc6331.png",
        )) {
            val html = decoder.decode("[img]$url[/img]")
            assertTrue("$url -> $html", html.contains("file:///android_asset/"))
            assertFalse(html.contains("nga.178.com"))
        }
    }

    @Test
    fun mediaAndImagesNeverPointAtDeadHosts() {
        val html = ForumImageDecoder().decode("[img]http://img6.nga.178.com/attachments/mon_202610/26/a.jpg[/img]")
        assertTrue(html, html.contains("https://img.nga.cn/attachments/mon_202610/26/a.jpg"))
        assertFalse(html.contains("nga.178.com"))
    }
}
