package sp.phone.task

import gov.anzong.androidnga.base.json.NgaJsonArray
import gov.anzong.androidnga.base.json.NgaJsonObject

/**
 * 用户资料中的账号状态，规则与论坛网页（common_res/js_ucp.js、js_commonui.js 的 activeInfo）一致：
 * - 激活状态优先按 `bit` 标志判断，没有 `bit` 时按旧字段 `verified`；
 * - 禁言只看 `muteTime`（全站）与 `maxBuffMute`（部分区域）；
 * - `buffs` 是道具、称号等各类状态，网页单独列为“状态”，与禁言无关。
 */
object ProfileStatus {

    enum class Level { NORMAL, WARNING, BANNED }

    class Active(val label: String, val description: String, val level: Level)

    private val unactivated = Active("未激活", "账号未激活", Level.WARNING)
    private val phone = Active("已激活", "已绑定手机激活", Level.NORMAL)
    private val nuke = Active("NUKED", "因违反版规而被禁用", Level.BANNED)
    private val lock = Active("锁定", "因违规注册或发布违规广告等情况而被禁用", Level.BANNED)
    private val pass = Active("锁定", "因账号安全问题被禁用，修改密码可解锁", Level.BANNED)
    private val reset = Active("锁定", "因账号安全问题被禁用，重置密码可解锁", Level.BANNED)
    private val manual = Active("锁定", "因账号被盗或发布违规广告等情况被禁用，只能人工解锁", Level.BANNED)
    private val link = Active("关联", "此账号关联到了其他的账号激活", Level.NORMAL)
    private val discard = Active("注销", "此账号已注销", Level.BANNED)

    /** 激活状态；两个字段都没有时返回 null。 */
    @JvmStatic
    fun active(verified: Int?, bit: Long?): Active? {
        if (bit != null) {
            val state = bit and 14680064L
            return when {
                bit and 16777216L != 0L -> discard
                state != 0L -> when (state) {
                    4194304L -> nuke
                    12582912L -> lock
                    10485760L -> manual
                    8388608L -> reset
                    6291456L -> pass
                    else -> lock
                }
                bit and 1048576L != 0L -> phone
                bit and 536870912L != 0L -> link
                else -> unactivated
            }
        }
        return when (verified ?: return null) {
            1 -> Active("已激活", "已使用激活码激活", Level.NORMAL)
            0 -> unactivated
            4 -> phone
            5 -> Active("已激活", "已绑定微信激活", Level.NORMAL)
            -1 -> nuke
            -5 -> lock
            -4 -> pass
            -3 -> reset
            -2 -> manual
            else -> if (verified < 0) Active("锁定", "", Level.BANNED) else Active("已激活", "", Level.NORMAL)
        }
    }

    /** 禁言说明：全站禁言优先，其次为部分区域禁言；未禁言返回 null。[format] 把秒级时间戳转为文字。 */
    @JvmStatic
    fun muteText(muteTime: Long, maxBuffMute: Long, format: (Long) -> String): String? = when {
        muteTime > 0 -> "禁言至 ${format(muteTime)}"
        maxBuffMute > 0 -> "在某些区域禁言至 ${format(maxBuffMute)}"
        else -> null
    }

    /** 各项状态的说明文字（每项的第 9 个字段），去掉其中的网页标签。 */
    @JvmStatic
    fun buffDescriptions(buffs: Any?): List<String> {
        val entries = when (buffs) {
            is NgaJsonObject -> buffs.values.toList()
            is NgaJsonArray -> buffs.toList()
            else -> emptyList()
        }
        return entries.mapNotNull { entry ->
            val text = when (entry) {
                is NgaJsonObject -> entry.getString("9")
                is NgaJsonArray -> if (entry.size > 9) entry.getString(9) else null
                else -> null
            }
            text?.replace(Regex("(?i)<br\\s*/?>"), " ")?.replace(Regex("<[^>]+>"), "")
                ?.replace("&nbsp;", " ")?.replace("&amp;", "&")?.replace("&lt;", "<")?.replace("&gt;", ">")
                ?.trim()?.takeIf { it.isNotEmpty() }
        }
    }
}
