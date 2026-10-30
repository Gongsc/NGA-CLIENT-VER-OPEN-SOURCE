package gov.anzong.androidnga.mvvm.model.entity

/** 主题列表的一页：版块名、子版块与主题。 */
class TopicListInfo {

    var name: String? = null

    /** 服务器时间（秒）。 */
    @JvmField
    var curTime = 0

    val subBoardList = ArrayList<SubBoard>()

    private val threadPages = ArrayList<ThreadPageInfo>()

    var threadPageList: List<ThreadPageInfo>
        get() = threadPages
        set(value) {
            threadPages.clear()
            threadPages.addAll(value)
        }

    fun addThreadPage(threadPage: ThreadPageInfo) {
        threadPages.add(threadPage)
    }

    fun addSubBoard(subBoard: SubBoard) {
        subBoardList.add(subBoard)
    }
}
