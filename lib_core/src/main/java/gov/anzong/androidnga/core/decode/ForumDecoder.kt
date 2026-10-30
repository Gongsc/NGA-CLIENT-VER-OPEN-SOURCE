package gov.anzong.androidnga.core.decode

import gov.anzong.androidnga.core.data.HtmlData

/**
 * 依次执行基本格式、相册、表情与图片的解码。
 * Created by Justwen on 2018/8/25.
 */
object ForumDecoder {

    /** 解码器保存最近一次的图片地址，调用需串行。 */
    private val decoders = listOf(ForumBasicDecoder(), ForumAlbumDecoder(), ForumEmoticonDecoder(), ForumImageDecoder())

    @JvmStatic
    @JvmOverloads
    fun decode(rawData: String?, htmlData: HtmlData, urls: MutableList<String>? = null): String {
        var content = rawData.orEmpty()
        synchronized(decoders) {
            for (decoder in decoders) {
                content = decoder.decode(content, htmlData)
                decoder.imageUrls?.let { urls?.addAll(it) }
            }
        }
        return content
    }
}
