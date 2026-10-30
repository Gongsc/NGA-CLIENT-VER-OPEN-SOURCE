package gov.anzong.androidnga.core.corebuild

import gov.anzong.androidnga.core.data.HtmlData

/**
 * Created by Justwen on 2018/8/28.
 */
object HtmlBuilder {

    private val builders = listOf(HtmlCommentBuilder, HtmlAttachmentBuilder, HtmlSignatureBuilder, HtmlVoteBuilder)

    @JvmStatic
    fun build(builder: StringBuilder, htmlData: HtmlData, images: MutableList<String>) {
        for (build in builders) {
            builder.append(build.build(htmlData, images))
        }
    }
}
