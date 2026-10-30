package gov.anzong.androidnga.core.data

/** 一个楼层转换为 HTML 所需的内容与显示设置。 */
class HtmlData(var rawData: String?) {

    var isInBackList = false

    var isDarkMode = false

    var subject: String? = null

    var alertInfo: String? = null

    var textSize = 0
        set(value) {
            field = value
            tableTextSize = (value * 0.9).toInt()
        }

    var emotionSize = 0

    var tableTextSize = 0
        private set

    var isShowImage = true

    var vote: String? = null

    var signature: String? = null

    @get:JvmName("getNGAHost")
    @set:JvmName("setNGAHost")
    var ngaHost: String? = null

    var commentList: List<CommentData>? = null

    var attachmentList: List<AttachmentData>? = null

    companion object {
        @JvmStatic
        fun create(rawData: String?, host: String?): HtmlData = HtmlData(rawData).apply { ngaHost = host }
    }
}
