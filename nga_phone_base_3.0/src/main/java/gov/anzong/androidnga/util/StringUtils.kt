package gov.anzong.androidnga.util

import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.util.EmoticonUtils
import gov.anzong.androidnga.core.decode.ForumBasicDecoder
import gov.anzong.androidnga.core.decode.ForumImageUrl
import gov.anzong.androidnga.network.bean.StringFindResult
import gov.anzong.androidnga.ui.theme.ThemeManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/** 字符串工具与旧版论坛代码解码（签名与短消息正文仍使用）。 */
object StringUtils {

    private const val lesserNukeStyle =
        "<div style='border:1px solid #B63F32;margin:10px 10px 10px 10px;padding:10px' > <span style='color:#EE8A9E'>用户因此贴被暂时禁言，此效果不会累加</span><br/>"
    private const val styleAlignRight = "<div style='text-align:right' >"
    private const val styleAlignLeft = "<div style='text-align:left' >"
    private const val styleAlignCenter = "<div style='text-align:center' >"
    private const val styleColor = "<span style='color:$1' >"
    private const val ignoreCaseTag = "(?i)"
    private const val endDiv = "</div>"

    /** 与 Java 的 String.replaceAll 相同：[regex] 为正则表达式，[replacement] 中的 `$n` 为分组引用。 */
    private fun String.replaceAll(regex: String, replacement: String): String =
        Pattern.compile(regex).matcher(this).replaceAll(replacement)

    /** 判断是否是 "" 或者 null */
    @JvmStatic
    fun isEmpty(str: String?): Boolean = str.isNullOrEmpty()

    /** 解码 `%XX` 与 `%uXXXX`，给候总客户端乱码加适配。 */
    @JvmStatic
    fun unescape(src: String?): String {
        if (src.isNullOrEmpty()) return ""
        val tmp = StringBuilder(src.length)
        var lastPos = 0
        while (lastPos < src.length) {
            val pos = src.indexOf("%", lastPos)
            if (pos == lastPos) {
                if (pos > src.length - 3) {
                    tmp.append(src, pos, src.length)
                    lastPos = pos + 3
                } else if (src[pos + 1] == 'u') {
                    try {
                        val hex = src.substring(pos + 2, pos + 6)
                        if (Pattern.matches("[A-Fa-f0-9]{4}", hex)) {
                            tmp.append(hex.toInt(16).toChar())
                            lastPos = pos + 6
                        } else {
                            tmp.append(src, pos, pos + 3)
                            lastPos = pos + 3
                        }
                    } catch (e: Exception) {
                        tmp.append(src, pos, pos + 3)
                        lastPos = pos + 3
                    }
                } else {
                    try {
                        tmp.append(src.substring(pos + 1, pos + 3).toInt(16).toChar())
                    } catch (e: Exception) {
                        tmp.append(src, pos, pos + 3)
                    }
                    lastPos = pos + 3
                }
            } else if (pos == -1) {
                tmp.append(src, lastPos, src.length)
                lastPos = src.length
            } else {
                tmp.append(src, lastPos, pos)
                lastPos = pos
            }
        }
        return tmp.toString()
    }

    /** 按指定字符集编码 URL 参数；java.net.URLEncoder 在部分系统上对中英文混排编码错误。 */
    @JvmStatic
    fun encodeUrl(s: String?, charset: String): String? = UriEncoderWithCharset.encode(s, null, charset)

    private fun decodeAlbum(content: String, quoteDiv: String): String {
        var s = content
        var startPos = s.indexOf("[album=")
        var endPos = s.indexOf("[/album]") + 8
        while (startPos in 0 until endPos) {
            val up = s.substring(0, startPos)
            val down = s.substring(endPos)
            val album = s.substring(startPos, endPos)
            val title = "相册列表:" + album.replaceAll("(?i)\\[album=(.*?)\\](.*?)\\[/album\\]", "$1") + "<br/>"
            var images = album.replaceAll("(?i)\\[album=(.*?)\\](.*?)\\[/album\\]", "$2")
            if (images.startsWith("<br/>")) {
                images = "[img]" + images.substring(5) + "[/img]"
            }
            images = images.replaceAll("<br/>", "[/img]<br/><br/>[img]")
            s = "$up<br/>$quoteDiv$title<br/>$images</div>$down"
            startPos = s.indexOf("[album=")
            endPos = s.indexOf("[/album]") + 8
        }
        return s
    }

