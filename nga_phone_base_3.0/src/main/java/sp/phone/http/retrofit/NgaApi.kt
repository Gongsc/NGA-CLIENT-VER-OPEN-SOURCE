package sp.phone.http.retrofit

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * 论坛接口（挂起函数），取代原先基于 RxJava 的 RetrofitService。
 *
 * 使用 [RetrofitHelper] 创建的实例：`String` 响应按 GBK 解码，非 2xx 响应抛出 `HttpException`。
 */
interface NgaApi {

    @GET("nuke.php")
    suspend fun get(@QueryMap map: Map<String, String>): String

    @GET("forum.php")
    suspend fun getByForum(@QueryMap map: Map<String, String>): String

    @GET
    suspend fun get(@Url url: String): String

    @POST
    suspend fun post(@Url url: String): String

    @FormUrlEncoded
    @POST("nuke.php")
    suspend fun post(@FieldMap map: Map<String, String>): String

    @FormUrlEncoded
    @POST("nuke.php")
    suspend fun post(@QueryMap queryMap: Map<String, String>, @FieldMap fieldMap: Map<String, String>): String

    @POST
    suspend fun uploadFile(@Url url: String, @Body body: MultipartBody): ResponseBody

    @GET
    suspend fun get(@Url url: String, @HeaderMap map: Map<String, String>): String

    @GET
    suspend fun getArticleResponse(@Url url: String, @HeaderMap headers: Map<String, String>): ResponseBody

    @FormUrlEncoded
    @POST
    suspend fun forumFavorites(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @FieldMap fields: Map<String, String>,
    ): ResponseBody

    companion object {
        /** 每次调用时取当前域名对应的实例，切换域名后立即生效。 */
        @JvmStatic
        fun get(): NgaApi = RetrofitHelper.getInstance().getService(NgaApi::class.java) as NgaApi
    }
}
