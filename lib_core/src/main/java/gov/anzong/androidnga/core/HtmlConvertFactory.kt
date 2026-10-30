package gov.anzong.androidnga.core

import gov.anzong.androidnga.common.util.FileUtils
import gov.anzong.androidnga.core.corebuild.HtmlBuilder
import gov.anzong.androidnga.core.data.HtmlData
import gov.anzong.androidnga.core.decode.ForumDecoder

/** 楼层内容转换为正文网页。 */
object HtmlConvertFactory {

    private val template by lazy { FileUtils.readAssetToString("html/html_template.html") }

    @JvmStatic
    fun convert(htmlData: HtmlData, images: MutableList<String>): String {
        val builder = StringBuilder()
        if (htmlData.isInBackList) {
            builder.append("<h5>[屏蔽]</h5>")
        } else if (htmlData.alertInfo.isNullOrEmpty() && htmlData.rawData.isNullOrEmpty()) {
            builder.append("<h5>[隐藏]</h5>")
        } else {
            if (!htmlData.subject.isNullOrEmpty()) {
                builder.append("<div class='title'>${htmlData.subject}</div><br>")
            }
            var ngaHtml: String? = ForumDecoder.decode(htmlData.rawData, htmlData, images)
            if (ngaHtml.isNullOrEmpty()) {
                ngaHtml = htmlData.alertInfo
            }
            builder.append(ngaHtml)
            HtmlBuilder.build(builder, htmlData, images)
        }
        val style = if (htmlData.isDarkMode) "style_dark.css" else "style_light.css"
        return String.format(template, style, builder.toString())
    }
}
