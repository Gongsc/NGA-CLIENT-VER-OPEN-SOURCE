package gov.anzong.androidnga

import gov.anzong.androidnga.util.ForumUtils

/**
 * Created by liuboyu on 2015/8/25.
 */
object Utils {

    /** 当前论坛域名，以 `/` 结尾。 */
    @JvmStatic
    fun getNGAHost(): String = ForumUtils.getAvailableDomain() + "/"
}
