package gov.anzong.androidnga.common.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.charset.Charset

/** 替代 commons-io 的工具方法；文件名期望值由 commons-io 2.4 实测得到。 */
class FileHelpersTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun fileNamesMatchCommonsIo() {
        for ((input, name, extension, baseName, path) in NAMES) {
            assertEquals(input, name, FileNameUtils.getName(input))
            assertEquals(input, extension, FileNameUtils.getExtension(input))
            assertEquals(input, baseName, FileNameUtils.getBaseName(input))
            assertEquals(input, path, FileNameUtils.getPath(input))
        }
    }

    @Test
    fun streamsAreDecodedWithRequestedCharset() {
        val gbk = "论坛 NGA".toByteArray(Charset.forName("GBK"))
        assertEquals("论坛 NGA", StreamUtils.toString(ByteArrayInputStream(gbk), "gbk"))
        assertArrayEquals(gbk, StreamUtils.toByteArray(ByteArrayInputStream(gbk)))
        val large = ByteArray(20000)
        large[19999] = 7
        assertArrayEquals(large, StreamUtils.toByteArray(ByteArrayInputStream(large)))
        StreamUtils.closeQuietly(null)
        StreamUtils.closeQuietly(Closeable { throw IOException("忽略") })
    }

    @Test
    fun writeCreatesParentsAndAppends() {
        val file = File(temp.root, "a/b/c.json")
        FileUtils.write(file, "{\"中\":1}")
        assertEquals("{\"中\":1}", FileUtils.readFileToString(file))
        FileUtils.write(file, "\n二", true)
        assertEquals("{\"中\":1}\n二", FileUtils.readFileToString(file))
        FileUtils.write(file, null)
        assertEquals("", FileUtils.readFileToString(file))
        FileUtils.write(file, "覆盖")
        assertEquals("覆盖", file.readText())
        try {
            FileUtils.write(file.parentFile!!, "x")
            fail("目录不能写入")
        } catch (expected: IOException) {
        }
        try {
            FileUtils.readFileToString(File(temp.root, "missing"))
            fail("文件不存在")
        } catch (expected: IOException) {
        }
    }

    @Test
    fun deleteDirectoryIsRecursiveAndToleratesMissing() {
        val root = temp.newFolder("cache")
        FileUtils.write(File(root, "1/2/3.txt"), "x")
        FileUtils.write(File(root, "top.txt"), "y")
        FileUtils.deleteDirectory(root)
        assertFalse(root.exists())
        FileUtils.deleteDirectory(root)
    }

    @Test
    fun copyInputStreamToFileClosesSource() {
        var closed = false
        val input = object : ByteArrayInputStream("缓存".toByteArray()) {
            override fun close() {
                closed = true
            }
        }
        val target = File(temp.root, "z/temp.zip")
        FileUtils.copyInputStreamToFile(input, target)
        assertEquals("缓存", FileUtils.readFileToString(target))
        assertTrue(closed)
    }

    private companion object {

        /** {输入, getName, getExtension, getBaseName, getPath} */
        val NAMES: List<Array<String?>> = listOf(
        arrayOf(null, null, null, null, null),
        arrayOf("", "", "", "", ""),
        arrayOf("a", "a", "", "a", ""),
        arrayOf("a.jpg", "a.jpg", "jpg", "a", ""),
        arrayOf("/a/b/c.jpg", "c.jpg", "jpg", "c", "a/b/"),
        arrayOf("a/b/c", "c", "", "c", "a/b/"),
        arrayOf("a\\b\\c.png", "c.png", "png", "c", "a\\b\\"),
        arrayOf("c:\\x\\y.png", "y.png", "png", "y", "x\\"),
        arrayOf("C:abc.txt", "C:abc.txt", "txt", "C:abc", ""),
        arrayOf("C:", "C:", "", "C:", ""),
        arrayOf("C:/", "", "", "", ""),
        arrayOf("c:/x.y/z", "z", "", "z", "x.y/"),
        arrayOf("http://img.nga.cn/attachments/mon_2024/a.jpg?x=1", "a.jpg?x=1", "jpg?x=1", "a", "http://img.nga.cn/attachments/mon_2024/"),
        arrayOf("https://img.nga.178.com/avatars/2002/123/45.png", "45.png", "png", "45", "https://img.nga.178.com/avatars/2002/123/"),
        arrayOf("https://x/y/", "", "", "", "https://x/y/"),
        arrayOf("noext", "noext", "", "noext", ""),
        arrayOf(".hidden", ".hidden", "hidden", "", ""),
        arrayOf("a.b.c", "a.b.c", "c", "a.b", ""),
        arrayOf("dir.v1/file", "file", "", "file", "dir.v1/"),
        arrayOf("/abs", "abs", "", "abs", ""),
        arrayOf("/", "", "", "", ""),
        arrayOf("a/", "", "", "", "a/"),
        arrayOf("x.jpg?a.b", "x.jpg?a.b", "b", "x.jpg?a", ""),
        arrayOf("~", "~", "", "~", ""),
        arrayOf("~/f.txt", "f.txt", "txt", "f", ""),
        arrayOf("~user", "~user", "", "~user", ""),
        arrayOf("~user/dir/f.gif", "f.gif", "gif", "f", "dir/"),
        arrayOf("//server/share/f.txt", "f.txt", "txt", "f", "share/"),
        arrayOf("//", "", "", "", null),
        arrayOf("///x", "x", "", "x", null),
        arrayOf("\\\\srv\\s\\f.jpg", "f.jpg", "jpg", "f", "s\\"),
        arrayOf(":bad", ":bad", "", ":bad", null),
        arrayOf("./mon_2024/a.webp", "a.webp", "webp", "a", "./mon_2024/"),
        arrayOf("../up/b.JPEG", "b.JPEG", "JPEG", "b", "../up/"),
        arrayOf("file.tar.gz", "file.tar.gz", "gz", "file.tar", ""),
        arrayOf("中文/图片.png", "图片.png", "png", "图片", "中文/"),
        arrayOf("a/b.c/d", "d", "", "d", "a/b.c/"),
        )
    }
}
