package gov.anzong.androidnga.util

import gov.anzong.androidnga.common.util.AppEnvironment

/** 论坛附件域名与头像缓存目录；网络请求统一经 Retrofit（NgaApi）或 NgaWriteClient。 */
object HttpUtil {

    const val NGA_ATTACHMENT_HOST = "img.nga.cn"

    @JvmField
    val PATH_AVATAR = AppEnvironment.externalStoragePictureDirectory + "/nga_cache/nga_cache"
}
