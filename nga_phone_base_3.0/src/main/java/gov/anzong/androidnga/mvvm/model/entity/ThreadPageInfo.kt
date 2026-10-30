package gov.anzong.androidnga.mvvm.model.entity

/**
 * 主题列表中的一项，也用于浏览历史（以 JSON 保存，属性名不可更改）。
 */
class ThreadPageInfo {

    var tid = 0

    var historyVisitedAt = 0L

    var author: String? = null

    var fid = 0

    var authorId = 0

    var lastPoster: String? = null

    var replies = 0

    var subject: String? = null

    var titleFont: String? = null

    var type = 0

    var topicMisc: String? = null

    var page = 0

    var pid = 0

    var position = 0

    var isAnonymity = false

    var postDate = 0

    var replyInfo: ReplyInfo? = null

    /** 是否是版面镜像 */
    var isMirrorBoard = false
        private set

    var board: String? = null
        set(value) {
            field = value
            isMirrorBoard = "版面镜像" == value
        }

    /** 搜索回复时匹配到的回复。 */
    class ReplyInfo {

        var pidStr: String? = null

        var content: String? = null

        var subject: String? = null

        var postDate: String? = null

        var authorId: String? = null

        var tidStr: String? = null
    }

    override fun equals(other: Any?): Boolean = other is ThreadPageInfo && tid == other.tid && pid == other.pid

    override fun hashCode(): Int = 31 * tid + pid

    override fun toString(): String = "tid = $tid  pid = $pid"
}
