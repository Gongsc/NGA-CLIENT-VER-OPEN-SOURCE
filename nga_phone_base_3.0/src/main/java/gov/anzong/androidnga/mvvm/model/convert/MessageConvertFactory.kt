package gov.anzong.androidnga.mvvm.model.convert

import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonObject
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.network.bean.MessageDetailInfo
import gov.anzong.androidnga.network.bean.MessageListInfo
import gov.anzong.androidnga.network.bean.MessageThreadPageInfo
import gov.anzong.androidnga.util.HttpUtil
import gov.anzong.androidnga.util.MessageUtil
import gov.anzong.androidnga.util.NLog
import gov.anzong.androidnga.util.StringUtils

/**
 * 短消息列表与会话内容的响应解析；失败时返回 null，原因见 [errorMsg]。
 * Created by Justwen on 2017/10/10.
 */
class MessageConvertFactory {

    var errorMsg: String? = ""
        private set

    /** 去掉脚本包装与错误附注，并把数字形式的标题与正文改为字符串。 */
    private fun normalize(response: String): String {
        var js = response.replace(Regex("window.script_muti_get_var_store="), "")
        if (js.indexOf("/*error fill content") > 0) js = js.substring(0, js.indexOf("/*error fill content"))
        return js.replace(Regex("\"content\":\\+(\\d+),"), "\"content\":\"+$1\",")
            .replace(Regex("\"subject\":\\+(\\d+),"), "\"subject\":\"+$1\",")
            .replace("/*\$js\$*/", "")
    }

    /** 响应中的 data；没有时从 error 中取得原因。 */
    private fun data(js: String): NgaJsonObject? {
        try {
            (NgaJson.parseObject(js)!!["data"] as NgaJsonObject?)?.let { return it }
        } catch (e: Exception) {
            NLog.e(TAG, "can not parse :\n$js")
        }
        var error: NgaJsonObject? = null
        try {
            error = NgaJson.parseObject(js)!!["error"] as NgaJsonObject?
        } catch (e: Exception) {
            NLog.e(TAG, "can not parse :\n$js")
        }
        errorMsg = error?.getString("0").takeUnless { it.isNullOrEmpty() } ?: "请重新登录"
        return null
    }

    fun getMessageListInfo(response: String?): MessageListInfo? {
        if (response.isNullOrEmpty()) {
            return null
        }
        val o = data(normalize(response)) ?: return null
        val o1 = o["0"] as NgaJsonObject?
        if (o1 == null) {
            errorMsg = "请重新登录"
            return null
        }
        val ret = MessageListInfo()
        ret.__nextPage = o1.getIntValue("nextPage")
        ret.__currentPage = o1.getIntValue("currentPage")
        ret.__rowsPerPage = o1.getIntValue("rowsPerPage")
        val messageEntryList = ArrayList<MessageThreadPageInfo>()
        var rowObj = o1["0"] as NgaJsonObject?
        var i = 1
        while (rowObj != null) {
            try {
                val entry = MessageThreadPageInfo()
                entry.mid = rowObj.getInteger("mid")!!
                entry.posts = rowObj.getInteger("posts")!!
                entry.subject = rowObj.getString("subject")
                entry.from_username = rowObj.getString("from_username")
                entry.last_from_username = rowObj.getString("last_from_username")
                val time = rowObj.getInteger("time")!!
                entry.time = if (time > 0) StringUtils.timeStamp2Date1(time.toString()) else ""
                val lastModify = rowObj.getIntValue("last_modify")
                entry.lastTime = if (lastModify > 0) StringUtils.timeStamp2Date1(lastModify.toString()) else ""
                messageEntryList.add(entry)
            } catch (e: Exception) {
                // 跳过无法解析的会话
            }
            rowObj = o1[(i++).toString()] as NgaJsonObject?
        }
        ret.messageEntryList = messageEntryList
        return ret
    }

    fun getMessageDetailInfo(response: String?, page: Int): MessageDetailInfo? {
        if (response == null) {
            errorMsg = ContextUtils.getString(R.string.network_error)
            return null
        }
        // 短消息图片的相对地址补全为附件域名；img6.nga.178.com 已不可用。
        val js = normalize(response)
            .replace(Regex("\\[img\\]./mon_"), "[img]https://" + HttpUtil.NGA_ATTACHMENT_HOST + "/attachments/mon_")
        data(js) ?: return null
        return MessageUtil.parseJsonThreadPage(js, page)
    }

    private companion object {
        const val TAG = "MessageConvertFactory"
    }
}