    /** 论坛代码转为 HTML；[imageUrls] 收集附件域名上的图片。 */
    @JvmStatic
    fun decodeForumTag(content: String?, showImage: Boolean, imageQuality: Int, imageUrls: MutableList<String>?): String {
        if (content.isNullOrEmpty()) return ""
        val quoteStyle = if (ThemeManager.getInstance().isNightMode) {
            "<div style='background:#000000;padding:5px;border:1px solid #888' >"
        } else {
            "<div style='background:#E8E8E8;padding:5px;border:1px solid #888' >"
        }
        var ret = decodeAlbum(content, quoteStyle)
        val styleLeft = "<div style='float:left' >"
        val styleRight = "<div style='float:right' >"
        ret = ret.replaceAll(ignoreCaseTag + "&amp;", "&")
        ret = ret.replaceAll(ignoreCaseTag + "\\[l\\]", styleLeft)
        ret = ret.replaceAll(ignoreCaseTag + "\\[/l\\]", endDiv)
        // ret = ret.replaceAll("\\[L\\]", styleLeft)
        // ret = ret.replaceAll("\\[/L\\]", endDiv)
        ret = ret.replaceAll(ignoreCaseTag + "\\[r\\]", styleRight)
        ret = ret.replaceAll(ignoreCaseTag + "\\[/r\\]", endDiv)
        // ret = ret.replaceAll("\\[R\\]", styleRight)
        // ret = ret.replaceAll("\\[/R\\]", endDiv)
        ret = ret.replaceAll(ignoreCaseTag + "\\[align=right\\]", styleAlignRight)
        ret = ret.replaceAll(ignoreCaseTag + "\\[align=left\\]", styleAlignLeft)
        ret = ret.replaceAll(ignoreCaseTag + "\\[align=center\\]", styleAlignCenter)
        ret = ret.replaceAll(ignoreCaseTag + "\\[/align\\]", endDiv)
        ret = ret.replaceAll(
                ignoreCaseTag
                        + "\\[b\\]Reply to \\[pid=(.+?),(.+?),(.+?)\\]Reply\\[/pid\\] (.+?)\\[/b\\]",
                "[quote]Reply to [b]<a href='" + Utils.getNGAHost() + "read.php?searchpost=1&pid=$1' style='font-weight: bold;'>[Reply]</a> $4[/b][/quote]")
        ret = ret.replaceAll(
                ignoreCaseTag + "\\[pid=(.+?),(.+?),(.+?)\\]Reply\\[/pid\\]",
                "<a href='" + Utils.getNGAHost() + "read.php?searchpost=1&pid=$1' style='font-weight: bold;'>[Reply]</a>")
        // 某些帖子会导致这个方法卡住, 暂时不清楚原因, 和这个方法的作用.... by elrond
        /*ret = ret.replaceAll(
                ignoreCaseTag + "={3,}((^=){0,}(.*?){0,}(^=){0,})={3,}",
                "<h4 style='font-weight: bold;border-bottom: 1px solid #AAA;clear: both;margin-bottom: 0px;'>$1</h4>");*/

        ret = ret.replaceAll(ignoreCaseTag + "\\[quote\\]", quoteStyle)
        ret = ret.replaceAll(ignoreCaseTag + "\\[/quote\\]", endDiv)
        ret = ret.replaceAll(ignoreCaseTag + "\\[code\\]", quoteStyle + "Code:")
        ret = ret.replaceAll(ignoreCaseTag + "\\[code(.+?)\\]", quoteStyle)
        ret = ret.replaceAll(ignoreCaseTag + "\\[/code\\]", endDiv)
        // reply
        // ret = ret.replaceAll(
        // ignoreCaseTag +"\\[pid=\\d+\\]Reply\\[/pid\\]", "Reply")
        // ret = ret.replaceAll(
        // ignoreCaseTag +"\\[pid=\\d+,\\d+,\\d\\]Reply\\[/pid\\]", "Reply")
        // topic
        ret = ret.replaceAll(ignoreCaseTag + "\\[tid=\\d+\\]Topic\\[/pid\\]",
                "Topic")
        ret = ret.replaceAll(ignoreCaseTag + "\\[tid=?(\\d{0,50})\\]Topic\\[/tid\\]",
                "<a href='" + Utils.getNGAHost() + "read.php?tid=$1' style='font-weight: bold;'>[Topic]</a>")
        // reply
        // s =
        // s.replaceAll("\\[b\\]Reply to \\[pid=\\d+\\]Reply\\[/pid\\] (Post by .+ \\(\\d{4,4}-\\d\\d-\\d\\d \\d\\d:\\d\\d\\))\\[/b\\]"
        // , "Reply to Reply <b>$1</b>")
        // 转换 tag
        // [b]
        ret = ret.replaceAll(ignoreCaseTag + "\\[b\\]", "<b>")
        ret = ret.replaceAll(ignoreCaseTag + "\\[/b\\]", "</b>")
        // item
        ret = ret.replaceAll(ignoreCaseTag + "\\[item\\]", "<b>")
        ret = ret.replaceAll(ignoreCaseTag + "\\[/item\\]", "</b>")
        ret = ret.replaceAll(ignoreCaseTag + "\\[u\\]", "<u>")
        ret = ret.replaceAll(ignoreCaseTag + "\\[/u\\]", "</u>")
        ret = ret.replaceAll(ignoreCaseTag + "\\[s:(\\d+)\\]",
                "<img src='file:///android_asset/a$1.gif'>")
        ret = buildEmoticonImage(ret)
        ret = ret.replace(ignoreCaseTag + "<br/><br/>", "<br/>")
        // [url][/url]
        ret = ret.replaceAll(
                ignoreCaseTag + "\\[url\\]/([^\\[|\\]]+)\\[/url\\]",
                "<a href=\"" + Utils.getNGAHost() + "$1\">" + Utils.getNGAHost() + "$1</a>")
        ret = ret.replaceAll(
                ignoreCaseTag + "\\[url\\]([^\\[|\\]]+)\\[/url\\]",
                "<a href=\"$1\">$1</a>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[url=/([^\\[|\\]]+)\\]\\s*(.+?)\\s*\\[/url\\]",
                "<a href=\"" + Utils.getNGAHost() + "$1\">$2</a>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[url=([^\\[|\\]]+)\\]\\s*(.+?)\\s*\\[/url\\]",
                "<a href=\"$1\">$2</a>")
        ret = ret.replaceAll(ignoreCaseTag
                + "\\[uid=?(\\d{0,50})\\](.+?)\\[\\/uid\\]", "$2")
        ret = ret.replaceAll(
                ignoreCaseTag + "Post by\\s{0,}([^\\[\\s]{1,})\\s{0,}\\(",
                "Post by <a href='" + Utils.getNGAHost() + "nuke.php?func=ucp&username=$1' style='font-weight: bold;'>[$1]</a> (")
        ret = ret.replaceAll(
                ignoreCaseTag + "\\[@(.{2,20}?)\\]",
                "<a href='" + Utils.getNGAHost() + "nuke.php?func=ucp&username=$1' style='font-weight: bold;'>[@$1]</a>")
        ret = ret.replaceAll(ignoreCaseTag
                + "\\[uid=-?(\\d{0,50})\\](.+?)\\[\\/uid\\]", "$2")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[hip\\](.+?)\\[\\/hip\\]",
                "$1")
        ret = ret.replaceAll(ignoreCaseTag + "\\[tid=?(\\d{0,50})\\](.+?)\\[/tid\\]",
                "<a href='" + Utils.getNGAHost() + "read.php?tid=$1' style='font-weight: bold;'>[$2]</a>")
        ret = ret.replaceAll(
                ignoreCaseTag
                        + "\\[pid=(.+?)\\]\\[/pid\\]",
                "<a href='" + Utils.getNGAHost() + "read.php?pid=$1' style='font-weight: bold;'>[Reply]</a>")
        ret = ret.replaceAll(
                ignoreCaseTag
                        + "\\[pid=(.+?)\\](.+?)\\[/pid\\]",
                "<a href='" + Utils.getNGAHost() + "read.php?pid=$1' style='font-weight: bold;'>[$2]</a>")
        // flash
        ret = ret.replaceAll(
                ignoreCaseTag + "\\[flash\\](http[^\\[|\\]]+)\\[/flash\\]",
                "<a href=\"$1\"><img src='file:///android_asset/flash.png' style= 'max-width:100%;' ></a>")
        // color

