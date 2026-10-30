package gov.anzong.androidnga.core.decode

import gov.anzong.androidnga.base.util.StringUtils
import gov.anzong.androidnga.core.data.HtmlData
import java.util.ArrayDeque
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 论坛代码的基本格式（引用、链接、字体、表格、列表、折叠等）转换为 HTML。
 * Created by Justwen on 2018/8/25.
 */
class ForumBasicDecoder : IForumDecoder {

    override fun decode(content: String, htmlData: HtmlData?): String {
        if (content.isEmpty()) {
            return ""
        }
        var content = content
        val host = htmlData?.ngaHost
        val ignoreCaseTag = IForumDecoder.IGNORE_CASE_TAG
        val styleLeft = "<div style='float:left' >"
        val styleRight = "<div style='float:right' >"
        content = StringUtils.replaceAll(content, ignoreCaseTag + "&amp;", "&")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[l\\]", styleLeft)
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/l\\]", endDiv)
        // content = StringUtils.replaceAll(content, "\\[L\\]", styleLeft)
        // content = StringUtils.replaceAll(content, "\\[/L\\]", endDiv)

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[r\\]", styleRight)
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/r\\]", endDiv)
        // content = StringUtils.replaceAll(content, "\\[R\\]", styleRight)
        // content = StringUtils.replaceAll(content, "\\[/R\\]", endDiv)

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[align=right\\]", styleAlignRight)
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[align=left\\]", styleAlignLeft)
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[align=center\\]", styleAlignCenter)
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/align\\]", endDiv)

        content = StringUtils.replaceAll(content,
                ignoreCaseTag
                        + "\\[b\\]Reply to \\[pid=(.+?),(.+?),(.+?)\\]Reply\\[/pid\\] (.+?)\\[/b\\]",
                "[quote]Reply to [b]<a href='" + host + "read.php?searchpost=1&pid=$1' style='font-weight: bold;color:#3181f4'>[Reply]</a> $4[/b][/quote]")

        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[pid=(.+?),(.+?),(.+?)\\]Reply\\[/pid\\]",
                "<a href='" + host + "read.php?searchpost=1&pid=$1' style='font-weight: bold;color:#3181f4'>[Reply]</a>")

        // 某些帖子会导致这个方法卡住, 暂时不清楚原因, 和这个方法的作用.... by elrond
        /*content = StringUtils.replaceAll(content, 
                ignoreCaseTag + "={3,}((^=){0,}(.*?){0,}(^=){0,})={3,}",
                "<h4 style='font-weight: bold;border-bottom: 1px solid #AAA;clear: both;margin-bottom: 0px;'>$1</h4>");*/

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[quote\\]", STYLE_QUOTE)
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/quote\\]", endDiv)

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[code\\]", STYLE_QUOTE + "Code:")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[code(.+?)\\]", STYLE_QUOTE)
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/code\\]", endDiv)
        // reply
        // content = StringUtils.replaceAll(content, 
        // ignoreCaseTag +"\\[pid=\\d+\\]Reply\\[/pid\\]", "Reply")
        // content = StringUtils.replaceAll(content, 
        // ignoreCaseTag +"\\[pid=\\d+,\\d+,\\d\\]Reply\\[/pid\\]", "Reply")

        // topic
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[tid=\\d+\\]Topic\\[/pid\\]",
                "Topic")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[tid=?(\\d{0,50})\\]Topic\\[/tid\\]",
                "<a href='" + host + "read.php?tid=$1' style='font-weight: bold;color:#3181f4'>[Topic]</a>")
        // reply
        // s =
        // s.replaceAll("\\[b\\]Reply to \\[pid=\\d+\\]Reply\\[/pid\\] (Post by .+ \\(\\d{4,4}-\\d\\d-\\d\\d \\d\\d:\\d\\d\\))\\[/b\\]"
        // , "Reply to Reply <b>$1</b>")
        // 转换 tag
        // [b]
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[b\\]", "<b>")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/b\\]", "</b>"/* "</font>" */)

        // item
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[item\\]", "<b>")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/item\\]", "</b>")

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[u\\]", "<u>")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/u\\]", "</u>")

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[s:(\\d+)\\]",
                "<img src='file:///android_asset/a$1.gif'>")
        content = content.replace(ignoreCaseTag + "<br/><br/>", "<br/>")
        // [url][/url]
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[url\\]/([^\\[|\\]]+)\\[/url\\]",
                "<a href=\"" + host + "$1\" style='color:#3181f4'>" + host + "$1</a>")
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[url\\]([^\\[|\\]]+)\\[/url\\]",
                "<a href=\"$1\" style='color:#3181f4'>$1</a>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[url=/([^\\[|\\]]+)\\]\\s*(.+?)\\s*\\[/url\\]",
                "<a href=\"" + host + "$1\" style='color:#3181f4'>$2</a>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[url=([^\\[|\\]]+)\\]\\s*(.+?)\\s*\\[/url\\]",
                "<a href=\"$1\">$2</a>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                + "\\[uid=?(\\d{0,50})\\](.+?)\\[\\/uid\\]", "$2")
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "Post by\\s{0,}([^\\[\\s]{1,})\\s{0,}\\(",
                "Post by <a href='" + host + "nuke.php?func=ucp&username=$1' style='font-weight: bold;color:#3181f4'>[$1]</a> (")
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[@(.{2,20}?)\\]",
                "<a href='" + host + "nuke.php?func=ucp&username=$1' style='font-weight: bold;color:#3181f4'>[@$1]</a>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                + "\\[uid=-?(\\d{0,50})\\](.+?)\\[\\/uid\\]", "$2")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[hip\\](.+?)\\[\\/hip\\]",
                "$1")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[tid=?(\\d{0,50})\\](.+?)\\[/tid\\]",
                "<a href='" + host + "read.php?tid=$1' style='font-weight: bold;color:#3181f4'>[$2]</a>")
        content = StringUtils.replaceAll(content,
                ignoreCaseTag
                        + "\\[pid=(.+?)\\]\\[/pid\\]",
                "<a href='" + host + "read.php?pid=$1' style='font-weight: bold;color:#3181f4'>[Reply]</a>")
        content = StringUtils.replaceAll(content,
                ignoreCaseTag
                        + "\\[pid=(.+?)\\](.+?)\\[/pid\\]",
                "<a href='" + host + "read.php?pid=$1' style='font-weight: bold;color:#3181f4'>[$2]</a>")
        // flash
        content = StringUtils.replaceAll(content,
                ignoreCaseTag + "\\[flash\\](http[^\\[|\\]]+)\\[/flash\\]",
                "<a href=\"$1\"><img src='file:///android_asset/flash.png' style= 'max-width:100%;' ></a>")
        // color

        // content = StringUtils.replaceAll(content, "\\[color=([^\\[|\\]]+)\\]\\s*(.+?)\\s*\\[/color\\]"
        // ,"<b style=\"color:$1\">$2</b>")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[color=([^\\[|\\]]+)\\]",
                styleColor)
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/color\\]", "</span>")

        // lessernuke
        content = StringUtils.replaceAll(content, "\\[lessernuke\\]", lesserNukeStyle)
        content = StringUtils.replaceAll(content, "\\[/lessernuke\\]", endDiv)

        // [table][/table]
        content = StringUtils.replaceAll(content, "\\[table](.*?)\\[/table]", "<div><table cellspacing='0px' class='default'><tbody>$1</tbody></table></div>")

        // [tr][/tr]
        content = StringUtils.replaceAll(content, "\\[tr](.*?)\\[/tr]", "<tr>$1</tr>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td[ ]*(\\d+)\\]",
                "<td style='border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\scolspan(\\d+)\\swidth(\\d+)\\]",
                "<td colspan='$1' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\scolspan(\\d+)\\]",
                "<td colspan='$2' style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")

        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\srowspan(\\d+)\\]",
                "<td rowspan='$2' style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\srowspan(\\d+)\\swidth(\\d+)\\]",
                "<td rowspan='$1' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")

        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\scolspan(\\d+)\\srowspan(\\d+)\\swidth(\\d+)\\]",
                "<td colspan='$1' rowspan='$2' style='width:$3%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\scolspan(\\d+)\\swidth(\\d+)\\srowspan(\\d+)\\]",
                "<td colspan='$1' rowspan='$3' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\srowspan(\\d+)\\scolspan(\\d+)\\swidth(\\d+)\\]",
                "<td rowspan='$1' colspan='$2' style='width:$3%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\srowspan(\\d+)\\swidth(\\d+)\\scolspan(\\d+)\\]",
                "<td rowspan='$1' colspan='$3' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\scolspan(\\d+)\\srowspan(\\d+)\\]",
                "<td rowspan='$3' colspan='$2' style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\srowspan(\\d+)\\scolspan(\\d+)\\]",
                "<td rowspan='$2' colspan='$3'  style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")


        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\scolspan=(\\d+)\\]",
                "<td colspan='$1' style='border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        content = StringUtils.replaceAll(content, ignoreCaseTag
                        + "\\[td\\srowspan=(\\d+)\\]",
                "<td rowspan='$1' style='border-left:1px solid #aaa;border-bottom:1px solid #aaa;'>")
        content = StringUtils.replaceAll(content, "\\[td\\]", "<td style='border-left:1px solid #aaa;border-bottom:1px solid #aaa;'>")
        content = StringUtils.replaceAll(content, "\\[/td\\]", "</td>")
        // 处理表格外面的额外空行
        content = StringUtils.replaceAll(content, "<([/]?(table|tbody|tr|td))><br/>", "<$1>")
        // [i][/i]
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[i\\]",
                "<i style=\"font-style:italic\">")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/i\\]", "</i>")
        // [del][/del]
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[del\\]", "<del class=\"gray\">")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/del\\]", "</del>")

        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[font=([^\\[|\\]]+)\\]",
                "<span style=\"font-family:$1\">")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/font\\]", "</span>")

        // size
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[size=(\\d+)%\\]",
                "<span style=\"font-size:$1%;line-height:$1%\">")
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[/size\\]", "</span>")

        // [list][/list]、[*]
        content = decodeLists(content)

        // [h][/h]
        content = StringUtils.replaceAll(content, ignoreCaseTag + "\\[h](.+?)\\[/h]", "<b>$1</b>")

        // [collapse][/collapse]、[collapse=标题][/collapse]：按配对转换，支持嵌套与跨行；未配对的标签保留原文
        content = replacePaired(content, COLLAPSE_TAG, { match ->
            val title = match.group(2)
            "<div><button onclick='toggleCollapse(this)'>点击显示内容" + (if (title == null) "" else " : $title") +
                "</button><div name='collapse' class='collapse' style='display:none'>"
        }, "</div></div>", null)

        // [flash=video]/flash]
        content = StringUtils.replaceAll(content, "\\[flash=video].(.*?)\\[/flash]", "<video src='https://img.nga.cn/attachments$1' controls='controls'></video>")

        // [flash=audio][/flash]"
        content = StringUtils.replaceAll(content, "\\[flash=audio].(.*?)\\[/flash]", "<audio src='https://img.nga.cn/attachments$1&filename=nga_audio.mp3' controls='controls'></audio>")

        return content
    }

    companion object {
        private const val lesserNukeStyle = "<div style='border:1px solid #B63F32;margin:10px 10px 10px 10px;padding:10px' > <span style='color:#EE8A9E'>用户因此贴被暂时禁言，此效果不会累加</span><br/>"
        private const val styleAlignRight = "<div style='text-align:right' >"
        private const val styleAlignLeft = "<div style='text-align:left' >"
        private const val styleAlignCenter = "<div style='text-align:center' >"
        private const val styleColor = "<span style='color:$1' >"
        private const val endDiv = "</div>"
        private const val STYLE_QUOTE = "<div class='quote' >"

        /** 第 1 组为开标签（collapse 的第 2 组为标题），闭标签没有分组。 */
        private val LIST_TAG: Pattern = Pattern.compile("(?i)\\[(list)\\]|\\[/list\\]")
        private val COLLAPSE_TAG: Pattern = Pattern.compile("(?i)\\[(collapse)(?:=([^\\]]*))?\\]|\\[/collapse\\]")

        /**
         * 转换 [list][/list] 与 [*]：开闭标签按配对转换，支持嵌套，未配对的去掉；列表项交给浏览器自动闭合，
         * 不再以 &lt;br/&gt; 截取内容，避免最后一项吞掉 &lt;/ul&gt; 与其后的折叠等标签。
         */
        @JvmStatic
        fun decodeLists(content: String): String {
            var result = replacePaired(content, LIST_TAG, { "<ul>" }, "</ul>", "")
            result = StringUtils.replaceAll(result, "\\[\\*\\]", "<li>")
            result = StringUtils.replaceAll(result, "(?:<br/>\\s*)+(?=<li>|</ul>)", "")
            return StringUtils.replaceAll(result, "(<ul>|</ul>)\\s*<br/>", "$1")
        }

        /**
         * 把成对的开闭标签转换为 HTML，嵌套时按就近原则配对。[tag] 匹配第 1 组为开标签、否则为闭标签；
         * [open] 在扫描到开标签时以当前匹配调用。
         * 未配对的标签替换为 [unmatched]；为 null 时保留原文。
         */
        @JvmStatic
        fun replacePaired(
            content: String,
            tag: Pattern,
            open: (Matcher) -> String,
            close: String,
            unmatched: String?,
        ): String {
            class Token(val start: Int, val end: Int, val html: String?) {
                val isOpen get() = html != null
                var paired = false
            }

            val tokens = ArrayList<Token>()
            val matcher = tag.matcher(content)
            while (matcher.find()) {
                val isOpen = matcher.group(1) != null
                tokens.add(Token(matcher.start(), matcher.end(), if (isOpen) open(matcher) else null))
            }
            if (tokens.isEmpty()) return content
            val stack = ArrayDeque<Token>()
            for (token in tokens) {
                if (token.isOpen) {
                    stack.push(token)
                } else if (!stack.isEmpty()) {
                    stack.pop().paired = true
                    token.paired = true
                }
            }
            val out = StringBuilder(content.length + tokens.size * 32)
            var last = 0
            for (token in tokens) {
                out.append(content, last, token.start)
                when {
                    token.paired -> out.append(token.html ?: close)
                    else -> out.append(unmatched ?: content.substring(token.start, token.end))
                }
                last = token.end
            }
            return out.append(content, last, content.length).toString()
        }
    }
}
