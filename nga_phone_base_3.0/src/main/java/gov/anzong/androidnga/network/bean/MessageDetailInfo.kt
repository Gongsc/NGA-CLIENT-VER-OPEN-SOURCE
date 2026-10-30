package gov.anzong.androidnga.network.bean

/** 短消息会话内容的一页。 */
class MessageDetailInfo {

    var __nextPage = 0

    var __currentPage = 0

    var _Alluser: String? = null

    var _Title: String? = null

    var messageEntryList: List<MessageArticlePageInfo> = emptyList()
}
