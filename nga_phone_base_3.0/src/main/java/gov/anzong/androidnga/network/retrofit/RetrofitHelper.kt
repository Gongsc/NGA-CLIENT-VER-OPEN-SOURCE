package gov.anzong.androidnga.network.retrofit

import android.content.Context
import android.content.SharedPreferences
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.base.util.StringUtils
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.debug.Debugger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.network.retrofit.converter.JsonStringConvertFactory
import gov.anzong.androidnga.util.ForumUtils
import gov.anzong.androidnga.util.NLog
import java.net.URLDecoder

/**
 * 论坛读取请求的 Retrofit 实例；设置中更换域名后重新创建。
 * Created by Justwen on 2017/10/10.
 */
class RetrofitHelper private constructor() {

    private var baseUrl: String = ForumUtils.getAvailableDomain()

    private var retrofit: Retrofit = createRetrofit()

    /** SharedPreferences 只弱引用监听器，需要保存在字段中。 */
    private val domainListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PreferenceKey.KEY_NGA_DOMAIN) {
            baseUrl = ForumUtils.getAvailableDomain()
            retrofit = createRetrofit()
        }
    }

    init {
        ContextUtils.getContext().getSharedPreferences(PreferenceKey.PERFERENCE, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(domainListener)
    }

    @JvmOverloads
    fun createRetrofit(builder: OkHttpClient.Builder? = null): Retrofit = createRetrofit(baseUrl, builder)

    fun createRetrofit(baseUrl: String, builder: OkHttpClient.Builder?): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(JsonStringConvertFactory.create())
            .client((builder ?: createOkHttpClientBuilder()).build())
            .build()

    /** 未指定时附带当前账号的 Cookie 与官方客户端的 User-Agent。 */
    fun createOkHttpClientBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Cookie", original.header("Cookie") ?: UserManagerImpl.getInstance().cookie.orEmpty())
                .header("User-Agent", original.header("User-Agent") ?: "Nga_Official/80023")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .addInterceptor { chain ->
            var request = chain.request()
            try {
                if (request.method.equals("post", ignoreCase = true)) {
                    val body = URLDecoder.decode(StringUtils.requestBody2String(request.body), "utf-8")
                    if (body.contains("charset=gbk")) {
                        request = request.newBuilder()
                            .post(body.toRequestBody("application/x-www-form-urlencoded;charset=GBK".toMediaType()))
                            .build()
                    }
                }
            } catch (e: Exception) {
                NLog.e(e.message)
            }
            chain.proceed(request)
        }
        .addInterceptor { chain ->
            Debugger.collectRequest(chain.request())
            chain.proceed(chain.request())
        }

    fun <T> getService(service: Class<T>): T = retrofit.create(service)

    companion object {

        private val INSTANCE by lazy { RetrofitHelper() }

        @JvmStatic
        fun getInstance(): RetrofitHelper = INSTANCE
    }
}
