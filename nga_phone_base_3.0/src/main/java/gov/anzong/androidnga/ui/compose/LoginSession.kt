package gov.anzong.androidnga.ui.compose

import java.net.URI
import java.net.URLDecoder

data class LoginSession(val uid: String, val cid: String, val userName: String)

fun parseLoginSession(cookies: String?): LoginSession? {
    val fields = cookies.orEmpty().split(';').mapNotNull { cookie ->
        val separator = cookie.indexOf('=')
        if (separator < 0) null else cookie.substring(0, separator).trim() to cookie.substring(separator + 1).trim()
    }.toMap()
    val uid = fields["ngaPassportUid"]?.takeIf { it.toLongOrNull()?.let { id -> id > 0 } == true } ?: return null
    val cid = fields["ngaPassportCid"]?.takeIf { it.isNotBlank() } ?: return null
    val encodedName = fields["ngaPassportUrlencodedUname"]?.takeIf { it.isNotBlank() } ?: return null
    val name = runCatching {
        val decoded = URLDecoder.decode(encodedName, "GBK")
        if (Regex("%[0-9a-fA-F]{2}").containsMatchIn(decoded)) URLDecoder.decode(decoded.replace("+", "%2B"), "GBK") else decoded
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
    return LoginSession(uid, cid, name)
}

fun isNgaLoginUrl(url: String): Boolean = runCatching {
    val uri = URI(url)
    val host = uri.host?.lowercase() ?: return false
    uri.scheme.equals("https", ignoreCase = true) &&
        listOf("ngabbs.com", "nga.cn", "ngacn.cc", "nga.178.com").any { host == it || host.endsWith(".$it") }
}.getOrDefault(false)
