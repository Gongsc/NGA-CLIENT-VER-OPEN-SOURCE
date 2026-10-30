package gov.anzong.androidnga.task

import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonObject
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.network.bean.AdminForumsData
import gov.anzong.androidnga.network.bean.ProfileData
import gov.anzong.androidnga.network.bean.ReputationData
import gov.anzong.androidnga.util.NLog
import gov.anzong.androidnga.util.StringUtils

/** 个人资料响应解析，自 JsonProfileLoadTask 原样拆出。 */
internal object ProfileParser {

    private const val TAG = "ProfileParser"

    private const val DEFAULT_VALUE = "N/A"

    /** 解析个人资料响应；失败时抛出带提示文字的 IllegalStateException。 */
    @JvmStatic
    fun parse(response: String?): ProfileData {
        if (response.isNullOrEmpty()) {
            throw IllegalStateException(ContextUtils.getString(R.string.network_error))
        }
        var js = response.replace(Regex("window.script_muti_get_var_store="), "")
        if (js.contains("/*error fill content")) {
            js = js.substring(0, js.indexOf("/*error fill content"))
                .replace(Regex("\"content\":\\+(\\d+),"), "\"content\":\"+$1\",")
        }
        js = js.replace(Regex("\"subject\":\\+(\\d+),"), "\"subject\":\"+$1\",").replace("/*\$js\$*/", "")
        val obj = NgaJson.parseObject(js)!!
        if (obj.containsKey("data")) {
            try {
                val dataObj = obj.getJSONObject("data")!!.getJSONObject("0")!!
                return ProfileData().apply {
                    buildBasicInfo(this, dataObj)
                    buildReputation(this, dataObj.getJSONObject("reputation"))
                    buildAdminForums(this, dataObj.getJSONObject("adminForums"))
                }
            } catch (e: Exception) {
                NLog.e(TAG, "can not parse :\n$js")
            }
        }
        throw IllegalStateException(obj.getJSONObject("error")?.getString("0") ?: "二哥玩坏了或者你需要重新登录")
    }

    private fun buildAdminForums(ret: ProfileData, obj: NgaJsonObject?) {
        if (obj != null) {
            ret.adminForums = obj.keys.map { AdminForumsData(it, obj.getString(it)) }
        }
    }

    private fun buildReputation(ret: ProfileData, obj: NgaJsonObject?) {
        if (obj != null) {
            ret.reputationEntryList = (0 until obj.size).mapNotNull { i ->
                obj.getJSONObject(i.toString())?.let { ReputationData(it.getString("0"), it.getString("1")) }
            }
        }
    }

    private fun NgaJsonObject.text(key: String, default: String = DEFAULT_VALUE): String =
        getString(key).takeUnless { it.isNullOrEmpty() } ?: default

    private fun buildBasicInfo(ret: ProfileData, obj: NgaJsonObject) {
        ret.money = obj.text("money", "0")
        ret.frame = obj.text("fame")
        ret.postCount = obj.text("posts")
        ret.emailAddress = obj.text("email")
        ret.phoneNumber = obj.text("phone")
        ret.userName = obj.text("username")
        ret.uid = obj.text("uid")
        ret.memberGroup = obj.text("group")
        // 状态规则与论坛网页一致，见 ProfileStatus
        ret.active = ProfileStatus.active(obj.getInteger("verified"), obj.getLong("bit"))
        val muteText = ProfileStatus.muteText(obj.getLongValue("muteTime"), obj.getLongValue("maxBuffMute")) { time ->
            StringUtils.timeStamp2Date(time.toString(), "yyyy-MM-dd HH:mm")
        }
        ret.isMuted = muteText != null
        ret.mutedTime = muteText
        ret.buffs = ProfileStatus.buffDescriptions(obj["buffs"])
        val regdate = obj.getString("regdate")
        ret.registerDate = if (regdate.isNullOrEmpty()) DEFAULT_VALUE else StringUtils.timeStamp2Date1(regdate)
        ret.sign = obj.text("sign", "无签名")
        ret.avatarUrl = obj.getString(PreferenceKey.PREFERENCE_AVATAR)
    }
}
