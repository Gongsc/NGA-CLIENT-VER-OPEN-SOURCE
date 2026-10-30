package gov.anzong.androidnga.util

import androidx.core.content.ContextCompat
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonObject
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.network.bean.MessageArticlePageInfo
import gov.anzong.androidnga.network.bean.MessageDetailInfo
import gov.anzong.androidnga.ui.theme.ThemeManager

/**
 * 短消息会话内容的解析与正文网页。
 */
object MessageUtil {

    private const val TAG = "MessageUtil"

    /** 解析会话的一页；无法解析时返回 null。 */
    @JvmStatic
    fun parseJsonThreadPage(response: String, page: Int): MessageDetailInfo? {
        var js = response.replace(Regex("\"content\":\\+(\\d+),"), "\"content\":\"+$1\",")
            .replace(Regex("\"subject\":\\+(\\d+),"), "\"subject\":\"+$1\",")
            .replace(Regex("\"content\":(0\\d+),"), "\"content\":\"$1\",")
            .replace(Regex("\"subject\":(0\\d+),"), "\"subject\":\"$1\",")
            .replace(Regex("\"author\":(0\\d+),"), "\"author\":\"$1\",")
        val start = "\"__P\":{\"aid\":"
        val end = "\"this_visit_rows\":"
        if (js.indexOf(start) != -1 && js.indexOf(end) != -1) {
            NLog.w(TAG, "here comes an invalid response")
            js = js.substring(0, js.indexOf(start)) + js.substring(js.indexOf(end))
        }
        var o: NgaJsonObject? = null
        try {
            o = NgaJson.parseObject(js)!!["data"] as NgaJsonObject?
        } catch (e: Exception) {
            NLog.e(TAG, "can not parse :\n$js")
        }
        val o1 = o?.get("0") as NgaJsonObject? ?: return null
        val userInfoMap = o1["userInfo"] as NgaJsonObject?
        // 现在的接口把消息放在 allmsgs 中（字段不变），旧接口直接放在 data.0 下。
        val rows = o1["allmsgs"] as? NgaJsonObject ?: o1
        val messageEntryList = convertToList(rows, userInfoMap, page)
        val data = MessageDetailInfo()
        data.messageEntryList = messageEntryList
        data.__currentPage = o1.getIntValue("currentPage")
        data.__nextPage = o1.getIntValue("nextPage")
        // allUsers 为“uid 用户名”交替排列，以空格或制表符分隔
        val users = o1.getString("allUsers").orEmpty().replace('\t', ' ').split(" ").dropLastWhile { it.isEmpty() }
        data._Alluser = (1 until users.size step 2).joinToString(",") { users[it] }
        if (messageEntryList.isNotEmpty()) {
            data._Title = messageEntryList[0].subject.orEmpty()
        }
        return data
    }

    private fun convertToList(rowMap: NgaJsonObject, userInfoMap: NgaJsonObject?, page: Int): List<MessageArticlePageInfo> {
        val result = ArrayList<MessageArticlePageInfo>()
        var rowObj = rowMap["0"] as NgaJsonObject?
        var i = 1
        while (rowObj != null) {
            val row = MessageArticlePageInfo()
            row.content = rowObj.getString("content")
            row.lou = 20 * (page - 1) + i
            row.subject = rowObj.getString("subject")
            val time = rowObj.getIntValue("time")
            row.time = if (time > 0) StringUtils.timeStamp2Date1(time.toString()) else ""
            row.from = rowObj.getString("from")
            fillUserInfo(row, userInfoMap)
            fillFormattedHtmlData(row, i)
            result.add(row)
            rowObj = rowMap[(i++).toString()] as NgaJsonObject?
        }
        return result
    }

    private fun fillUserInfo(row: MessageArticlePageInfo, userInfoMap: NgaJsonObject?) {
        val from = row.from ?: return
        val userInfo = userInfoMap?.get(from) as? NgaJsonObject ?: return
        row.author = userInfo.getString("username")
        row.js_escap_avatar = userInfo.getString("avatar")
        row.yz = userInfo.getString("yz")
        row.mute_time = userInfo.getString("mute_time")
        row.signature = userInfo.getString("signature")
    }

    private fun fillFormattedHtmlData(row: MessageArticlePageInfo, i: Int) {
        if (row.content == null) {
            row.content = row.subject
            row.subject = null
        }
        val context = ContextUtils.getContext()
        val theme = ThemeManager.getInstance()
        val bgColor = String.format("%06x", ContextCompat.getColor(context, theme.getBackgroundColor(i)) and 0xffffff)
        val fgColor = String.format("%06x", ContextCompat.getColor(context, theme.getForegroundColor()) and 0xffffff)
        row.formated_html_data = convertToHtmlText(row, PhoneConfiguration.getInstance().isImageLoadEnabled, fgColor, bgColor)
    }

    private fun convertToHtmlText(row: MessageArticlePageInfo, showImage: Boolean, fgColorStr: String, bgColorStr: String): String {
        var ngaHtml = StringUtils.decodeForumTag(row.content, showImage, 0, null)
        if (ngaHtml.isNullOrEmpty()) {
            ngaHtml = "<font color='red'>[" + ContextUtils.getString(R.string.hide) + "]</font>"
        }
        return "<HTML> <HEAD><META   http-equiv=Content-Type   content= \"text/html;   charset=utf-8 \">" +
            buildHeader(row, fgColorStr) +
            "<body bgcolor= '#$bgColorStr'><font color='#$fgColorStr' size='2'>$ngaHtml</font></body>"
    }

    private fun buildHeader(row: MessageArticlePageInfo, fgColorStr: String): String {
        val subject = row.subject
        return if (subject.isNullOrEmpty()) "" else "<h4 style='color:$fgColorStr' >$subject</h4>"
    }
}
