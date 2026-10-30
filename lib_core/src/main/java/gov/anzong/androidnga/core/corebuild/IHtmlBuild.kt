package gov.anzong.androidnga.core.corebuild

import gov.anzong.androidnga.core.data.HtmlData

/** 正文之后的附加内容，如贴条、附件、签名与投票提示。 */
fun interface IHtmlBuild {

    fun build(htmlData: HtmlData, images: MutableList<String>): CharSequence
}
