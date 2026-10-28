package gov.anzong.androidnga.coroutines

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import sp.phone.http.retrofit.NgaApi

/** 测试用接口：记录每次请求，按 [respond] 返回合成响应或抛出异常。 */
class FakeNgaApi(var respond: suspend (request: String) -> String = { error("未设置响应：$it") }) : NgaApi {
    val requests = mutableListOf<String>()

    private suspend fun handle(request: String): String {
        requests += request
        return respond(request)
    }

    override suspend fun get(map: Map<String, String>) = handle("GET nuke.php ${map.toSortedMap()}")
    override suspend fun getByForum(map: Map<String, String>) = handle("GET forum.php ${map.toSortedMap()}")
    override suspend fun get(url: String) = handle("GET $url")
    override suspend fun post(url: String) = handle("POST $url")
    override suspend fun post(map: Map<String, String>) = handle("POST nuke.php ${map.toSortedMap()}")
    override suspend fun post(queryMap: Map<String, String>, fieldMap: Map<String, String>) =
        handle("POST nuke.php ${queryMap.toSortedMap()} ${fieldMap.toSortedMap()}")
    override suspend fun uploadFile(url: String, body: MultipartBody): ResponseBody = error("未使用")
    override suspend fun get(url: String, map: Map<String, String>) = handle("GET $url ${map.toSortedMap()}")
    override suspend fun getArticleResponse(url: String, headers: Map<String, String>): ResponseBody = error("未使用")
    override suspend fun forumFavorites(url: String, headers: Map<String, String>, fields: Map<String, String>): ResponseBody =
        error("未使用")
}
