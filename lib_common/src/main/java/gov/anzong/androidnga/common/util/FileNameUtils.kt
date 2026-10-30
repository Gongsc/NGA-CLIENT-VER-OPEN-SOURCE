package gov.anzong.androidnga.common.util

/**
 * 文件名解析，替代 commons-io 2.4 的 FilenameUtils，结果与其一致。
 *
 * 分隔符同时识别 `/` 与 `\`；参数为 null 时返回 null。
 */
object FileNameUtils {

    /** 最后一个分隔符之后的部分，如 `a/b/c.jpg` → `c.jpg`。 */
    @JvmStatic
    fun getName(fileName: String?): String? = fileName?.substring(indexOfLastSeparator(fileName) + 1)

    /** 最后一个点之后的部分；点位于最后一个分隔符之前或不存在时返回空串。 */
    @JvmStatic
    fun getExtension(fileName: String?): String? {
        if (fileName == null) {
            return null
        }
        val index = indexOfExtension(fileName)
        return if (index == -1) "" else fileName.substring(index + 1)
    }

    /** 去掉目录与扩展名，如 `a/b/c.tar.gz` → `c.tar`。 */
    @JvmStatic
    fun getBaseName(fileName: String?): String? {
        val name = getName(fileName) ?: return null
        val index = indexOfExtension(name)
        return if (index == -1) name else name.substring(0, index)
    }

    /**
     * 去掉前缀后的目录部分并保留末尾分隔符，如 `/a/b/c.jpg` → `a/b/`；没有目录时返回空串，前缀非法时返回 null。
     */
    @JvmStatic
    fun getPath(fileName: String?): String? {
        if (fileName == null) {
            return null
        }
        val prefix = getPrefixLength(fileName)
        if (prefix < 0) {
            return null
        }
        val end = indexOfLastSeparator(fileName) + 1
        if (prefix >= fileName.length || end == 0 || prefix >= end) {
            return ""
        }
        return fileName.substring(prefix, end)
    }

    /** 前缀：`/`、`~`、`~user/`、盘符 `C:`、`C:/` 或 UNC `//server/`。 */
    @JvmStatic
    fun getPrefixLength(fileName: String): Int {
        val length = fileName.length
        if (length == 0) {
            return 0
        }
        val first = fileName[0]
        if (first == ':') {
            return -1
        }
        if (length == 1) {
            if (first == '~') {
                return 2
            }
            return if (isSeparator(first)) 1 else 0
        }
        if (first == '~') {
            val separator = indexOfFirstSeparator(fileName, 1)
            return if (separator == -1) length + 1 else separator + 1
        }
        val second = fileName[1]
        if (second == ':') {
            val drive = first.uppercaseChar()
            if (drive in 'A'..'Z') {
                return if (length == 2 || !isSeparator(fileName[2])) 2 else 3
            }
            return -1
        }
        if (isSeparator(first) && isSeparator(second)) {
            val separator = indexOfFirstSeparator(fileName, 2)
            if (separator == -1 || separator == 2) {
                return -1
            }
            return separator + 1
        }
        return if (isSeparator(first)) 1 else 0
    }

    private fun indexOfExtension(fileName: String): Int {
        val dot = fileName.lastIndexOf('.')
        return if (indexOfLastSeparator(fileName) > dot) -1 else dot
    }

    private fun indexOfLastSeparator(fileName: String): Int =
        maxOf(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'))

    private fun indexOfFirstSeparator(fileName: String, from: Int): Int {
        val unix = fileName.indexOf('/', from)
        val windows = fileName.indexOf('\\', from)
        if (unix == -1) {
            return windows
        }
        return if (windows == -1) unix else minOf(unix, windows)
    }

    private fun isSeparator(c: Char): Boolean = c == '/' || c == '\\'
}
