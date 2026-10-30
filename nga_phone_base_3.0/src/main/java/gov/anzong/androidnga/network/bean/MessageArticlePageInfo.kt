package gov.anzong.androidnga.network.bean

/** 短消息会话中的一条消息。 */
class MessageArticlePageInfo {

    var subject: String? = null

    var time: String? = null

    var content: String? = null

    var from: String? = null

    var lou = 0

    /** 头像地址 */
    var js_escap_avatar: String? = null

    /** 用户名 */
    var author: String? = null

    /** 用户被禁言时为负数 */
    var yz: String? = null

    var mute_time: String? = null

    var signature: String? = null

    var formated_html_data: String? = null
}
