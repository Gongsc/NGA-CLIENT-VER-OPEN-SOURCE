package gov.anzong.androidnga.network.bean

import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo

/** 主题详情的一页：楼层、热点回复与主题信息。 */
class ThreadData {

    var rowList: List<ThreadRowInfo>? = null

    var hotReplyList: List<ThreadRowInfo> = ArrayList()

    var threadInfo: ThreadPageInfo? = null

    var __F: Map<String, String>? = null

    var __ROWS = 0

    var rowNum = 0

    /** 从服务端获取的原始数据 */
    var rawData: String? = null
}
