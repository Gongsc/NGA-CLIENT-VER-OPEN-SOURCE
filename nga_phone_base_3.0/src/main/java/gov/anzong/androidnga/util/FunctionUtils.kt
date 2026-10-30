package gov.anzong.androidnga.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import gov.anzong.androidnga.R
import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.core.data.HtmlData
import gov.anzong.androidnga.core.decode.ForumDecoder
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.network.bean.ThreadRowInfo
import gov.anzong.androidnga.ui.theme.ThemeManager
import gov.anzong.androidnga.ui.dialog.BaseDialogFragment
import gov.anzong.androidnga.ui.dialog.ReportDialogFragment
import gov.anzong.androidnga.ui.dialog.SignatureDialogFragment
import gov.anzong.androidnga.ui.dialog.VoteDialogFragment

/** 楼层操作、彩色字与外部打开等杂项。 */
object FunctionUtils {

    private val QUOTE_KEYWORDS = arrayOf(
        "[customachieve]" to "[/customachieve]",
        "[wow" to "]]",
        "[lol" to "]]",
        "[cnarmory" to "]",
        "[usarmory" to "]",
        "[twarmory" to "]",
        "[euarmory" to "]",
        "[url" to "[/url]",
        "[color=" to "[/color]",
        "[size=" to "[/size]",
        "[font=" to "[/font]",
        "[b]" to "[/b]",
        "[u]" to "[/u]",
        "[i]" to "[/i]",
        "[del]" to "[/del]",
        "[align=" to "[/align]",
        "[h]" to "[/h]",
        "[l]" to "[/l]",
        "[r]" to "[/r]",
        "[list" to "[/list]",
        "[img]" to "[/img]",
        "[album=" to "[/album]",
        "[code]" to "[/code]",
        "[code=lua]" to "[/code] lua",
        "[code=php]" to "[/code] php",
        "[code=c]" to "[/code] c",
        "[code=js]" to "[/code] javascript",
        "[code=xml]" to "[/code] xml/html",
        "[flash]" to "[/flash]",
        "[table]" to "[/table]",
        "[tid" to "[/tid]",
        "[pid" to "[/pid]",
        "[dice]" to "[/dice]",
        "[crypt]" to "[/crypt]",
        "[randomblock]" to "[/randomblock]",
        "[@" to "]",
        "[t.178.com/" to "]",
        "[collapse" to "[/collapse]",
    )

    private val COLORS = arrayOf(
        "[color=skyblue]",
        "[color=royalblue]",
        "[color=blue]",
        "[color=darkblue]",
        "[color=orange]",
        "[color=orangered]",
        "[color=crimson]",
        "[color=red]",
        "[color=firebrick]",
        "[color=darkred]",
        "[color=green]",
        "[color=limegreen]",
        "[color=seagreen]",
        "[color=teal]",
        "[color=deeppink]",
        "[color=tomato]",
        "[color=coral]",
        "[color=purple]",
        "[color=indigo]",
        "[color=burlywood]",
        "[color=sandybrown]",
        "[color=sienna]",
        "[color=chocolate]",
        "[color=silver]",
    )

