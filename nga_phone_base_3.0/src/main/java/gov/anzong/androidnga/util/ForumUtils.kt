package gov.anzong.androidnga.util

import android.content.Context
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.PreferenceKey

/**
 * Created by Justwen on 2018/7/2.
 */
object ForumUtils {

    /** 设置中选择的论坛域名，如 `https://bbs.nga.cn`。 */
    @JvmStatic
    fun getAvailableDomain(): String {
        val context = ContextUtils.getContext()
        val sp = context.getSharedPreferences(PreferenceKey.PERFERENCE, Context.MODE_PRIVATE)
        val index = sp.getString(PreferenceKey.KEY_NGA_DOMAIN, "1")!!.toInt()
        return context.resources.getStringArray(R.array.nga_domain)[index]
    }

    /** 子版块是否已订阅（显示）。 */
    @JvmStatic
    fun isBoardSubscribed(statusCode: Int): Boolean =
        // 3,810 返回false
        statusCode == 7 || statusCode == 558 || statusCode == 542 || statusCode == 2606 || statusCode == 2590 ||
            statusCode == 4654
}
