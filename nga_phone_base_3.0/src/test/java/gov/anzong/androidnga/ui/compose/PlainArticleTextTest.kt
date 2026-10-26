package gov.anzong.androidnga.ui.compose

import org.junit.Assert.*
import org.junit.Test

class PlainArticleTextTest {
    @Test fun simpleTextAndLineBreaksUseNativeRendering() {
        listOf("普通回复", "正文<br/>第二行", "<p>第一段</p><p>第二段</p>", "A &amp; B", "表情 😀").forEach {
            assertTrue(it, canRenderPlainArticle(it))
        }
    }
    @Test fun interactiveAndFormattedContentKeepsRichRenderer() {
        listOf("[img]./mon_test/a.webp[/img]", "[b]加粗[/b]", "[quote]引用[/quote]", "[s:ac:哭笑]",
            "<img src='a.jpg'>", "<a href='/read.php?tid=1'>链接</a>", "https://example.org/a", "<span style='color:red'>彩色</span>").forEach {
            assertFalse(it, canRenderPlainArticle(it))
        }
    }
}
