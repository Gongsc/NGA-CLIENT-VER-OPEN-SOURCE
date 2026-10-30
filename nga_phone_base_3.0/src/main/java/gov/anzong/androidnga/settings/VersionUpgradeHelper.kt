package gov.anzong.androidnga.settings

import androidx.core.content.edit
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.common.R

/**
 * 旧版设置迁移。
 * @author yangyihang
 */
object VersionUpgradeHelper {

    @JvmStatic
    fun upgrade() {
        val sp = ContextUtils.getSharedPreferences(PreferenceKey.PERFERENCE)
        // 旧版的“非 Wi-Fi 下载”开关改为加载策略：开启为总是加载（0），关闭为仅 Wi-Fi（2）
        @Suppress("DEPRECATION")
        val migrations = mapOf(
            PreferenceKey.DOWNLOAD_AVATAR_NO_WIFI to R.string.pref_load_avatar_strategy,
            PreferenceKey.DOWNLOAD_IMG_NO_WIFI to R.string.pref_load_pic_strategy,
        )
        for ((oldKey, newKey) in migrations) {
            if (sp.contains(oldKey)) {
                val value = if (sp.getBoolean(oldKey, true)) "0" else "2"
                sp.edit {
                    putString(ContextUtils.getString(newKey), value)
                    remove(oldKey)
                }
            }
        }
    }
}
