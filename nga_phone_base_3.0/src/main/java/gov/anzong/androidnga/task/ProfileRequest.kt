package gov.anzong.androidnga.task

import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.network.bean.ProfileData
import gov.anzong.androidnga.network.retrofit.NgaApi

/** 读取个人资料；[query] 为 "uid=…" 或 "username=…"（GBK 编码），解析见 [ProfileParser]。 */
suspend fun requestProfile(query: String, api: NgaApi = NgaApi.get()): ProfileData {
    val headers = mapOf("Referer" to Utils.getNGAHost() + "nuke.php?func=ucp&lite=jsx&" + query)
    val url = Utils.getNGAHost() + "nuke.php?__lib=ucp&__act=get&lite=js&noprefix&" + query
    return ProfileParser.parse(api.get(url, headers)) ?: throw IllegalStateException("用户资料加载失败")
}
