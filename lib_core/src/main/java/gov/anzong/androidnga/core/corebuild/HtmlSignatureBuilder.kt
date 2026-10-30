package gov.anzong.androidnga.core.corebuild

import gov.anzong.androidnga.core.data.HtmlData
import gov.anzong.androidnga.core.decode.ForumDecoder

/**
 * @author Justwen
 * @date 2018/8/28
 */
object HtmlSignatureBuilder : IHtmlBuild {

    override fun build(htmlData: HtmlData, images: MutableList<String>): CharSequence {
        val signature = htmlData.signature
        return if (signature.isNullOrEmpty()) "" else "<br/></br>签名<hr/><br/>" + ForumDecoder.decode(signature, htmlData)
    }
}
