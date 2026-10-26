package gov.anzong.androidnga.core.decode;

import static org.junit.Assert.*;

import org.junit.Test;

public class ForumImageUrlTest {
    @Test
    public void forumAttachmentFormsUseCurrentHttpsHost() {
        String path = "/attachments/mon_202610/26/sample.webp";
        for (String input :
                new String[] {
                    "./mon_202610/26/sample.webp",
                    "mon_202610/26/sample.webp",
                    path,
                    "http://img.nga.178.com" + path,
                    "https://img6.nga.178.com" + path,
                    "//img.nga.cn" + path,
                    "https://img.ngacn.cc" + path
                }) assertEquals("https://img.nga.cn" + path, ForumImageUrl.normalize(input));
    }

    @Test
    public void externalHostsArePreservedAndUnsafeOrSpoofedAddressesAreNotPromoted() {
        assertEquals(
                "https://example.org/a.jpg?q=1&b=2",
                ForumImageUrl.normalize("https://example.org/a.jpg?q=1&amp;b=2"));
        assertEquals(
                "https://img.nga.178.com.example.org/a.jpg",
                ForumImageUrl.normalize("https://img.nga.178.com.example.org/a.jpg"));
        assertNull(ForumImageUrl.normalize("javascript:bad()"));
        assertNull(ForumImageUrl.normalize("https://name:secret@img.nga.cn/a.jpg"));
    }

    @Test
    public void thumbnailGalleryUsesOriginalWithoutChangingQuery() {
        assertEquals(
                "https://img.nga.cn/a.webp?q=1",
                ForumImageUrl.original("https://img.nga.cn/a.webp.thumb.jpg?q=1"));
        assertEquals(
                "https://img.nga.cn/a.jpg", ForumImageUrl.original("https://img.nga.cn/a.jpg"));
    }
}
