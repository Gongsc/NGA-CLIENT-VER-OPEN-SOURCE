package gov.anzong.androidnga.core.decode;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** 正文、附件和图库共用的论坛图片地址规则。 */
public final class ForumImageUrl {
    public static final String HOST = "img.nga.cn";
    private static final Set<String> LEGACY_HOSTS =
            new HashSet<>(
                    Arrays.asList(HOST, "img.nga.178.com", "img6.nga.178.com", "img.ngacn.cc"));

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
            if (LEGACY_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
                return "https://"
                        + HOST
                        + (uri.getRawPath() == null ? "" : uri.getRawPath())
                        + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())
                        + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment());
            }
            return uri.toASCIIString();
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