        // ret = ret.replaceAll("\\[color=([^\\[|\\]]+)\\]\\s*(.+?)\\s*\\[/color\\]"
        // ,"<b style=\"color:$1\">$2</b>")
        ret = ret.replaceAll(ignoreCaseTag + "\\[color=([^\\[|\\]]+)\\]",
                styleColor)
        ret = ret.replaceAll(ignoreCaseTag + "\\[/color\\]", "</span>")
        // lessernuke
        ret = ret.replaceAll("\\[lessernuke\\]", lesserNukeStyle)
        ret = ret.replaceAll("\\[/lessernuke\\]", endDiv)
        ret = ret.replaceAll(
                "\\[table\\]",
                "<div><table cellspacing='0px' style='border:1px solid #aaa;width:99.9%;'><tbody>")
        ret = ret.replaceAll("\\[/table\\]", "</tbody></table></div>")
        ret = ret.replaceAll("\\[tr\\]", "<tr>")
        ret = ret.replaceAll("\\[/tr\\]", "<tr>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td(\\d+)\\]",
                "<td style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\scolspan(\\d+)\\swidth(\\d+)\\]",
                "<td colspan='$1' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\scolspan(\\d+)\\]",
                "<td colspan='$2' style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\srowspan(\\d+)\\]",
                "<td rowspan='$2' style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\srowspan(\\d+)\\swidth(\\d+)\\]",
                "<td rowspan='$1' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\scolspan(\\d+)\\srowspan(\\d+)\\swidth(\\d+)\\]",
                "<td colspan='$1' rowspan='$2' style='width:$3%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\scolspan(\\d+)\\swidth(\\d+)\\srowspan(\\d+)\\]",
                "<td colspan='$1' rowspan='$3' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\srowspan(\\d+)\\scolspan(\\d+)\\swidth(\\d+)\\]",
                "<td rowspan='$1' colspan='$2' style='width:$3%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\srowspan(\\d+)\\swidth(\\d+)\\scolspan(\\d+)\\]",
                "<td rowspan='$1' colspan='$3' style='width:$2%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\scolspan(\\d+)\\srowspan(\\d+)\\]",
                "<td rowspan='$3' colspan='$2' style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\swidth(\\d+)\\srowspan(\\d+)\\scolspan(\\d+)\\]",
                "<td rowspan='$2' colspan='$3'  style='width:$1%;border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\scolspan=(\\d+)\\]",
                "<td colspan='$1' style='border-left:1px solid #aaa;border-bottom:1px solid #aaa'>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[td\\srowspan=(\\d+)\\]",
                "<td rowspan='$1' style='border-left:1px solid #aaa;border-bottom:1px solid #aaa;'>")
        ret = ret.replaceAll("\\[td\\]", "<td style='border-left:1px solid #aaa;border-bottom:1px solid #aaa;'>")
        ret = ret.replaceAll("\\[/td\\]", "<td>")
        // [i][/i]
        ret = ret.replaceAll(ignoreCaseTag + "\\[i\\]",
                "<i style=\"font-style:italic\">")
        ret = ret.replaceAll(ignoreCaseTag + "\\[/i\\]", "</i>")
        // [del][/del]
        ret = ret.replaceAll(ignoreCaseTag + "\\[del\\]", "<del class=\"gray\">")
        ret = ret.replaceAll(ignoreCaseTag + "\\[/del\\]", "</del>")
        ret = ret.replaceAll(ignoreCaseTag + "\\[font=([^\\[|\\]]+)\\]",
                "<span style=\"font-family:$1\">")
        ret = ret.replaceAll(ignoreCaseTag + "\\[/font\\]", "</span>")
        // size
        ret = ret.replaceAll(ignoreCaseTag + "\\[size=(\\d+)%\\]",
                "<span style=\"font-size:$1%;line-height:$1%\">")
        ret = ret.replaceAll(ignoreCaseTag + "\\[/size\\]", "</span>")
        // [img]./ddd.jpg[/img]
        // if(showImage){
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[img\\]\\s*\\.(/[^\\[|\\]]+)\\s*\\[/img\\]",
                "<a href='https://" + HttpUtil.NGA_ATTACHMENT_HOST
                        + "/attachments$1'><img src='https://"
                        + HttpUtil.NGA_ATTACHMENT_HOST
                        + "/attachments$1' style= 'max-width:100%' ></a>")
        ret = ret.replaceAll(ignoreCaseTag
                        + "\\[img\\]\\s*(http[^\\[|\\]]+)\\s*\\[/img\\]",
                "<a href='$1'><img src='$1' style= 'max-width:100%' ></a>")
        ret = ForumBasicDecoder.decodeLists(ret)
        try {
            ret = buildImage(ret, showImage, imageUrls)
            ret = buildAudioHtml(ret)
            ret = buildVideoHtml(ret)
            ret = convertGifImage(ret)
        } catch (e: Exception) {
            // 保留已完成的转换
        }
        return ret
    }

    private fun imageBlock(src: String) = "<img src='$src' style= 'max-width:100%' >"

    private fun buildImage(source: String, showImage: Boolean, imageUrls: MutableList<String>?): String {
        var content = source
        val m = Pattern.compile("<img src='(http\\S+)' style= 'max-width:100%' >").matcher(source)
        while (m.find()) {
            val s0 = m.group()
            // 短消息、签名中的旧图片域名已不可用，换成可用域名；表情仍按路径使用内置图片。
            val s1 = ForumImageUrl.normalize(m.group(1)) ?: m.group(1)
            val path = EmoticonUtils.getPathByURI(s1) ?: EmoticonUtils.getPathByForumPath(ForumImageUrl.forumPath(s1))
            content = when {
                path != null -> content.replace(s0, imageBlock("file:///android_asset/$path"))
                !showImage -> content.replace(s0, imageBlock("file:///android_asset/ic_offline_image.png"))
                else -> {
                    if (s1.contains(HttpUtil.NGA_ATTACHMENT_HOST)) imageUrls?.add(s1)
                    content.replace(s0, imageBlock(s1))
                }
            }
        }
        return content
    }

    private fun convertGifImage(source: String): String {
        var content = source
        val matcher = Pattern.compile("(http\\S+).gif.(.*?).jpg").matcher(source)
        while (matcher.find()) {
            val s = matcher.group(0)
            content = content.replaceAll(s, s.substring(0, s.indexOf(".gif") + 4))
        }
        return content
    }

    private val acniangofubbcode = arrayOf(
        "blink", "goodjob", "上", "中枪", "偷笑", "冷", "凌乱", "反对", "吓", "吻", "呆", "咦", "哦", "哭", "哭1", "哭笑", "哼", "喘",
        "喷", "嘲笑", "嘲笑1", "囧", "委屈", "心", "忧伤", "怒", "怕", "惊", "愁", "抓狂", "抠鼻", "擦汗", "无语", "晕", "汗", "瞎", "羞",
        "羡慕", "花痴", "茶", "衰", "计划通", "赞同", "闪光", "黑枪",
    )

    private val acniangappadd = arrayOf(
        "-47218_5052bca81a77f.png", "-47218_5052bd3b4b3bd.png", "-1324875_50e597f5ce78d.png",
        "-47218_5052bcba15fcf.png", "-47218_5052bcb6e96d1.png", "-47218_5052bd2a0d49a.png",
        "-47218_5052c10aa0303.png", "-47218_5052bcaaacb45.png", "-1324875_50e597c090c58.png",
        "-47218_5052c104b8e27.png", "-47218_5052bc587c6f9.png", "-47218_5052c1076f119.png",
        "-47218_5052bd2497822.png", "-47218_5052bd2fa0790.png", "-47218_5052c0f6da079.png",
        "-47218_5052bc4cc6331.png", "-47218_5052bcf37c4c9.png", "-1324875_513394fbc54e1.gif",
        "-47218_5052bc4f51be7.png", "-47218_5052c1101747c.png", "-47218_5052c10d1f08c.png",
        "-47218_5052bcdd279bc.png", "-47218_5052bce27ab4d.png", "-47218_5052bd35aec58.png",
        "-47218_5052bcdfd9c69.png", "-47218_5052bc835856c.png", "-47218_5052bce4f2963.png",
        "-47218_5052bd330dfad.png", "-47218_5052bc7d91913.png", "-47218_5052c112b3b1b.png",
        "-47218_5052bcf0ba2db.png", "-47218_5052bc8638067.png", "-47218_5052bca55cb6e.png",
        "-47218_5052bc521c04b.png", "-47218_5052bca2a2f43.png", "-47218_5052bcad49530.png",
        "-47218_5052bceb823da.png", "-47218_5052bc80140e3.png", "-47218_5052bcb3b8944.png",
        "-1324875_50d841a63a673.png", "-47218_5052bcf68ddc2.png", "-1324875_50e597e9d6319.png",
        "-47218_5052bd27520ef.png", "-47218_5052bcbe35760.png", "-1324875_50e597f190a11.png",
    )

    private val newacniangofubbcode = arrayOf(
        "goodjob", "诶嘿", "偷笑", "怒", "笑", "那个…", "哦嗬嗬嗬", "舔", "鬼脸", "冷", "大哭", "哭", "恨", "中枪", "囧", "你看看你", "doge",
        "自戳双目", "偷吃", "冷笑", "壁咚", "不活了", "不明觉厉", "是在下输了", "你为猴这么", "干杯", "干杯2", "异议", "认真", "你已经死了", "你这种人…",
        "妮可妮可妮", "惊", "抢镜头", "yes", "有何贵干", "病娇", "lucky", "poi", "囧2", "威吓", "jojo立", "jojo立2", "jojo立3", "jojo立4",
        "jojo立5",
    )

    private val newacniangappadd = arrayOf(
        "a2_02.png", "a2_05.png", "a2_03.png", "a2_04.png", "a2_07.png", "a2_08.png", "a2_09.png", "a2_10.png",
        "a2_14.png", "a2_16.png", "a2_15.png", "a2_17.png", "a2_21.png", "a2_23.png", "a2_24.png", "a2_25.png",
        "a2_27.png", "a2_28.png", "a2_30.png", "a2_31.png", "a2_32.png", "a2_33.png", "a2_36.png", "a2_51.png",
        "a2_53.png", "a2_54.png", "a2_55.png", "a2_47.png", "a2_48.png", "a2_45.png", "a2_49.png", "a2_18.png",
        "a2_19.png", "a2_52.png", "a2_26.png", "a2_11.png", "a2_12.png", "a2_13.png", "a2_20.png", "a2_22.png",
        "a2_42.png", "a2_37.png", "a2_38.png", "a2_39.png", "a2_41.png", "a2_40.png",
    )

    private val penguinOfUBBCode = arrayOf(
        "战斗力", "哈啤", "满分", "衰", "拒绝", "心", "严肃", "吃瓜", "嘣", "嘣2", "冻", "谢", "哭", "响指", "转身",
    )

    private val penguinAppAdd = arrayOf(
        "pg01.png", "pg02.png", "pg03.png", "pg04.png", "pg05.png", "pg06.png", "pg07.png", "pg08.png", "pg09.png",
        "pg10.png", "pg11.png", "pg12.png", "pg13.png", "pg14.png", "pg15.png",
    )

    private val pstofubbcode = arrayOf(
        "举手", "亲", "偷笑", "偷笑2", "偷笑3", "傻眼", "傻眼2", "兔子", "发光", "呆", "呆2", "呆3", "呕", "呵欠", "哭", "哭2", "哭3", "嘲笑",
        "基", "宅", "安慰", "幸福", "开心", "开心2", "开心3", "怀疑", "怒", "怒2", "怨", "惊吓", "惊吓2", "惊呆", "惊呆2", "惊呆3", "惨", "斜眼",
        "晕", "汗", "泪", "泪2", "泪3", "泪4", "满足", "满足2", "火星", "牙疼", "电击", "看戏", "眼袋", "眼镜", "笑而不语", "紧张", "美味", "背",
        "脸红", "脸红2", "腐", "星星眼", "谢", "醉", "闷", "闷2", "音乐", "黑脸", "鼻血",
    )

    private val pstappadd = arrayOf(
        "pt00.png", "pt01.png", "pt02.png", "pt03.png", "pt04.png", "pt05.png", "pt06.png", "pt07.png", "pt08.png",
        "pt09.png", "pt10.png", "pt11.png", "pt12.png", "pt13.png", "pt14.png", "pt15.png", "pt16.png", "pt17.png",
        "pt18.png", "pt19.png", "pt20.png", "pt21.png", "pt22.png", "pt23.png", "pt24.png", "pt25.png", "pt26.png",
        "pt27.png", "pt28.png", "pt29.png", "pt30.png", "pt31.png", "pt32.png", "pt33.png", "pt34.png", "pt35.png",
        "pt36.png", "pt37.png", "pt38.png", "pt39.png", "pt40.png", "pt41.png", "pt42.png", "pt43.png", "pt44.png",
        "pt45.png", "pt46.png", "pt47.png", "pt48.png", "pt49.png", "pt50.png", "pt51.png", "pt52.png", "pt53.png",
        "pt54.png", "pt55.png", "pt56.png", "pt57.png", "pt58.png", "pt59.png", "pt60.png", "pt61.png", "pt62.png",
        "pt63.png", "pt64.png",
    )

    private val dtofubbcode = arrayOf(
        "ROLL", "上", "傲娇", "叉出去", "发光", "呵欠", "哭", "啃古头", "嘲笑", "心", "怒", "怒2", "怨", "惊", "惊2", "无语", "星星眼", "星星眼2",
        "晕", "注意", "注意2", "泪", "泪2", "烧", "笑", "笑2", "笑3", "脸红", "药", "衰", "鄙视", "闲", "黑脸",
    )

    private val dtappadd = arrayOf(
        "dt01.png", "dt02.png", "dt03.png", "dt04.png", "dt05.png", "dt06.png", "dt07.png", "dt08.png", "dt09.png",
        "dt10.png", "dt11.png", "dt12.png", "dt13.png", "dt14.png", "dt15.png", "dt16.png", "dt17.png", "dt18.png",
        "dt19.png", "dt20.png", "dt21.png", "dt22.png", "dt23.png", "dt24.png", "dt25.png", "dt26.png", "dt27.png",
        "dt28.png", "dt29.png", "dt30.png", "dt31.png", "dt32.png", "dt33.png",
    )

    private fun buildEmoticonImage(source: String): String {
        var content = source
        for (i in 0 until 45) {
            content = content.replaceAll(
                ignoreCaseTag + "\\[s:ac:" + acniangofubbcode[i] + "]",
                "<img src='file:///android_asset/acniang/" + acniangappadd[i] + "'>",
            )
        }
        for (i in 0 until 46) {
            content = content.replaceAll(
                ignoreCaseTag + "\\[s:a2:" + newacniangofubbcode[i] + "]",
                "<img src='file:///android_asset/newacniang/" + newacniangappadd[i] + "'>",
            )
        }
        for (i in penguinOfUBBCode.indices) {
            content = content.replaceAll(
                ignoreCaseTag + "\\[s:pg:" + penguinOfUBBCode[i] + "]",
                "<img src='file:///android_asset/pg/" + penguinAppAdd[i] + "' width=60 height=60>",
            )
        }
        for (i in 0 until 65) {
            content = content.replaceAll(
                ignoreCaseTag + "\\[s:pst:" + pstofubbcode[i] + "]",
                "<img src='file:///android_asset/pst/" + pstappadd[i] + "'>",
            )
        }
        for (i in 0 until 33) {
            content = content.replaceAll(
                ignoreCaseTag + "\\[s:dt:" + dtofubbcode[i] + "]",
                "<img src='file:///android_asset/dt/" + dtappadd[i] + "'>",
            )
        }
        return content
    }

    private fun buildAudioHtml(source: String): String {
        var content = source
        val pattern = Pattern.compile("\\[flash=audio](.*?)\\[/flash]")
        var matcher = pattern.matcher(content)
        while (matcher.find()) {
            var audioUrl = matcher.group()
            audioUrl = audioUrl.substring(14, audioUrl.indexOf("[/flash]") - 1)
            audioUrl = "<audio src=\"https://" + HttpUtil.NGA_ATTACHMENT_HOST + "/attachments" + audioUrl +
                "&filename=nga_audio.mp3\" controls=\"controls\"></audio>"
            content = matcher.replaceFirst(audioUrl)
            matcher = pattern.matcher(content)
        }
        return content
    }

    private fun buildVideoHtml(source: String): String {
        var content = source
        val pattern = Pattern.compile("\\[flash=video](.*?)\\[/flash]")
        var matcher = pattern.matcher(content)
        while (matcher.find()) {
            var url = matcher.group()
            url = url.substring("[flash=video]".length + 1, url.indexOf("[/flash]"))
            url = "<video src=\"https://" + HttpUtil.NGA_ATTACHMENT_HOST + "/attachments" + url + "\" controls=\"controls\"></video>"
            content = matcher.replaceFirst(url)
            matcher = pattern.matcher(content)
        }
        return content
    }

    @JvmStatic
    fun removeBrTag(s: String): String = s.replace("<br/><br/>", "\n").replace("<br/>", "\n")

    @JvmStatic
    fun unEscapeHtml(s: String?): String = StringHelper.unescapeHTML(s).orEmpty()

    /** [data] 中 [begPosition] 之后 [startStr] 与 [endStr] 之间的内容，以及其后的位置。 */
    @JvmStatic
    fun getStringBetween(data: String?, begPosition: Int, startStr: String?, endStr: String): StringFindResult {
        val ret = StringFindResult()
        if (data.isNullOrEmpty() || begPosition < 0 || data.length <= begPosition || startStr.isNullOrEmpty()) {
            return ret
        }
        var start = data.indexOf(startStr, begPosition)
        if (start == -1) return ret
        start += startStr.length
        var end = data.indexOf(endStr, start)
        if (end == -1) end = data.length
        ret.result = data.substring(start, end)
        ret.position = end + endStr.length
        return ret
    }

    /** 每个字节高位在前的二进制字符串。 */
    @JvmStatic
    fun toBinaryArray(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * Byte.SIZE_BITS)
        for (i in 0 until Byte.SIZE_BITS * bytes.size) {
            builder.append(if ((bytes[i / Byte.SIZE_BITS].toInt() shl i % Byte.SIZE_BITS) and 0x80 == 0) '0' else '1')
        }
        return builder.toString()
    }

    /** 地址中整数参数的值，缺失或无效时为 0。 */
    @JvmStatic
    fun getUrlParameter(url: String?, paraName: String): Int {
        if (url.isNullOrEmpty()) return 0
        val pattern = "$paraName="
        var start = url.indexOf(pattern)
        if (start == -1) return 0
        start += pattern.length
        var end = url.indexOf("&", start)
        if (end == -1) end = url.length
        val value = url.substring(start, end)
        return value.toIntOrNull() ?: run {
            NLog.e("getUrlParameter", "invalid url:$url")
            0
        }
    }

    @JvmStatic
    fun timeStamp2Date1(timeStamp: String): String = timeStamp2Date(timeStamp, "yyyy-MM-dd HH:mm:ss")

    @JvmStatic
    fun timeStamp2Date2(timeStamp: String): String = timeStamp2Date(timeStamp, "MM-dd HH:mm")

    /** 秒级时间戳按本地时区格式化。 */
    @JvmStatic
    fun timeStamp2Date(timeStamp: String, format: String): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeStamp.toLong() * 1000
        return SimpleDateFormat(format, Locale.getDefault()).format(calendar.time)
    }

    /** 以 UTF-8 读取 assets 中的文件，失败时返回空串。 */
    @JvmStatic
    fun getStringFromAssets(path: String): String = try {
        ContextUtils.getContext().assets.open(path).use { String(it.readBytes()) }
    } catch (e: java.io.IOException) {
        e.printStackTrace()
        ""
    }
}
