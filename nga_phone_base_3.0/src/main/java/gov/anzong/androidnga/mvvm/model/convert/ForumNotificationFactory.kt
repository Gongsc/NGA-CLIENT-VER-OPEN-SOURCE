package gov.anzong.androidnga.mvvm.model.convert

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonObject
import gov.anzong.androidnga.mvvm.model.entity.NotificationInfo
import gov.anzong.androidnga.mvvm.model.entity.RecentReplyInfo
import gov.anzong.androidnga.util.NLog

/** 解析论坛提醒接口：data.0 下 "0" 为回复提醒，"1" 为短消息提醒。 */
object ForumNotificationFactory {

    private const val SCRIPT_PREFIX = "window.script_muti_get_var_store="

    /** 提醒数据；没有任何通知时服务端不返回 data.0。 */
    private fun notifications(content: String): NgaJsonObject? =
        NgaJson.parseObject(content.removePrefix(SCRIPT_PREFIX))?.getJSONObject("data")?.getJSONObject("0")

    @JvmStatic
    fun buildRecentReplyList(content: String): List<RecentReplyInfo> {
        val infoList = ArrayList<RecentReplyInfo>()
        try {
            val obj = notifications(content) ?: return infoList
            // 部分响应不带 unread 字段，按无未读处理，避免整个列表解析失败。
            val unread = (obj.getInteger("unread") ?: 0) > 0
            val array = obj.getJSONArray("0") ?: return infoList
            for (i in array.indices) {
                infoList.add(buildRecentReplyInfo(array.getJSONObject(i)!!).apply { isUnread = unread })
            }
        } catch (e: Exception) {
            NLog.e("buildRecentReplyList error" + e.message)
        }
        return infoList
    }

    private fun buildRecentReplyInfo(obj: NgaJsonObject): RecentReplyInfo {
        val userName = obj["2"]!!.toString()
        val title = obj["5"]!!.toString()
        val tidStr = obj["6"]!!.toString()
        val pidStr = (if (obj.containsKey("7")) obj["7"] else obj["8"])!!.toString()
        val timeStamp = obj["9"]!!.toString()
        val uid = obj["1"]!!.toString()
        val type = obj["0"]!!.toString().toInt()
        return RecentReplyInfo(type, userName, title, pidStr, tidStr).apply {
            this.timeStamp = timeStamp
            userId = uid
        }
    }

    @JvmStatic
    fun buildNotificationList(content: String): List<NotificationInfo> {
        val infoList = ArrayList<NotificationInfo>()
        try {
            val obj = notifications(content) ?: return infoList
            val unread = obj.getInteger("unread")!! > 0
            obj.getJSONArray("0")?.let { array ->
                for (i in array.indices) {
                    infoList.add(buildRecentReplyInfo(array.getJSONObject(i)!!).apply { isUnread = unread })
                }
            }
            obj.getJSONArray("1")?.let { array ->
                for (i in array.indices) {
                    val message = array.getJSONObject(i)!!
                    infoList.add(NotificationInfo(message["0"]!!.toString().toInt(), message["2"]!!.toString()).apply { isUnread = unread })
                }
            }
        } catch (e: Exception) {
            NLog.e("buildNotificationList error " + e.message)
        }
        return infoList
    }
}
