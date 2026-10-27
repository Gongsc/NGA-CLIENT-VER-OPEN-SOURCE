package gov.anzong.androidnga.common.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/** 流读取工具，替代 commons-io 的 IOUtils。读取方法不关闭传入的流。 */
public final class StreamUtils {

    private StreamUtils() {}

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    /** 按指定字符集解码全部内容；不支持的字符集抛出运行时异常。 */
    public static String toString(InputStream input, String charsetName) throws IOException {
        return new String(toByteArray(input), Charset.forName(charsetName));
    }

    static long copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        long count = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
            count += read;
        }
        return count;
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // 与 commons-io 一致，关闭失败不影响调用方。
        }
    }
}
