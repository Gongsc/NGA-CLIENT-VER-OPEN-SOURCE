package gov.anzong.androidnga.base.util

import okhttp3.RequestBody
import okio.Buffer
import java.io.IOException
import java.util.regex.Pattern

/**
 * @author Justwen
 */
object StringUtils {

    private val patternMap = HashMap<String, Pattern>()

    @JvmStatic
    fun replaceAll(content: String, regex: String, replacement: String): String =
        getPattern(regex).matcher(content).replaceAll(replacement)

    @JvmStatic
    fun getPattern(regex: String): Pattern = patternMap.getOrPut(regex) { Pattern.compile(regex) }

    @JvmStatic
    fun requestBody2String(request: RequestBody?): String {
        try {
            if (request != null) {
                val buffer = Buffer()
                request.writeTo(buffer)
                return buffer.readUtf8()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    @JvmStatic
    fun isEmpty(content: String?): Boolean = content.isNullOrEmpty()
}
