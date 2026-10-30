package gov.anzong.androidnga.core.corebuild

import gov.anzong.androidnga.core.data.AttachmentData
import gov.anzong.androidnga.core.data.HtmlData
import gov.anzong.androidnga.core.decode.ForumImageUrl

/**
 * Created by Justwen on 2018/8/28.
 */
object HtmlAttachmentBuilder : IHtmlBuild {

    private fun buildMediaAttachment(ret: StringBuilder, attachment: AttachmentData, name: String) {
        ret.append("<tr><td><a href='https://")
            .append(ForumImageUrl.HOST)
            .append("/attachments/")
            .append(attachment.attachUrl)
            .append("'>")
            .append(name)
            .append("</a></td></tr>")
    }

    private fun buildImageAttachment(ret: StringBuilder, attachment: AttachmentData, index: Int, imageUrls: MutableList<String>) {
        val attachUrl = ForumImageUrl.normalize(attachment.attachUrl) ?: return
        val attachUrlThumb = if ("1" == attachment.thumb) "$attachUrl.thumb.jpg" else attachUrl
        ret.append("<tr><td>")
            .append("<button id='show$index' type='button' onclick='displayImg($index,\"$attachUrlThumb\")'>点击显示附件</button>")
            .append("<a href=$attachUrl>")
            .append("<img style='max-width:100%'; id='img$index'/>")
            .append("</a></td></tr>")
        if (!imageUrls.contains(attachUrl)) {
            imageUrls.add(attachUrl)
        }
    }

    override fun build(htmlData: HtmlData, images: MutableList<String>): CharSequence {
        val attachments = htmlData.attachmentList
        if (attachments.isNullOrEmpty()) {
            return ""
        }
        val ret = StringBuilder()
        ret.append("<br/><br/>附件<hr/><br/>")
        ret.append("<table style='border:1px solid #b9986e;padding:10px;color:#6b2d25;font-size:10'>")
        ret.append("<tbody>")
        var imageAttachmentCount = 0
        for (attach in attachments) {
            val attachUrl = attach.attachUrl.orEmpty()
            if (attachUrl.contains("mp3")) {
                buildMediaAttachment(ret, attach, "nga_audio.mp3")
            } else if (attachUrl.contains("mp4")) {
                buildMediaAttachment(ret, attach, "nga_video.mp4")
            } else {
                imageAttachmentCount++
                buildImageAttachment(ret, attach, imageAttachmentCount, images)
            }
        }
        if (imageAttachmentCount > 0) {
            ret.append("<script> function displayImg(a,b){ document.getElementById('img'+a).src=b; document.getElementById('show' + a).style.display='none'; } </script>")
        }
        ret.append("</tbody></table>")
        return ret
    }
}
