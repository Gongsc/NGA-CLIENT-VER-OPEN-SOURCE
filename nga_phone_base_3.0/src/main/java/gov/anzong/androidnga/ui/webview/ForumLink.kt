package gov.anzong.androidnga.ui.webview

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

/** 依据实际域名和路径识别论坛链接，并解析正文中的相对地址。 */
class ForumLink private constructor(@JvmField val url: HttpUrl, @JvmField val target: Target) {

    enum class Target {
        ARTICLE,
        FORUM,
        PROFILE,
        IMAGE,
        EXTERNAL,
    }

    companion object {

        private val HOSTS = setOf(
            "bbs.nga.cn",
            "ngabbs.com",
            "www.ngabbs.com",
            "bbs.ngacn.cc",
            "nga.178.com",
            "nga.donews.com",
        )

        private val IMAGE_PATH = Regex(".*\\.(gif|jpe?g|png|bmp|webp)")

        @JvmStatic
        fun isForum(url: HttpUrl?): Boolean = url != null && url.host.lowercase(Locale.ROOT) in HOSTS

        @JvmStatic
        fun resolve(input: String?, currentUrl: String?, defaultDomain: String?): ForumLink? {
            // 与 Java 的 String.trim 一致，只去掉不大于空格的字符
            var value = input?.trim { it <= ' ' }
            if (value.isNullOrEmpty() || value.startsWith("#")) return null
            var base = currentUrl.orEmpty().toHttpUrlOrNull()
            if (!isForum(base)) base = defaultDomain?.toHttpUrlOrNull()
            if (base == null) return null
            val lower = value.lowercase(Locale.ROOT)
            if (HOSTS.any { value.equals(it, ignoreCase = true) || lower.startsWith("$it/") }) {
                value = "https://$value"
            }
            val url = base.resolve(value)
            if (url == null || url.username.isNotEmpty() || url.password.isNotEmpty()) return null
            val path = url.encodedPath
            var target = Target.EXTERNAL
            if (isForum(url)) {
                if ("/read.php" == path) {
                    target = Target.ARTICLE
                } else if ("/thread.php" == path) {
                    target = Target.FORUM
                } else if ("/nuke.php" == path && "ucp" == url.queryParameter("func") &&
                    url.queryParameter("username") != null
                ) {
                    target = Target.PROFILE
                }
            }
            if (target == Target.EXTERNAL && path.lowercase(Locale.ROOT).matches(IMAGE_PATH)) {
                target = Target.IMAGE
            }
            return ForumLink(url, target)
        }
    }
}
