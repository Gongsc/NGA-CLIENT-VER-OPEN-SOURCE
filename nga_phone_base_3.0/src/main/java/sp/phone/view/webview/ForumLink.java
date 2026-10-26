package sp.phone.view.webview;

import okhttp3.HttpUrl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** 依据实际域名和路径识别论坛链接，并解析正文中的相对地址。 */
public final class ForumLink {
    private static final Set<String> HOSTS =
            new HashSet<>(
                    Arrays.asList(
                            "bbs.nga.cn",
                            "ngabbs.com",
                            "www.ngabbs.com",
                            "bbs.ngacn.cc",
                            "nga.178.com",
                            "nga.donews.com"));

    public enum Target {
        ARTICLE,
        FORUM,
        PROFILE,
        IMAGE,
        EXTERNAL
    }

    public final HttpUrl url;
    public final Target target;

    private ForumLink(HttpUrl url, Target target) {
        this.url = url;
        this.target = target;
    }

    public static boolean isForum(HttpUrl url) {
        return url != null && HOSTS.contains(url.host().toLowerCase(Locale.ROOT));
    }

    public static ForumLink resolve(String input, String currentUrl, String defaultDomain) {
        if (input == null || input.trim().isEmpty() || input.trim().startsWith("#")) return null;
        String value = input.trim();
        HttpUrl base = HttpUrl.parse(currentUrl == null ? "" : currentUrl);
        if (!isForum(base)) base = HttpUrl.parse(defaultDomain);
        if (base == null) return null;
        for (String host : HOSTS) {
            if (value.equalsIgnoreCase(host)
                    || value.toLowerCase(Locale.ROOT).startsWith(host + "/")) {
                value = "https://" + value;
                break;
            }
        }
        HttpUrl url = base.resolve(value);
        if (url == null || !url.username().isEmpty() || !url.password().isEmpty()) return null;
        String path = url.encodedPath();
        Target target = Target.EXTERNAL;
        if (isForum(url)) {
            if ("/read.php".equals(path)) target = Target.ARTICLE;
            else if ("/thread.php".equals(path)) target = Target.FORUM;
            else if ("/nuke.php".equals(path)
                    && "ucp".equals(url.queryParameter("func"))
                    && url.queryParameter("username") != null) target = Target.PROFILE;
        }
        if (target == Target.EXTERNAL
                && path.toLowerCase(Locale.ROOT).matches(".*\\.(gif|jpe?g|png|bmp|webp)")) {
            target = Target.IMAGE;
        }
        return new ForumLink(url, target);
    }
}
