package gov.anzong.androidnga.core.decode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gov.anzong.androidnga.base.util.StringUtils;
import gov.anzong.androidnga.core.data.HtmlData;

/**
 * Created by Justwen on 2018/8/25.
 */
public class ForumBasicDecoder implements IForumDecoder {

    private static final String lesserNukeStyle = "<div style='border:1px solid #B63F32;margin:10px 10px 10px 10px;padding:10px' > <span style='color:#EE8A9E'>用户因此贴被暂时禁言，此效果不会累加</span><br/>";
    private static final String styleAlignRight = "<div style='text-align:right' >";
    private static final String styleAlignLeft = "<div style='text-align:left' >";
    private static final String styleAlignCenter = "<div style='text-align:center' >";
    private static final String styleColor = "<span style='color:$1' >";
    private static final String endDiv = "</div>";

    private static final String STYLE_QUOTE = "<div class='quote' >";

    /** 第 1 组为开标签（collapse 的第 2 组为标题），闭标签没有分组。 */
    private static final Pattern LIST_TAG = Pattern.compile("(?i)\\[(list)\\]|\\[/list\\]");
    private static final Pattern COLLAPSE_TAG = Pattern.compile("(?i)\\[(collapse)(?:=([^\\]]*))?\\]|\\[/collapse\\]");

    /**
     * 转换 [list][/list] 与 [*]：开闭标签按配对转换，支持嵌套，未配对的去掉；列表项交给浏览器自动闭合，
     * 不再以 &lt;br/&gt; 截取内容，避免最后一项吞掉 &lt;/ul&gt; 与其后的折叠等标签。
     */
    public static String decodeLists(String content) {
        content = replacePaired(content, LIST_TAG, match -> "<ul>", "</ul>", "");
        content = StringUtils.replaceAll(content, "\\[\\*\\]", "<li>");
        content = StringUtils.replaceAll(content, "(?:<br/>\\s*)+(?=<li>|</ul>)", "");
        return StringUtils.replaceAll(content, "(<ul>|</ul>)\\s*<br/>", "$1");
    }

