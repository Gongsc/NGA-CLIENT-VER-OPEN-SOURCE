package gov.anzong.androidnga.util

import java.nio.charset.Charset

/**
 * 按指定字符集对 URL 参数进行百分号编码；字母、数字、`_-!.~'()*` 与 [encode] 的 allow 中的字符保持原样。
 * Created by Administrator on 13-12-3.
 */
object UriEncoderWithCharset {

    private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

    @JvmStatic
    fun encode(s: String?, allow: String?, charset: String): String? {
        if (s == null) {
            return null
        }
        val encoded = StringBuilder()
        var current = 0
        while (current < s.length) {
            var nextToEncode = current
            while (nextToEncode < s.length && isAllowed(s[nextToEncode], allow)) {
                nextToEncode++
            }
            if (nextToEncode == s.length) {
                return if (current == 0) s else encoded.append(s, current, s.length).toString()
            }
            encoded.append(s, current, nextToEncode)
            current = nextToEncode
            var nextAllowed = current + 1
            while (nextAllowed < s.length && !isAllowed(s[nextAllowed], allow)) {
                nextAllowed++
            }
            for (b in s.substring(current, nextAllowed).toByteArray(Charset.forName(charset))) {
                encoded.append('%')
                encoded.append(HEX_DIGITS[(b.toInt() and 0xf0) shr 4])
                encoded.append(HEX_DIGITS[b.toInt() and 0xf])
            }
            current = nextAllowed
        }
        // s 为空串时没有需要编码的内容
        return if (encoded.isEmpty()) s else encoded.toString()
    }

    private fun isAllowed(c: Char, allow: String?): Boolean =
        c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || "_-!.~'()*".indexOf(c) >= 0 ||
            (allow != null && allow.indexOf(c) >= 0)
}
