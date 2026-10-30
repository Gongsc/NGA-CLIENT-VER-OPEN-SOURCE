package gov.anzong.androidnga.core.decode

import gov.anzong.androidnga.core.data.HtmlData

/**
 * [album=标题]图片…[/album] 转换为图片列表。
 * Created by Justwen on 2018/8/25.
 */
class ForumAlbumDecoder : IForumDecoder {

    override fun decode(content: String, htmlData: HtmlData?): String {
        val quoteStyle = if (htmlData != null && htmlData.isDarkMode) {
            "<div style='background:#000000;padding:5px;border:1px solid #888' >"
        } else {
            "<div style='background:#E8E8E8;padding:5px;border:1px solid #888' >"
        }
        var result = content
        var startPos = result.indexOf("[album=")
        var endPos = result.indexOf("[/album]") + 8
        while (startPos in 0 until endPos) {
            val up = result.substring(0, startPos)
            val down = result.substring(endPos)
            val album = result.substring(startPos, endPos)
            val title = "相册列表:" + album.replace(ALBUM, "$1") + "<br/>"
            var images = album.replace(ALBUM, "$2")
            if (images.startsWith("<br/>")) {
                images = "[img]" + images.substring(5) + "[/img]"
            }
            images = images.replace("<br/>", "[/img]<br/><br/>[img]")
            result = "$up<br/>$quoteStyle$title<br/>$images</div>$down"
            startPos = result.indexOf("[album=")
            endPos = result.indexOf("[/album]") + 8
        }
        return result
    }

    private companion object {
        val ALBUM = Regex("(?i)\\[album=(.*?)\\](.*?)\\[/album\\]")
    }
}
