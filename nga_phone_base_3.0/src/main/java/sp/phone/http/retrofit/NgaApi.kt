package sp.phone.http.retrofit

import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.QueryMap
import retrofit2.http.Url

/**
 * [RetrofitService] 的挂起函数版本，方法与之一一对应，迁移完成后取代它。
 *
 * 与 [RetrofitService] 共用 Retrofit 实例：`String` 响应按 GBK 解码，非 2xx 响应抛出 `HttpException`。
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

    @FormUrlEncoded
    @POST("nuke.php")
    suspend fun login(@FieldMap map: Map<String, String>): String

    @GET
    @Headers("Referer:https://bbs.ngacn.cc/nuke.php?__lib=login&__act=login_ui")
    suspend fun getAuthCodeImage(@Url url: String): ResponseBody

    @POST("nuke.php?__lib=login&__act=login&raw=3")
    @Headers(
        "Referer:https://bbs.ngacn.cc/nuke.php?__lib=login&__act=login_ui",
        "Content-Type:application/x-www-form-urlencoded",
        "Origin: https://bbs.ngacn.cc",
        "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.181 Safari/537.36",
        "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
        "Upgrade-Insecure-Requests:1",
    )
    suspend fun login(@Body body: FormBody): String

    @POST("nuke.php")
    @Headers(
        "Referer:https://bbs.ngacn.cc/nuke/p2.htm?login",
        "Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryklQov1cm1BhZqEM4",
    )
    suspend fun login(@Body multipartBody: MultipartBody): String

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
