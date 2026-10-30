package gov.anzong.androidnga.util

import java.security.MessageDigest

object MD5Util {

    /** 小写十六进制的 MD5 摘要。 */
    @JvmStatic
    fun MD5(plainText: String): String =
        MessageDigest.getInstance("MD5").digest(plainText.toByteArray()).joinToString("") { "%02x".format(it) }
}
