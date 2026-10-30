package gov.anzong.androidnga.core.decode

import gov.anzong.androidnga.core.data.HtmlData

/**
 * Created by Justwen on 2018/8/25.
 */
interface IForumDecoder {

    fun decode(content: String, htmlData: HtmlData?): String

    fun decode(content: String): String = decode(content, null)

    /** 最近一次解码时收集的图片地址。 */
    val imageUrls: List<String>?
        get() = null

    companion object {
        const val IGNORE_CASE_TAG = "(?i)"
    }
}
