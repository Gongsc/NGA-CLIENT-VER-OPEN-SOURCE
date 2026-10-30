package gov.anzong.androidnga.core.decode

import java.net.URI
import java.util.Locale

/**
 * 正文、附件和图库共用的论坛图片地址规则。
 *
 * *.nga.178.com 图片域名已不可用（http 返回 502 或无响应，https 握手被断开），img.ngacn.cc /
 * img4.ngacn.cc 只会重定向到这些域名：附件改用 [HOST]，表情与版块图标等 ngabbs 路径改用 [STATIC_HOST]。
 */
object ForumImageUrl {

    const val HOST = "img.nga.cn"

    const val STATIC_HOST = "img4.nga.cn"

    private val LEGACY_HOSTS = setOf(HOST, "img.nga.178.com", "img6.nga.178.com", "img.ngacn.cc")

    private val LEGACY_STATIC_HOSTS = setOf(STATIC_HOST, "img4.nga.178.com", "img4.ngacn.cc")

    @JvmStatic
    fun normalize(input: String?): String? {
        // 与 Java 的 String.trim 一致，只去掉不大于空格的字符
        val trimmed = input?.trim { it <= ' ' }
        if (trimmed.isNullOrEmpty()) return null
        var value = trimmed.replace("&amp;", "&")
        if (value.startsWith("file:///android_asset/")) return value
        value = when {
            value.startsWith("./") -> "https://$HOST/attachments/" + value.substring(2)
            value.startsWith("mon_") -> "https://$HOST/attachments/$value"
            value.startsWith("/attachments/") -> "https://$HOST$value"
            value.startsWith("//") -> "https:$value"
            else -> value
        }
        try {
            val uri = URI.create(value)
            val scheme = uri.scheme
            val host = uri.host
            if (host == null || uri.userInfo != null ||
                !("https".equals(scheme, ignoreCase = true) || "http".equals(scheme, ignoreCase = true))
            ) {
                return null
            }
            val lower = host.lowercase(Locale.ROOT)
            val target = when (lower) {
                in LEGACY_HOSTS -> HOST
                in LEGACY_STATIC_HOSTS -> STATIC_HOST
                else -> return uri.toASCIIString()
            }
            return "https://" + target +
                (uri.rawPath ?: "") +
                (uri.rawQuery?.let { "?$it" } ?: "") +
                (uri.rawFragment?.let { "#$it" } ?: "")
        } catch (error: IllegalArgumentException) {
            return null
        }
    }

    /** 论坛图片域名（含已停用的旧域名）上的地址返回路径部分，如 `/attachments/mon_…`；其他地址返回 null。 */
    @JvmStatic
    fun forumPath(url: String?): String? {
        if (url == null) return null
        return try {
            val uri = URI.create(url.trim { it <= ' ' })
            val lower = uri.host?.lowercase(Locale.ROOT) ?: return null
            if (lower !in LEGACY_HOSTS && lower !in LEGACY_STATIC_HOSTS) null else uri.rawPath
        } catch (error: IllegalArgumentException) {
            null
        }
    }

    @JvmStatic
    fun original(url: String): String = url.replaceFirst(THUMBNAIL, ".$1")

    @JvmStatic
    fun attribute(value: String): String =
        value.replace("&", "&amp;").replace("'", "&#39;").replace("\"", "&quot;").replace("<", "&lt;")

    private val THUMBNAIL = Regex("(?i)\\.(png|jpe?g|webp|gif)\\.(?:thumb_s|medium|thumb|thumb_ss)\\.jpg(?=\\?|$)")
}
