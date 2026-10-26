package sp.phone.view.webview;

import static org.junit.Assert.*;

import org.junit.Test;

public class ForumLinkTest {
    private ForumLink resolve(String url) {
        return ForumLink.resolve(url, "about:blank", "https://bbs.nga.cn/");
    }

    @Test
    public void forumAliasesAndRelativeTopicLinksStayNative() {
        for (String url :
                new String[] {
                    "https://nga.178.com/read.php?tid=12",
                    "//ngabbs.com/read.php?pid=45",
                    "/read.php?tid=12&page=3#pid45",
                    "read.php?tid=12",
                    "bbs.ngacn.cc/read.php?tid=12",
                    "https://BBS.NGA.CN:443/read.php?tid=12"
                }) {
            assertEquals(url, ForumLink.Target.ARTICLE, resolve(url).target);
        }
    }

    @Test
    public void queryOnlyLinkPreservesCurrentTopicPath() {
        ForumLink link =
                ForumLink.resolve(
                        "?tid=12&page=4",
                        "https://ngabbs.com/read.php?tid=12",
                        "https://bbs.nga.cn/");
        assertEquals(ForumLink.Target.ARTICLE, link.target);
        assertEquals("4", link.url.queryParameter("page"));
    }

    @Test
    public void encodedParametersAndFragmentsArePreserved() {
        ForumLink link = resolve("/read.php?tid=%31%32&pid=34&page=2&authorid=56#pid34");
        assertEquals("12", link.url.queryParameter("tid"));
        assertEquals("34", link.url.queryParameter("pid"));
        assertEquals("56", link.url.queryParameter("authorid"));
        assertEquals("pid34", link.url.fragment());
    }

    @Test
    public void hostSpoofingDoesNotBecomeNativeNavigation() {
        assertEquals(
                ForumLink.Target.EXTERNAL,
                resolve("https://bbs.nga.cn.example.org/read.php?tid=12").target);
        assertEquals(
                ForumLink.Target.EXTERNAL,
                resolve("https://example.org/?url=bbs.nga.cn/read.php?tid=12").target);
        assertNull(resolve("https://bbs.nga.cn@example.org/read.php?tid=12"));
    }

    @Test
    public void nonWebSchemesAndAnchorsAreNotDispatched() {
        for (String url :
                new String[] {
                    "javascript:alert(1)",
                    "intent://read.php?tid=1",
                    "file:///read.php?tid=1",
                    "#pid12"
                }) {
            assertNull(url, resolve(url));
        }
    }

    @Test
    public void forumProfileAndImageLinksHaveDedicatedDestinations() {
        assertEquals(ForumLink.Target.FORUM, resolve("/thread.php?fid=7").target);
        assertEquals(
                ForumLink.Target.PROFILE,
                resolve("/nuke.php?username=%E6%B5%8B%E8%AF%95&func=ucp").target);
        assertEquals(
                ForumLink.Target.IMAGE,
                resolve("https://img.example.org/PHOTO.JPG?token=abc").target);
    }
}
