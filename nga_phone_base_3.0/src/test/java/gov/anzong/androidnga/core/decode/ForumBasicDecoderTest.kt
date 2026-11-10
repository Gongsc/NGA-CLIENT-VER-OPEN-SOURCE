package gov.anzong.androidnga.core.decode

import gov.anzong.androidnga.core.data.HtmlData
import org.junit.Assert.assertEquals
import org.junit.Test

/** 列表与折叠标签的转换：配对、嵌套与相互包含；内容均为合成。 */
class ForumBasicDecoderTest {

    private val decoder = ForumBasicDecoder()

    private fun decode(content: String): String = decoder.decode(content, HtmlData.create(content, "https://bbs.nga.cn/"))

    private fun open(title: String?): String =
        "<div><button onclick='toggleCollapse(this)'>点击显示内容${if (title == null) "" else " : $title"}" +
            "</button><div name='collapse' class='collapse' style='display:none'>"

    private val head = "<div class='quote-head'><a class='quote-user' href='https://bbs.nga.cn/nuke.php?func=ucp&username=甲乙'>甲乙</a>" +
        "<span class='quote-time'>2026-01-02 03:04</span>" +
        "<a class='quote-link' href='https://bbs.nga.cn/read.php?searchpost=1&pid=11'>原帖</a></div>"

    /** 引用头是一行标题（用户名、时间、原帖链接），头部与引用块后面的空行不再保留。 */
    @Test
    fun quoteHeaderBecomesHeadLine() {
        assertEquals(
            "<div class='quote' >${head}原文</div>回复",
            decode("[quote][pid=11,22,1]Reply[/pid] [b]Post by [uid=33]甲乙[/uid] (2026-01-02 03:04):[/b]<br/><br/>原文[/quote]<br/><br/>回复"),
        )
    }

    @Test
    fun replyToHeaderIsQuotedHeadLine() {
        assertEquals(
            "<div class='quote' >$head</div>回复",
            decode("[b]Reply to [pid=11,22,1]Reply[/pid] Post by [uid=33]甲乙[/uid] (2026-01-02 03:04)[/b]<br/>回复"),
        )
    }

    @Test
    fun topicQuoteLinksToTopic() {
        assertEquals(
            "<div class='quote' >" + head.replace("read.php?searchpost=1&pid=11", "read.php?tid=22") + "原文</div>",
            decode("[quote][tid=22]Topic[/tid] [b]Post by [uid=33]甲乙[/uid] (2026-01-02 03:04):[/b]<br/>原文[/quote]"),
        )
    }

    /** 网页版在换行处结束未闭合的粗体，后面的内容不能跟着加粗。 */
    @Test
    fun unclosedBoldEndsAtLineBreak() {
        assertEquals("<b>标题</b><br/>正文<br/>第三行", decode("[b]标题<br/>正文<br/>第三行"))
        assertEquals("<b>标题</b>\n正文", decode("[b]标题\n正文"))
        assertEquals("前文<b>末尾</b>", decode("前文[b]末尾"))
    }

    @Test
    fun pairedBoldIsUnchanged() {
        assertEquals("甲<b>乙</b>丙<br/><b>丁</b>", decode("甲[b]乙[/b]丙<br/>[B]丁[/B]"))
    }

    @Test
    fun unmatchedInlineClosersAreDropped() {
        assertEquals("甲乙<br/>丙", decode("甲[/b]乙[/color]<br/>丙[/size]"))
    }

    @Test
    fun unclosedNestedInlineTagsCloseInnermostFirst() {
        assertEquals(
            "<span style='color:red' ><b><u>甲</u></b></span><br/>乙",
            decode("[color=red][b][u]甲<br/>乙[/u][/b][/color]"),
        )
    }

    @Test
    fun listItemsDropTrailingLineBreaks() {
        assertEquals("<ul><li>甲<li>乙</ul>后文", decode("[list][*]甲<br/>[*]乙<br/>[/list]<br/>后文"))
    }

    /** 最后一项没有换行时，列表也要在 [/list] 处结束，不能吞掉其后的折叠。 */
    @Test
    fun listEndingWithoutLineBreakDoesNotSwallowFollowingCollapse() {
        assertEquals(
            "<ul><li>甲<li>乙</ul>" + open("剧透") + "秘密" + CLOSE,
            decode("[list][*]甲[*]乙[/list][collapse=剧透]秘密[/collapse]"),
        )
    }

    @Test
    fun nestedCollapsesPairInnermostFirst() {
        assertEquals(
            open("外") + "A" + open("内") + "B" + CLOSE + "C" + CLOSE,
            decode("[collapse=外]A[collapse=内]B[/collapse]C[/collapse]"),
        )
    }

    @Test
    fun nestedListsKeepOuterItems() {
        assertEquals("<ul><li>甲<ul><li>乙</ul><li>丙</ul>", decode("[list][*]甲[list][*]乙[/list][*]丙[/list]"))
    }

    @Test
    fun listInsideCollapseAndCollapseInsideListItem() {
        assertEquals(open(null) + "<ul><li>甲<li>乙</ul>" + CLOSE, decode("[collapse][list][*]甲<br/>[*]乙<br/>[/list][/collapse]"))
        assertEquals(
            "<ul><li>甲" + open("细节") + "内容" + CLOSE + "<li>乙</ul>",
            decode("[list][*]甲[collapse=细节]内容[/collapse][*]乙[/list]"),
        )
    }

    @Test
    fun collapseSpansLinesAndIgnoresCase() {
        assertEquals(open(null) + "第一行\n第二行" + CLOSE, decode("[COLLAPSE]第一行\n第二行[/Collapse]"))
    }

    /** 未配对的折叠标签按原文显示，不产生多余的 </div>；未配对的列表标签与以前一样去掉。 */
    @Test
    fun unmatchedTagsDoNotBreakStructure() {
        assertEquals("[collapse]没有结尾", decode("[collapse]没有结尾"))
        assertEquals("多余的结尾[/collapse]", decode("多余的结尾[/collapse]"))
        assertEquals("前" + open(null) + "中" + CLOSE + "[/collapse]后", decode("前[collapse]中[/collapse][/collapse]后"))
        assertEquals("甲乙", decode("甲[/list]乙"))
    }

    /** 签名与短消息使用的旧解码器共用同一套列表转换。 */
    @Test
    fun decodeListsIsSharedWithLegacyDecoder() {
        assertEquals("<ul><li>甲<li>乙</ul>尾", ForumBasicDecoder.decodeLists("[list][*]甲<br/>[*]乙[/list]<br/>尾"))
    }

    private companion object {
        const val CLOSE = "</div></div>"
    }
}