    /** 标签的开头、结尾与跳过的长度。 */
    private val KEYWORDS = arrayOf(
        Triple("[customachieve]", "[/customachieve]", 16),
        Triple("[wow", "]]", 2),
        Triple("[lol", "]]", 2),
        Triple("[cnarmory", "]", 1),
        Triple("[usarmory", "]", 1),
        Triple("[twarmory", "]", 1),
        Triple("[euarmory", "]", 1),
        Triple("[url", "[/url]", 6),
        Triple("[size=", "]", 1),
        Triple("[/size]", "[/size]", 7),
        Triple("[font=", "]", 1),
        Triple("[/font]", "[/font]", 7),
        Triple("[b]", "[b]", 3),
        Triple("[/b]", "[/b]", 4),
        Triple("[u]", "[u]", 3),
        Triple("[/u]", "[/u]", 4),
        Triple("[i]", "[i]", 3),
        Triple("[/i]", "[/i]", 4),
        Triple("[del]", "[del]", 5),
        Triple("[/del]", "[/del]", 6),
        Triple("[align", "]", 1),
        Triple("[/align]", "[/align]", 8),
        Triple("[l]", "[l]", 3),
        Triple("[/l]", "[/l]", 4),
        Triple("[h]", "[h]", 3),
        Triple("[/h]", "[/h]", 4),
        Triple("[r]", "[r]", 3),
        Triple("[/r]", "[/r]", 4),
        Triple("[img]", "[/img]", 6),
        Triple("[album=", "[/album]", 8),
        Triple("[code]", "[/code]", 7),
        Triple("[code=lua]", "[/code] lua", 11),
        Triple("[code=php]", "[/code] php", 11),
        Triple("[code=c]", "[/code] c", 9),
        Triple("[code=js]", "[/code] javascript", 18),
        Triple("[code=xml]", "[/code] xml/html", 16),
        Triple("[flash]", "[/flash]", 8),
        Triple("[table]", "[table]", 7),
        Triple("[/table]", "[/table]", 8),
        Triple("[tid", "[/tid]", 6),
        Triple("[pid", "[/pid]", 6),
        Triple("[dice]", "[/dice]", 7),
        Triple("[crypt]", "[/crypt]", 8),
        Triple("[randomblock]", "[randomblock]", 13),
        Triple("[/randomblock]", "[/randomblock]", 14),
        Triple("[@", "]", 1),
        Triple("[t.178.com/", "]", 1),
        Triple("[tr]", "[tr]", 4),
        Triple("[/tr]", "[/tr]", 5),
        Triple("[td", "]", 1),
        Triple("[/td]", "[/td]", 5),
        Triple("[*]", "[*]", 3),
        Triple("[list", "]", 1),
        Triple("[/list]", "[/list]", 7),
        Triple("[collapse", "]", 1),
        Triple("[/collapse]", "[/collapse]", 11),
    )

    @JvmStatic
    fun openUrlByDefaultBrowser(context: Context, url: String?) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    @JvmStatic
    fun copyToClipboard(context: Context, text: String?) {
        val clipboardManager = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboardManager.setPrimaryClip(ClipData.newPlainText(text, text))
        ToastUtils.info(R.string.copied_to_clipboard)
    }

    /** 当前主题的正文背景色与文字色，形如 `ffffff`。 */
    private fun webColors(context: Context): Pair<String, String> {
        val theme = ThemeManager.getInstance()
        val bg = String.format("%06x", ContextCompat.getColor(context, theme.getBackgroundColor(0)) and 0xffffff)
        val fg = String.format("%06x", ContextCompat.getColor(context, theme.getForegroundColor()) and 0xffffff)
        return bg to fg
    }

    /** 显示楼层作者的签名。 */
    @JvmStatic
    fun showSignatureDialog(row: ThreadRowInfo, context: Context, fm: FragmentManager) {
        val (bg, fg) = webColors(context)
        val args = Bundle()
        args.putString("name", row.author)
        args.putString("html", signatureToHtmlText(row, fg, bg, context))
        BaseDialogFragment.show(fm, args, SignatureDialogFragment::class.java)
    }

    /** 显示楼层中的投票/投注。 */
    @JvmStatic
    fun showVoteDialog(row: ThreadRowInfo, context: Context, fm: FragmentManager) {
        val (bg, fg) = webColors(context)
        val args = Bundle()
        args.putString("html", voteToHtmlText(row, fg, bg))
        BaseDialogFragment.show(fm, args, VoteDialogFragment::class.java)
    }

    private fun signatureToHtmlText(row: ThreadRowInfo, fgColorStr: String, bgColorStr: String, context: Context): String {
        var ngaHtml: String? = ForumDecoder.decode(row.signature, HtmlData.create(row.signature, Utils.getNGAHost()))
        if (ngaHtml.isNullOrEmpty()) {
            ngaHtml = row.alterinfo
        }
        if (ngaHtml.isNullOrEmpty()) {
            ngaHtml = "<font color='red'>[" + context.getString(R.string.hide) + "]</font>"
        }
        return "<HTML> <HEAD><META   http-equiv=Content-Type   content= \"text/html;   charset=utf-8 \">" +
            "<body bgcolor= '#$bgColorStr'><font color='#$fgColorStr' size='2'>$ngaHtml</font></body>" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/html/script.js\"></script>"
    }