    /**
     * 把成对的开闭标签转换为 HTML，嵌套时按就近原则配对。[tag] 匹配第 1 组为开标签、否则为闭标签；
     * [open] 在扫描到开标签时以当前匹配调用。
     * 未配对的标签替换为 [unmatched]；为 null 时保留原文。
     */
    static String replacePaired(String content, Pattern tag, Function<Matcher, String> open, String close, String unmatched) {
        List<int[]> tokens = new ArrayList<>();
        List<String> opened = new ArrayList<>();
        Matcher matcher = tag.matcher(content);
        while (matcher.find()) {
            boolean isOpen = matcher.group(1) != null;
            tokens.add(new int[]{matcher.start(), matcher.end(), isOpen ? 1 : 0, 0});
            opened.add(isOpen ? open.apply(matcher) : null);
        }
        if (tokens.isEmpty()) return content;
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i)[2] == 1) {
                stack.push(i);
            } else if (!stack.isEmpty()) {
                tokens.get(stack.pop())[3] = 1;
                tokens.get(i)[3] = 1;
            }
        }
        StringBuilder out = new StringBuilder(content.length() + tokens.size() * 32);
        int last = 0;
        for (int i = 0; i < tokens.size(); i++) {
            int[] token = tokens.get(i);
            out.append(content, last, token[0]);
            if (token[3] == 1) out.append(token[2] == 1 ? opened.get(i) : close);
            else out.append(unmatched != null ? unmatched : content.substring(token[0], token[1]));
            last = token[1];
        }
        return out.append(content, last, content.length()).toString();
    }

    @Override
    public String decode(String content, HtmlData htmlData) {
        if (StringUtils.isEmpty(content)) {
            return "";
        }
        // s = StringUtils.unEscapeHtml(s);

        String quoteStyle = STYLE_QUOTE;

        final String styleLeft = "<div style='float:left' >";
        final String styleRight = "<div style='float:right' >";
        content = StringUtils.replaceAll(content, ignoreCaseTag + "&amp;", "&");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[l\\]", styleLeft);
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/l\\]", endDiv);
        // content = StringUtils.replaceAll(content, "\\[L\\]", styleLeft);
        // content = StringUtils.replaceAll(content, "\\[/L\\]", endDiv);

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[r\\]", styleRight);
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/r\\]", endDiv);
        // content = StringUtils.replaceAll(content, "\\[R\\]", styleRight);
        // content = StringUtils.replaceAll(content, "\\[/R\\]", endDiv);

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[align=right\\]", styleAlignRight);
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[align=left\\]", styleAlignLeft);
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[align=center\\]", styleAlignCenter);
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/align\\]", endDiv);

        content = StringUtils.replaceAll(content,
                ignoreCaseTag
                        + "\\[b\\]Reply to \\[pid=(.+?),(.+?),(.+?)\\]Reply\\[/pid\\] (.+?)\\[/b\\]",
                "[quote]Reply to [b]<a href='" + htmlData.getNGAHost() + "read.php?searchpost=1&pid=$1' style='font-weight: bold;color:#3181f4'>[Reply]</a> $4[/b][/quote]");

        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[pid=(.+?),(.+?),(.+?)\\]Reply\\[/pid\\]",
                "<a href='" + htmlData.getNGAHost() + "read.php?searchpost=1&pid=$1' style='font-weight: bold;color:#3181f4'>[Reply]</a>");

        // 某些帖子会导致这个方法卡住, 暂时不清楚原因, 和这个方法的作用.... by elrond
        /*content = StringUtils.replaceAll(content, 
                ignoreCaseTag + "={3,}((^=){0,}(.*?){0,}(^=){0,})={3,}",
                "<h4 style='font-weight: bold;border-bottom: 1px solid #AAA;clear: both;margin-bottom: 0px;'>$1</h4>");*/

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[quote\\]", quoteStyle);
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/quote\\]", endDiv);

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[code\\]", quoteStyle + "Code:");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[code(.+?)\\]", quoteStyle);
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/code\\]", endDiv);
        // reply
        // content = StringUtils.replaceAll(content, 
        // ignoreCaseTag +"\\[pid=\\d+\\]Reply\\[/pid\\]", "Reply");
        // content = StringUtils.replaceAll(content, 
        // ignoreCaseTag +"\\[pid=\\d+,\\d+,\\d\\]Reply\\[/pid\\]", "Reply");

        // topic
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[tid=\\d+\\]Topic\\[/pid\\]",
                "Topic");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[tid=?(\\d{0,50})\\]Topic\\[/tid\\]",
                "<a href='" + htmlData.getNGAHost() + "read.php?tid=$1' style='font-weight: bold;color:#3181f4'>[Topic]</a>");
        // reply
        // s =
        // s.replaceAll("\\[b\\]Reply to \\[pid=\\d+\\]Reply\\[/pid\\] (Post by .+ \\(\\d{4,4}-\\d\\d-\\d\\d \\d\\d:\\d\\d\\))\\[/b\\]"
        // , "Reply to Reply <b>$1</b>");
        // 转换 tag
        // [b]
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[b\\]", "<b>");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/b\\]", "</b>"/* "</font>" */);

        // item
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[item\\]", "<b>");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/item\\]", "</b>");

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[u\\]", "<u>");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/u\\]", "</u>");

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[s:(\\d+)\\]",
                "<img src='file:///android_asset/a$1.gif'>");
        content = content.replace(ignoreCaseTag + "<br/><br/>", "<br/>");
        // [url][/url]
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[url\\]/([^\\[|\\]]+)\\[/url\\]",
                "<a href=\"" + htmlData.getNGAHost() + "$1\" style='color:#3181f4'>" + htmlData.getNGAHost() + "$1</a>");
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[url\\]([^\\[|\\]]+)\\[/url\\]",
                "<a href=\"$1\" style='color:#3181f4'>$1</a>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[url=/([^\\[|\\]]+)\\]\\s*(.+?)\\s*\\[/url\\]",
                "<a href=\"" + htmlData.getNGAHost() + "$1\" style='color:#3181f4'>$2</a>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[url=([^\\[|\\]]+)\\]\\s*(.+?)\\s*\\[/url\\]",
                "<a href=\"$1\">$2</a>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                + "\\[uid=?(\\d{0,50})\\](.+?)\\[\\/uid\\]", "$2");
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "Post by\\s{0,}([^\\[\\s]{1,})\\s{0,}\\(",
                "Post by <a href='" + htmlData.getNGAHost() + "nuke.php?func=ucp&username=$1' style='font-weight: bold;color:#3181f4'>[$1]</a> (");
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[@(.{2,20}?)\\]",
                "<a href='" + htmlData.getNGAHost() + "nuke.php?func=ucp&username=$1' style='font-weight: bold;color:#3181f4'>[@$1]</a>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                + "\\[uid=-?(\\d{0,50})\\](.+?)\\[\\/uid\\]", "$2");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[hip\\](.+?)\\[\\/hip\\]",
                "$1");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[tid=?(\\d{0,50})\\](.+?)\\[/tid\\]",
                "<a href='" + htmlData.getNGAHost() + "read.php?tid=$1' style='font-weight: bold;color:#3181f4'>[$2]</a>");
        content = StringUtils.replaceAll(content,
                ignoreCaseTag
                        + "\\[pid=(.+?)\\]\\[/pid\\]",
                "<a href='" + htmlData.getNGAHost() + "read.php?pid=$1' style='font-weight: bold;color:#3181f4'>[Reply]</a>");
        content = StringUtils.replaceAll(content,
                ignoreCaseTag
                        + "\\[pid=(.+?)\\](.+?)\\[/pid\\]",
                "<a href='" + htmlData.getNGAHost() + "read.php?pid=$1' style='font-weight: bold;color:#3181f4'>[$2]</a>");
        // flash
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[flash\\](http[^\\[|\\]]+)\\[/flash\\]",
                "<a href=\"$1\"><img src='file:///android_asset/flash.png' style= 'max-width:100%;' ></a>");
        // color

        // content = StringUtils.replaceAll(content, "\\[color=([^\\[|\\]]+)\\]\\s*(.+?)\\s*\\[/color\\]"
        // ,"<b style=\"color:$1\">$2</b>");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[color=([^\\[|\\]]+)\\]",
                styleColor);
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/color\\]", "</span>");

        // lessernuke
        content = StringUtils.replaceAll(content, "\\[lessernuke\\]", lesserNukeStyle);
        content = StringUtils.replaceAll(content, "\\[/lessernuke\\]", endDiv);

        // [table][/table]
        content = StringUtils.replaceAll(content, "\\[table](.*?)\\[/table]", "<div><table cellspacing='0px' class='default'><tbody>$1</tbody></table></div>");

        // [tr][/tr]
        content = StringUtils.replaceAll(content, "\\[tr](.*?)\\[/tr]", "<tr>$1</tr>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td[ ]*(\\d+)\\]",
                "<td style='border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\scolspan(\\d+)\\swidth(\\d+)\\]",
                "<td colspan='$1' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\scolspan(\\d+)\\]",
                "<td colspan='$2' style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");

        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\srowspan(\\d+)\\]",
                "<td rowspan='$2' style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\srowspan(\\d+)\\swidth(\\d+)\\]",
                "<td rowspan='$1' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");

        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\scolspan(\\d+)\\srowspan(\\d+)\\swidth(\\d+)\\]",
                "<td colspan='$1' rowspan='$2' style='width:$3%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\scolspan(\\d+)\\swidth(\\d+)\\srowspan(\\d+)\\]",
                "<td colspan='$1' rowspan='$3' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\srowspan(\\d+)\\scolspan(\\d+)\\swidth(\\d+)\\]",
                "<td rowspan='$1' colspan='$2' style='width:$3%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\srowspan(\\d+)\\swidth(\\d+)\\scolspan(\\d+)\\]",
                "<td rowspan='$1' colspan='$3' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\scolspan(\\d+)\\srowspan(\\d+)\\]",
                "<td rowspan='$3' colspan='$2' style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\srowspan(\\d+)\\scolspan(\\d+)\\]",
                "<td rowspan='$2' colspan='$3'  style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");


        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\scolspan=(\\d+)\\]",
                "<td colspan='$1' style='border-left:1px solid #aaa;border-bottom:1px solid #aaa'>");
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\srowspan=(\\d+)\\]",
                "<td rowspan='$1' style='border-left:1px solid #aaa;border-bottom:1px solid #aaa;'>");
        content = StringUtils.replaceAll(content, "\\[td\\]", "<td style='border-left:1px solid #aaa;border-bottom:1px solid #aaa;'>");
        content = StringUtils.replaceAll(content, "\\[/td\\]", "</td>");
        // 处理表格外面的额外空行
        content = StringUtils.replaceAll(content, "<([/]?(table|tbody|tr|td))><br/>", "<$1>");
        // [i][/i]
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[i\\]",
                "<i style=\"font-style:italic\">");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/i\\]", "</i>");
        // [del][/del]
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[del\\]", "<del class=\"gray\">");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/del\\]", "</del>");

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[font=([^\\[|\\]]+)\\]",
                "<span style=\"font-family:$1\">");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/font\\]", "</span>");

        // size
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[size=(\\d+)%\\]",
                "<span style=\"font-size:$1%;line-height:$1%\">");
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/size\\]", "</span>");

        // [list][/list]、[*]
        content = decodeLists(content);

        // [h][/h]
        content = StringUtils.replaceAll(content, IGNORE_CASE_TAG + "\\[h](.+?)\\[/h]", "<b>$1</b>");

        // [collapse][/collapse]、[collapse=标题][/collapse]：按配对转换，支持嵌套与跨行；未配对的标签保留原文
        content = replacePaired(content, COLLAPSE_TAG, match -> {
            String title = match.group(2);
            return "<div><button onclick='toggleCollapse(this)'>点击显示内容" + (title == null ? "" : " : " + title)
                    + "</button><div name='collapse' class='collapse' style='display:none'>";
        }, "</div></div>", null);

        // [flash=video]/flash]
        content = StringUtils.replaceAll(content, "\\[flash=video].(.*?)\\[/flash]", "<video src='https://img.nga.cn/attachments$1' controls='controls'></video>");

        // [flash=audio][/flash]"
        content = StringUtils.replaceAll(content, "\\[flash=audio].(.*?)\\[/flash]", "<audio src='https://img.nga.cn/attachments$1&filename=nga_audio.mp3' controls='controls'></audio>");

        return content;
    }
}
