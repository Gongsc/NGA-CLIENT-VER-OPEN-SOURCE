package gov.anzong.androidnga.core.corebuild

import gov.anzong.androidnga.common.util.FileUtils
import gov.anzong.androidnga.core.data.HtmlData
import gov.anzong.androidnga.core.decode.ForumDecoder

/**
 * 贴条。
 * Created by Justwen on 2018/8/28.
 */
object HtmlCommentBuilder : IHtmlBuild {

    private val template by lazy { FileUtils.readAssetToString("html/html_comment_template.html") }

    override fun build(htmlData: HtmlData, images: MutableList<String>): CharSequence {
        val comments = htmlData.commentList
        if (comments.isNullOrEmpty()) {
            return ""
        }
        val ret = StringBuilder()
        for (comment in comments) {
            val avatarUrl = comment.avatarUrl.takeUnless { it.isNullOrEmpty() } ?: "file:///android_asset/default_avatar.png"
            var content = comment.content.orEmpty()
            content = content.substring(content.indexOf("[/b]") + 4)
            content = ForumDecoder.decode(content, htmlData)
            ret.append(
                "<tr><td width='10%'> <img class='circle' src='$avatarUrl' />  " +
                    "<span style='font-weight:bold'>${comment.author} (${comment.postTime})</span>$content</td></tr>"
            )
        }
        return String.format(template, ret.toString())
    }
}
