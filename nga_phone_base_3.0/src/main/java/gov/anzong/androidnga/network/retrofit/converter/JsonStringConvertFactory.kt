package gov.anzong.androidnga.network.retrofit.converter

import gov.anzong.androidnga.common.util.StreamUtils
import gov.anzong.androidnga.debug.Debugger
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

/**
 * 论坛页面默认以 GBK 解码为字符串。
 * Created by Justwen on 2017/10/10.
 */
class JsonStringConvertFactory private constructor() : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? =
        if (type == String::class.java) JsonStringConverter else super.responseBodyConverter(type, annotations, retrofit)

    private object JsonStringConverter : Converter<ResponseBody, String> {
        override fun convert(responseBody: ResponseBody): String = try {
            responseBody.byteStream().use { StreamUtils.toString(it, "GBK") }.also { Debugger.collectBody(it) }
        } catch (e: IOException) {
            ""
        }
    }

    companion object {
        @JvmStatic
        fun create(): JsonStringConvertFactory = JsonStringConvertFactory()
    }
}