    private fun voteToHtmlText(row: ThreadRowInfo, fgColorStr: String, bgColorStr: String): String {
        if (row.vote.isNullOrEmpty()) return "本楼没有投票/投注内容"
        return "<!DOCTYPE html><html><head><meta http-equiv=Content-Type content=\"text/html;charset=utf-8\">" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/vote/vote.js\"></script>" +
            "<link rel=\"stylesheet\" type=\"text/css\" href=\"file:///android_asset/vote/vote.css\" /> </head>" +
            "<body style=\"color:#$fgColorStr\"bgcolor= '#$bgColorStr'><span id='votec'></span>" +
            "<script>vote(${row.tid},'${row.vote}')</script></body></html>"
    }

    /**
     * 头像字段可能是地址，也可能是包含多个头像的 JSON，如
     * `{ "t":1,"l":2,"0":{ "0":"http://pic2.178.com/…/a.jpg","cX":0.47,"cY":0.78}}`，取第一个地址。
     */
    @JvmStatic
    fun parseAvatarUrl(jsEscapAvatar: String?): String? {
        if (jsEscapAvatar == null) return null
        val start = jsEscapAvatar.indexOf("http")
        if (start == 0 || start == -1) return jsEscapAvatar
        var end = jsEscapAvatar.indexOf("\"", start)
        if (end == -1) end = jsEscapAvatar.length
        return jsEscapAvatar.substring(start, end)
    }

    /** 贴条没有附件、头像、等级与签名等楼层信息。 */
    @JvmStatic
    fun isComment(row: ThreadRowInfo): Boolean =
        row.alterinfo == null && row.attachs == null && row.comments == null && row.js_escap_avatar == null &&
            row.level == null && row.signature == null

    @JvmStatic
    fun handleReport(row: ThreadRowInfo, tid: Int, fm: FragmentManager) {
        val df = ReportDialogFragment()
        df.arguments = Bundle().apply {
            putInt("tid", tid)
            putInt("pid", row.pid)
        }
        df.show(fm, null)
    }

    /** 引用用的正文摘要：最多 99 个字符，去掉末尾未闭合的论坛代码。 */
    @JvmStatic
    fun checkContent(text: String): String {
        var content = text.trim { it <= ' ' }
        while (content.startsWith("\n")) {
            content = content.replaceFirst("\n", "")
        }
        var truncated = false
        if (content.length > 100) {
            content = content.substring(0, 99)
            truncated = true
        }
        for ((open, close) in QUOTE_KEYWORDS) {
            while (content.lowercase().lastIndexOf(open) > content.lowercase().lastIndexOf(close)) {
                content = content.substring(0, content.lowercase().lastIndexOf(open))
            }
        }
        if (truncated) {
            content += "......"
        }
        return content
    }

    private fun randomColor(): String = COLORS[(Math.random() * 23).toInt()]

