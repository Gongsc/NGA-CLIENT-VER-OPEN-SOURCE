package gov.anzong.androidnga.network.bean

/** 短消息会话列表的一页。 */
class MessageListInfo {

    var __rowsPerPage = 0

    var __nextPage = 0

    var __currentPage = 0

    var messageEntryList: List<MessageThreadPageInfo> = emptyList()
}
