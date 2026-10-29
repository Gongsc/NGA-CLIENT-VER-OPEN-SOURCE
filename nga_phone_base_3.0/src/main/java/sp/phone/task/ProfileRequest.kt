package sp.phone.task

import gov.anzong.androidnga.Utils
import sp.phone.http.bean.ProfileData
import sp.phone.http.retrofit.NgaApi

/** 读取个人资料；[query] 为 "uid=…" 或 "username=…"（GBK 编码），解析见 [ProfileParser]。 */
suspend fun requestProfile(query: String, api: NgaApi = NgaApi.get()): ProfileData {
    val headers = mapOf("Referer" to Utils.getNGAHost() + "nuke.php?func=ucp&lite=jsx&" + query)
    val url = Utils.getNGAHost() + "nuke.php?__lib=ucp&__act=get&lite=js&noprefix&" + query
    return ProfileParser.parse(api.get(url, headers)) ?: throw IllegalStateException("用户资料加载失败")
}
