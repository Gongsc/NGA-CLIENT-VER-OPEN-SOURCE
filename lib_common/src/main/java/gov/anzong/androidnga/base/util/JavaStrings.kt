package gov.anzong.androidnga.base.util

/**
 * 与 Java 的 `String.trim()` 一致：只去掉首尾不大于空格（U+0020）的字符。
 * Kotlin 的 `trim()` 按 Unicode 空白处理，会去掉全角空格等字符、却保留控制字符，解析论坛内容时不能混用。
 */
fun String.javaTrim(): String {
    var start = 0
    var end = length
    while (start < end && this[start] <= ' ') start++
    while (end > start && this[end - 1] <= ' ') end--
    return if (start == 0 && end == length) this else substring(start, end)
}
