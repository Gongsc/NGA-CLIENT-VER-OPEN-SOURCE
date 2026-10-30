package gov.anzong.androidnga.network.bean

/**
 * 每一行的内容。按接口字段名精确绑定，部分访问器名称沿用旧版以保持绑定结果。
 */
class ThreadRowInfo {

    var tid = 0

    var fid = 0
        private set

    /** 用户名 */
    var author: String? = null

    var authorid = 0

    var subject: String? = null

    var vote: String? = null

    var postdate: String? = null

    var pid = 0

    @get:JvmName("getISANONYMOUS")
    @set:JvmName("setISANONYMOUS")
    var isAnonymous = false

    /** 如“编辑于……” */
    var alterinfo: String? = null

    var content: String? = null

    var lou = 0

    var attachs: Map<String, Attachment>? = null

    var level: String? = null

    /** 用户被禁言时为负数 */
    var yz: String? = null

    /** 头像地址 */
    var js_escap_avatar: String? = null

    var muteTime: String? = null

    /** 威望 */
    var aurvrc = 0

    var signature: String? = null

    var comments: List<ThreadRowInfo>? = null

    /** 热门回复 */
    @JvmField
    var hotReplies: MutableList<String>? = null

    @get:JvmName("get_isInBlackList")
    @set:JvmName("set_IsInBlackList")
    var isInBlackList = false

    var formattedHtmlData: String? = null

    var fromClient: String? = null

    var fromClientModel: String? = null

    var isMuted = false

    var postCount: String? = null

    var reputation = 0f

    var memberGroup: String? = null

    var score = 0

    var score_2 = 0

    private val imageUrlList = ArrayList<String>()

    val imageUrls: List<String>
        get() = imageUrlList

    fun addImageUrl(url: String) {
        imageUrlList.add(url)
    }
}
