package gov.anzong.androidnga.common.util;

/**
 * 文件名解析，替代 commons-io 2.4 的 FilenameUtils，结果与其一致。
 *
 * <p>分隔符同时识别 {@code /} 与 {@code \}；参数为 null 时返回 null。
 */
public final class FileNameUtils {

    private FileNameUtils() {}

    /** 最后一个分隔符之后的部分，如 {@code a/b/c.jpg} → {@code c.jpg}。 */
    public static String getName(String fileName) {
        if (fileName == null) {
            return null;
        }
        return fileName.substring(indexOfLastSeparator(fileName) + 1);
    }

    /** 最后一个点之后的部分；点位于最后一个分隔符之前或不存在时返回空串。 */
    public static String getExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int index = indexOfExtension(fileName);
        return index == -1 ? "" : fileName.substring(index + 1);
    }

    /** 去掉目录与扩展名，如 {@code a/b/c.tar.gz} → {@code c.tar}。 */
    public static String getBaseName(String fileName) {
        String name = getName(fileName);
        if (name == null) {
            return null;
        }
        int index = indexOfExtension(name);
        return index == -1 ? name : name.substring(0, index);
    }

    /**
     * 去掉前缀后的目录部分并保留末尾分隔符，如 {@code /a/b/c.jpg} → {@code a/b/}；没有目录时返回空串，前缀非法时返回 null。
     */
    public static String getPath(String fileName) {
        if (fileName == null) {
            return null;
        }
        int prefix = getPrefixLength(fileName);
        if (prefix < 0) {
            return null;
        }
        int end = indexOfLastSeparator(fileName) + 1;
        if (prefix >= fileName.length() || end == 0 || prefix >= end) {
            return "";
        }
        return fileName.substring(prefix, end);
    }

    /** 前缀：{@code /}、{@code ~}、{@code ~user/}、盘符 {@code C:}、{@code C:/} 或 UNC {@code //server/}。 */
    static int getPrefixLength(String fileName) {
        int length = fileName.length();
        if (length == 0) {
            return 0;
        }
        char first = fileName.charAt(0);
        if (first == ':') {
            return -1;
        }
        if (length == 1) {
            if (first == '~') {
                return 2;
            }
            return isSeparator(first) ? 1 : 0;
        }
        if (first == '~') {
            int separator = indexOfFirstSeparator(fileName, 1);
            return separator == -1 ? length + 1 : separator + 1;
        }
        char second = fileName.charAt(1);
        if (second == ':') {
            char drive = Character.toUpperCase(first);
            if (drive >= 'A' && drive <= 'Z') {
                return length == 2 || !isSeparator(fileName.charAt(2)) ? 2 : 3;
            }
            return -1;
        }
        if (isSeparator(first) && isSeparator(second)) {
            int separator = indexOfFirstSeparator(fileName, 2);
            if (separator == -1 || separator == 2) {
                return -1;
            }
            return separator + 1;
        }
        return isSeparator(first) ? 1 : 0;
    }

    private static int indexOfExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return indexOfLastSeparator(fileName) > dot ? -1 : dot;
    }

    private static int indexOfLastSeparator(String fileName) {
        return Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
    }

    private static int indexOfFirstSeparator(String fileName, int from) {
        int unix = fileName.indexOf('/', from);
        int windows = fileName.indexOf('\\', from);
        if (unix == -1) {
            return windows;
        }
        return windows == -1 ? unix : Math.min(unix, windows);
    }

    private static boolean isSeparator(char c) {
        return c == '/' || c == '\\';
    }
}