    /** 彩色字：每个字随机颜色，论坛代码与引用保持原样。 */
    @JvmStatic
    fun ColorTxt(text: String): String {
        var bodyString = text
        while (bodyString.startsWith("\n")) {
            bodyString = bodyString.substring(1)
        }
        var existQuoteTxt = ""
        if (bodyString.lowercase().indexOf("[quote]") == 0) {
            existQuoteTxt = bodyString.substring(0, bodyString.lowercase().indexOf("[/quote]")) + "[/quote]"
            bodyString = bodyString.substring(bodyString.lowercase().indexOf("[/quote]") + 8)
        }
        var bslenth = bodyString.length
        // 遍历的是处理前的字符，与旧版一致
        val arrtxtchar = bodyString.toCharArray()
        var txtsendout = randomColor()
        var quotetxt: String
        var i = 0
        while (i < bslenth) {
            val ch = arrtxtchar[i]
            if (ch != '\n' && ch != '[' && ch != ' ') {
                // 开始就是普通文字的话就直接加彩色字体了
                txtsendout += ch + "[/color]" + randomColor()
            } else if (ch == '[') {
                // 首字符是[要判断
                if (bodyString.lowercase().indexOf("[quote]", i - 1) == i) {
                    if (bodyString.lowercase().indexOf("[quote]", i - 1) > bodyString.lowercase().indexOf("[/quote]", i - 1)) {
                        // 引用没有结束
                        quotetxt = bodyString.substring(i + 7)
                        if (quotetxt.lowercase().lastIndexOf("[") >= 0) {
                            quotetxt = quotetxt.substring(0, quotetxt.lowercase().lastIndexOf("["))
                        }
                        while (quotetxt.endsWith(".")) {
                            quotetxt = quotetxt.substring(0, quotetxt.length - 1)
                        }
                        bslenth = bodyString.length
                        txtsendout = txtsendout.substring(0, txtsendout.lowercase().lastIndexOf("[color"))
                        quotetxt = "[quote]" + checkContent(quotetxt) + "[/quote]"
                        txtsendout += quotetxt + randomColor()
                        break
                    } else {
                        quotetxt = bodyString.substring(i + 7, bodyString.lowercase().indexOf("[/quote]", i))
                        while (quotetxt.endsWith(".")) {
                            quotetxt = quotetxt.substring(0, quotetxt.length - 1)
                        }
                        txtsendout = txtsendout.substring(0, txtsendout.lowercase().lastIndexOf("[color"))
                        quotetxt = "[quote]" + checkContent(quotetxt) + "[/quote]"
                        txtsendout += quotetxt + randomColor()
                        i = bodyString.lowercase().indexOf("[/quote]", i) + 7
                    }
                } else if (bodyString.lowercase().indexOf("[color", i - 1) == i) {
                    if (bodyString.lowercase().indexOf("[/color]", i) >= 0) {
                        txtsendout += bodyString.substring(bodyString.indexOf("]", i) + 1, bodyString.lowercase().indexOf("[/color]", i) + 8) +
                            randomColor()
                        i = bodyString.indexOf("[/color]", i) + 7
                    } else {
                        bodyString = bodyString.substring(0, i) + bodyString.substring(bodyString.lowercase().indexOf("]", i) + 1, bslenth)
                        i = bodyString.indexOf("]", i)
                    }
                } else {
                    for ((open, close, skip) in KEYWORDS) {
                        if (bodyString.lowercase().indexOf(open, i - 1) == i) {
                            if (bodyString.lowercase().indexOf(close, i) >= 0) {
                                txtsendout = txtsendout.substring(0, txtsendout.lowercase().lastIndexOf("[color"))
                                txtsendout += bodyString.substring(i, bodyString.lowercase().indexOf(close, i)) + close + randomColor()
                                i = bodyString.lowercase().indexOf(close, i) + skip - 1
                            } else {
                                val itmp = bodyString.indexOf("]", i)
                                bodyString = bodyString.substring(0, i) + bodyString.substring(bodyString.lowercase().indexOf("]", i) + 1, bslenth)
                                i = itmp
                            }
                            break
                        }
                    }
                }
            } else {
                // 空格与换行
                txtsendout = txtsendout.substring(0, txtsendout.lowercase().lastIndexOf("[color"))
                txtsendout += bodyString.substring(i, i + 1) + randomColor()
            }
            i++
        }
        if (txtsendout.lowercase().lastIndexOf("[color") >= 0) {
            txtsendout = txtsendout.substring(0, txtsendout.lowercase().lastIndexOf("[color"))
        }
        return existQuoteTxt + txtsendout.replace("&nbsp;", " ").trim { it <= ' ' }
    }

    /** 设置中开启彩色字时转换正文。 */
    @JvmStatic
    fun ColorTxtCheck(text: String): String =
        if (PhoneConfiguration.getInstance().isShowColorText) ColorTxt(text.trim { it <= ' ' }) else text

    @JvmStatic
    fun share(context: Context, title: String?, content: String?) {
        try {
            val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, content)
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: ActivityNotFoundException) {
            ToastUtils.error("分享失败！")
        }
    }
}
