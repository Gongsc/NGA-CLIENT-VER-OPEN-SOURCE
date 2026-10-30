package gov.anzong.androidnga.core.decode

import gov.anzong.androidnga.common.util.EmoticonUtils
import gov.anzong.androidnga.core.data.HtmlData
import java.util.regex.Matcher
import java.util.regex.Pattern

/** 图片 BBCode 和网页图片标签使用一致的地址、缩略图及图库规则。 */
class ForumImageDecoder : IForumDecoder {

    private val urls = ArrayList<String>()

    override val imageUrls: List<String>
        get() = urls

    override fun decode(content: String, htmlData: HtmlData?): String {
        val bbcode = BBCODE.matcher(content)
        val converted = StringBuffer()
        while (bbcode.find()) {
            val url = ForumImageUrl.normalize(bbcode.group(1))
            val image = if (url == null) {
                OFFLINE
            } else {
                "<a href='" + ForumImageUrl.attribute(ForumImageUrl.original(url)) + "'><img src='" +
                    ForumImageUrl.attribute(url) + "'></a>"
            }
            bbcode.appendReplacement(converted, Matcher.quoteReplacement(image))
        }
        bbcode.appendTail(converted)
        urls.clear()
        val images = IMAGE.matcher(converted.toString())
        val result = StringBuffer()
        while (images.find()) {
            val tag = images.group()
            val source = SOURCE.matcher(tag)
            if (!source.find()) continue
            val url = ForumImageUrl.normalize(source.group(2))
            var replacement = tag
            if (url != null && !url.startsWith("file:")) {
                val path = emoticonPath(url)
                if (path != null) {
                    replacement = "<img class='emoticon' src='file:///android_asset/$path'>"
                } else {
                    val original = ForumImageUrl.original(url)
                    if (!urls.contains(original)) urls.add(original)
                    replacement = if (htmlData != null && !htmlData.isShowImage) {
                        OFFLINE
                    } else {
                        tag.substring(0, source.start(2)) + ForumImageUrl.attribute(url) + tag.substring(source.end(2))
                    }
                }
            }
            images.appendReplacement(result, Matcher.quoteReplacement(replacement))
        }
        images.appendTail(result)
        val links = LINK.matcher(result.toString())
        val normalized = StringBuffer()
        while (links.find()) {
            val url = ForumImageUrl.normalize(links.group(3))
            val replacement = if (url == null) {
                links.group()
            } else {
                links.group(1) + links.group(2) + ForumImageUrl.attribute(ForumImageUrl.original(url)) + links.group(2)
            }
            links.appendReplacement(normalized, Matcher.quoteReplacement(replacement))
        }
        links.appendTail(normalized)
        return normalized.toString()
    }

    private companion object {
        val BBCODE: Pattern = Pattern.compile("(?i)\\[img]\\s*([^\\[\\]]+?)\\s*\\[/img]")
        val IMAGE: Pattern = Pattern.compile("(?i)<img\\b[^>]*>")
        val SOURCE: Pattern = Pattern.compile("(?i)\\bsrc\\s*=\\s*(['\"])(.*?)\\1")
        val LINK: Pattern = Pattern.compile("(?i)(<a\\b[^>]*\\bhref\\s*=\\s*)(['\"])(.*?)\\2")
        const val OFFLINE = "<img src='file:///android_asset/ic_offline_image.png'>"

        /** 旧帖以图片形式插入的表情使用内置图片；按路径匹配，与图片域名无关。 */
        fun emoticonPath(url: String): String? =
            EmoticonUtils.getPathByURI(url) ?: EmoticonUtils.getPathByForumPath(ForumImageUrl.forumPath(url))
    }
}
