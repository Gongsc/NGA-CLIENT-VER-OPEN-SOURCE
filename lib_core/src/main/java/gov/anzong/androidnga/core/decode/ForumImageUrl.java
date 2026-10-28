package gov.anzong.androidnga.core.decode;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 正文、附件和图库共用的论坛图片地址规则。
 *
 * <p>*.nga.178.com 图片域名已不可用（http 返回 502 或无响应，https 握手被断开），img.ngacn.cc /
 * img4.ngacn.cc 只会重定向到这些域名：附件改用 {@link #HOST}，表情与版块图标等 ngabbs 路径改用 {@link #STATIC_HOST}。
 */
public final class ForumImageUrl {
    public static final String HOST = "img.nga.cn";
    public static final String STATIC_HOST = "img4.nga.cn";
    private static final Set<String> LEGACY_HOSTS =
            new HashSet<>(
                    Arrays.asList(HOST, "img.nga.178.com", "img6.nga.178.com", "img.ngacn.cc"));
    private static final Set<String> LEGACY_STATIC_HOSTS =
            new HashSet<>(Arrays.asList(STATIC_HOST, "img4.nga.178.com", "img4.ngacn.cc"));

    private ForumImageUrl() {}

    public static String normalize(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String value = input.trim().replace("&amp;", "&");
        if (value.startsWith("file:///android_asset/")) return value;
        if (value.startsWith("./"))
            value = "https://" + HOST + "/attachments/" + value.substring(2);
        else if (value.startsWith("mon_")) value = "https://" + HOST + "/attachments/" + value;
        else if (value.startsWith("/attachments/")) value = "https://" + HOST + value;
        else if (value.startsWith("//")) value = "https:" + value;
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null
                    || uri.getUserInfo() != null
                    || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)))
                return null;
            String lower = host.toLowerCase(Locale.ROOT);
            String target =
                    LEGACY_HOSTS.contains(lower)
                            ? HOST
                            : LEGACY_STATIC_HOSTS.contains(lower) ? STATIC_HOST : null;
            if (target != null) {
                return "https://"
                        + target
                        + (uri.getRawPath() == null ? "" : uri.getRawPath())
                        + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())
                        + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment());
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    /** 论坛图片域名（含已停用的旧域名）上的地址返回路径部分，如 {@code /attachments/mon_…}；其他地址返回 null。 */
    public static String forumPath(String url) {
        if (url == null) return null;
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            if (host == null) return null;
            String lower = host.toLowerCase(Locale.ROOT);
            if (!LEGACY_HOSTS.contains(lower) && !LEGACY_STATIC_HOSTS.contains(lower)) return null;
            return uri.getRawPath();
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    public static String original(String url) {
        return url.replaceFirst(
                "(?i)\\.(png|jpe?g|webp|gif)\\.(?:thumb_s|medium|thumb|thumb_ss)\\.jpg(?=\\?|$)",
                ".$1");
    }

    static String attribute(String value) {
        return value.replace("&", "&amp;")
                .replace("'", "&#39;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;");
    }
}
