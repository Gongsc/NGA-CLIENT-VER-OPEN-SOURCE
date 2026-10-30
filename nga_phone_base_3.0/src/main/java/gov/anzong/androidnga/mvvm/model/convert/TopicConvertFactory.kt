package gov.anzong.androidnga.mvvm.model.convert

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.settings.FilterKeywordsManagerImpl
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.network.bean.TopicListBean
import gov.anzong.androidnga.mvvm.model.entity.SubBoard
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.mvvm.model.entity.TopicListInfo
import gov.anzong.androidnga.util.ForumUtils
import gov.anzong.androidnga.util.NLog
import gov.anzong.androidnga.util.StringUtils

/**
 * 主题列表响应转换为 [TopicListInfo]：子版块、主题、排序与屏蔽。
 * Created by Justwen on 2017/11/21.
 */
class TopicConvertFactory {

    /** 无法解析时返回 null。 */
    fun getTopicListInfo(response: String, page: Int): TopicListInfo? {
        val js = response.removePrefix("window.script_muti_get_var_store=")
        val topicListBean = NgaJson.parseObject(js, TopicListBean::class.java)
        return try {
            val bean = topicListBean!!
            val listInfo = TopicListInfo()
            convertSubBoard(listInfo, bean)
            convertTopic(listInfo, bean, page)
            listInfo.curTime = bean.time
            listInfo.name = bean.data!!.__F!!.name
            sort(listInfo)
            filter(listInfo)
            listInfo
        } catch (e: NullPointerException) {
            NLog.e(TAG, "can not parse :\n$js")
            null
        }
    }

    /** 去掉标题含屏蔽关键词或作者在黑名单中的主题。 */
    private fun filter(data: TopicListInfo) {
        val blackList = UserManagerImpl.getInstance().blackList
        val keywords = FilterKeywordsManagerImpl.getInstance().keywords
        data.threadPageList = data.threadPageList.filterNot { pageInfo ->
            keywords.any { keyword ->
                val text = keyword.keyword
                keyword.isEnabled && text != null && pageInfo.subject?.contains(text) == true
            } || blackList.any { it.nickName == pageInfo.author }
        }
    }

    private fun sort(listInfo: TopicListInfo) {
        if (PhoneConfiguration.getInstance().needSortByPostOrder()) {
            listInfo.threadPageList = listInfo.threadPageList.sortedByDescending { it.postDate }
        }
        listInfo.subBoardList.sortByDescending { it.fid }
    }

    private fun convertSubBoard(listInfo: TopicListInfo, topicListBean: TopicListBean) {
        try {
            val forum = topicListBean.data!!.__F!!
            val subForums = forum.sub_forums
            if (subForums.isNullOrEmpty()) {
                return
            }
            val subBoardMap = NgaJson.parseObject(subForums)!!
            for ((key, value) in subBoardMap) {
                val boardMap = value as Map<*, *>
                val board = SubBoard()
                val id = boardMap["0"].toString().toInt()
                if (key.startsWith("t")) board.stid = id else board.fid = id
                // 有些子版块的fid的key是3，大部分都是1
                if (boardMap.containsKey("3")) {
                    board.tidStr = boardMap["3"].toString()
                    board.type = 1
                } else {
                    board.type = 0
                }
                board.parentFidStr = forum.fid.toString()
                board.name = boardMap["1"] as String?
                board.description = boardMap["2"] as String?
                if (boardMap.containsKey("4")) {
                    board.isChecked = ForumUtils.isBoardSubscribed(boardMap["4"].toString().toInt())
                } else {
                    board.type = -1
                    board.isChecked = true
                }
                listInfo.addSubBoard(board)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun convertTopic(listInfo: TopicListInfo, topicListBean: TopicListBean, page: Int) {
        val map = topicListBean.data!!.__T!!
        for (count in 0 until map.size) {
            val tBean = map[count.toString()]
            if (tBean == null || filterTopic(topicListBean, tBean)) {
                continue
            }
            val pageInfo = ThreadPageInfo()
            val author = tBean.author!!
            if (author.startsWith("#anony_")) {
                pageInfo.isAnonymity = true
                pageInfo.author = anonymityName(author)
            } else {
                pageInfo.authorId = tBean.authorid!!.toInt()
                pageInfo.author = author
            }
            pageInfo.lastPoster = tBean.lastposter
            pageInfo.subject = tBean.subject
            pageInfo.replies = tBean.replies
            pageInfo.type = tBean.type
            pageInfo.topicMisc = tBean.topic_misc
            pageInfo.titleFont = tBean.titlefont
            val tpcUrl = tBean.tpcurl
            pageInfo.tid = if (tpcUrl != null && tpcUrl.contains("tid")) StringUtils.getUrlParameter(tpcUrl, "tid") else tBean.tid
            pageInfo.page = page
            tBean.__P?.let { pBean ->
                pageInfo.pid = pBean.pid
                pageInfo.replyInfo = ThreadPageInfo.ReplyInfo().apply {
                    authorId = pBean.authorid
                    content = pBean.content
                    postDate = pBean.postdate.toString()
                    pidStr = pBean.pid.toString()
                    tidStr = pageInfo.tid.toString()
                    subject = pageInfo.subject
                }
            }
            tBean.parent?.let { pageInfo.board = it["2"] }
            pageInfo.postDate = tBean.postdate
            val topicMiscVar = tBean.topic_misc_var
            if (topicMiscVar != null && pageInfo.isMirrorBoard) {
                topicMiscVar["3"]?.let { pageInfo.fid = it.toInt() }
            }
            listInfo.addThreadPage(pageInfo)
        }
    }

    /** 开启“屏蔽子版块”时，隐藏网事杂谈中置顶的子版块入口。 */
    private fun filterTopic(topicListBean: TopicListBean, tBean: TopicListBean.DataBean.TBean): Boolean {
        val forum = topicListBean.data!!.__F
        if (forum != null && PhoneConfiguration.getInstance().needFilterSubBoard() && forum.fid == -7 && tBean.recommend > 9) {
            NLog.d("屏蔽固定的渣帖子 $tBean")
            return true
        }
        return false
    }

    private fun anonymityName(author: String): String {
        val prefix = AnonymousName.STEMS
        val suffix = AnonymousName.SURNAMES
        val sb = StringBuilder()
        var i = 6
        for (j in 0 until 6) {
            if (j == 0 || j == 3) {
                val pos = author.substring(i + 1, i + 2).toInt(16).coerceIn(0, prefix.length - 1)
                sb.append(prefix[pos])
            } else {
                val pos = author.substring(i, i + 2).toInt(16).coerceIn(0, suffix.length - 1)
                sb.append(suffix[pos])
            }
            i += 2
        }
        return sb.toString()
    }

    private companion object {
        const val TAG = "TopicConvertFactory"
    }
}
