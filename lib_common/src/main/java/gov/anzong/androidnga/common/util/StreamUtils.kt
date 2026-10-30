package gov.anzong.androidnga.common.util

import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

/** 流读取工具，替代 commons-io 的 IOUtils。读取方法不关闭传入的流。 */
object StreamUtils {

    @JvmStatic
    @Throws(IOException::class)
    fun toByteArray(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        copy(input, output)
        return output.toByteArray()
    }

    /** 按指定字符集解码全部内容；不支持的字符集抛出运行时异常。 */
    @JvmStatic
    @Throws(IOException::class)
    fun toString(input: InputStream, charsetName: String): String =
        String(toByteArray(input), Charset.forName(charsetName))

    @JvmStatic
    @Throws(IOException::class)
    fun copy(input: InputStream, output: OutputStream): Long = input.copyTo(output, 8192)

    @JvmStatic
    fun closeQuietly(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (ignored: IOException) {
            // 与 commons-io 一致，关闭失败不影响调用方。
        }
    }
}
