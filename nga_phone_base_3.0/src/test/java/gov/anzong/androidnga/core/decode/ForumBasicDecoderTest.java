package gov.anzong.androidnga.core.decode;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import gov.anzong.androidnga.core.data.HtmlData;

/** 列表与折叠标签的转换：配对、嵌套与相互包含；内容均为合成。 */
public class ForumBasicDecoderTest {

    private static final String OPEN = "<div><button onclick='toggleCollapse(this)'>点击显示内容%s</button><div name='collapse' class='collapse' style='display:none'>";
    private static final String CLOSE = "</div></div>";

    private final ForumBasicDecoder decoder = new ForumBasicDecoder();

    private String decode(String content) {
        return decoder.decode(content, HtmlData.create(content, "https://bbs.nga.cn/"));
    }

    private static String open(String title) {
        return String.format(OPEN, title == null ? "" : " : " + title);
    }

    @Test
    public void listItemsDropTrailingLineBreaks() {
        assertEquals("<ul><li>甲<li>乙</ul>后文", decode("[list][*]甲<br/>[*]乙<br/>[/list]<br/>后文"));
    }

    /** 最后一项没有换行时，列表也要在 [/list] 处结束，不能吞掉其后的折叠。 */
    @Test
    public void listEndingWithoutLineBreakDoesNotSwallowFollowingCollapse() {
        assertEquals("<ul><li>甲<li>乙</ul>" + open("剧透") + "秘密" + CLOSE,
                decode("[list][*]甲[*]乙[/list][collapse=剧透]秘密[/collapse]"));
    }

    @Test
    public void nestedCollapsesPairInnermostFirst() {
        assertEquals(open("外") + "A" + open("内") + "B" + CLOSE + "C" + CLOSE,
                decode("[collapse=外]A[collapse=内]B[/collapse]C[/collapse]"));
    }

    @Test
    public void nestedListsKeepOuterItems() {
        assertEquals("<ul><li>甲<ul><li>乙</ul><li>丙</ul>", decode("[list][*]甲[list][*]乙[/list][*]丙[/list]"));
    }

    @Test
    public void listInsideCollapseAndCollapseInsideListItem() {
        assertEquals(open(null) + "<ul><li>甲<li>乙</ul>" + CLOSE, decode("[collapse][list][*]甲<br/>[*]乙<br/>[/list][/collapse]"));
        assertEquals("<ul><li>甲" + open("细节") + "内容" + CLOSE + "<li>乙</ul>",
                decode("[list][*]甲[collapse=细节]内容[/collapse][*]乙[/list]"));
    }

    @Test
    public void collapseSpansLinesAndIgnoresCase() {
        assertEquals(open(null) + "第一行\n第二行" + CLOSE, decode("[COLLAPSE]第一行\n第二行[/Collapse]"));
    }

    /** 未配对的折叠标签按原文显示，不产生多余的 </div>；未配对的列表标签与以前一样去掉。 */
    @Test
    public void unmatchedTagsDoNotBreakStructure() {
        assertEquals("[collapse]没有结尾", decode("[collapse]没有结尾"));
        assertEquals("多余的结尾[/collapse]", decode("多余的结尾[/collapse]"));
        assertEquals("前" + open(null) + "中" + CLOSE + "[/collapse]后", decode("前[collapse]中[/collapse][/collapse]后"));
        assertEquals("甲乙", decode("甲[/list]乙"));
    }

    /** 签名与短消息使用的旧解码器共用同一套列表转换。 */
    @Test
    public void decodeListsIsSharedWithLegacyDecoder() {
        assertEquals("<ul><li>甲<li>乙</ul>尾", ForumBasicDecoder.decodeLists("[list][*]甲<br/>[*]乙[/list]<br/>尾"));
    }
}
