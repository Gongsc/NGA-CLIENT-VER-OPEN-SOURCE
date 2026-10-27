package gov.anzong.androidnga.common.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** 替代 commons-io 的工具方法；文件名期望值由 commons-io 2.4 实测得到。 */
public class FileHelpersTest {

    @Rule public TemporaryFolder temp = new TemporaryFolder();

    /** {输入, getName, getExtension, getBaseName, getPath} */
    private static final String[][] NAMES = {
        {null, null, null, null, null},
        {"", "", "", "", ""},
        {"a", "a", "", "a", ""},
        {"a.jpg", "a.jpg", "jpg", "a", ""},
        {"/a/b/c.jpg", "c.jpg", "jpg", "c", "a/b/"},
        {"a/b/c", "c", "", "c", "a/b/"},
        {"a\\b\\c.png", "c.png", "png", "c", "a\\b\\"},
        {"c:\\x\\y.png", "y.png", "png", "y", "x\\"},
        {"C:abc.txt", "C:abc.txt", "txt", "C:abc", ""},
        {"C:", "C:", "", "C:", ""},
        {"C:/", "", "", "", ""},
        {"c:/x.y/z", "z", "", "z", "x.y/"},
        {"http://img.nga.cn/attachments/mon_2024/a.jpg?x=1", "a.jpg?x=1", "jpg?x=1", "a", "http://img.nga.cn/attachments/mon_2024/"},
        {"https://img.nga.178.com/avatars/2002/123/45.png", "45.png", "png", "45", "https://img.nga.178.com/avatars/2002/123/"},
        {"https://x/y/", "", "", "", "https://x/y/"},
        {"noext", "noext", "", "noext", ""},
        {".hidden", ".hidden", "hidden", "", ""},
        {"a.b.c", "a.b.c", "c", "a.b", ""},
        {"dir.v1/file", "file", "", "file", "dir.v1/"},
        {"/abs", "abs", "", "abs", ""},
        {"/", "", "", "", ""},
        {"a/", "", "", "", "a/"},
        {"x.jpg?a.b", "x.jpg?a.b", "b", "x.jpg?a", ""},
        {"~", "~", "", "~", ""},
        {"~/f.txt", "f.txt", "txt", "f", ""},
        {"~user", "~user", "", "~user", ""},
        {"~user/dir/f.gif", "f.gif", "gif", "f", "dir/"},
        {"//server/share/f.txt", "f.txt", "txt", "f", "share/"},
        {"//", "", "", "", null},
        {"///x", "x", "", "x", null},
        {"\\\\srv\\s\\f.jpg", "f.jpg", "jpg", "f", "s\\"},
        {":bad", ":bad", "", ":bad", null},
        {"./mon_2024/a.webp", "a.webp", "webp", "a", "./mon_2024/"},
        {"../up/b.JPEG", "b.JPEG", "JPEG", "b", "../up/"},
        {"file.tar.gz", "file.tar.gz", "gz", "file.tar", ""},
        {"中文/图片.png", "图片.png", "png", "图片", "中文/"},
        {"a/b.c/d", "d", "", "d", "a/b.c/"},
    };

    @Test
    public void fileNamesMatchCommonsIo() {
        for (String[] row : NAMES) {
            assertEquals(row[0], row[1], FileNameUtils.getName(row[0]));
            assertEquals(row[0], row[2], FileNameUtils.getExtension(row[0]));
            assertEquals(row[0], row[3], FileNameUtils.getBaseName(row[0]));
            assertEquals(row[0], row[4], FileNameUtils.getPath(row[0]));
        }
    }

    @Test
    public void streamsAreDecodedWithRequestedCharset() throws IOException {
        byte[] gbk = "论坛 NGA".getBytes(Charset.forName("GBK"));
        assertEquals("论坛 NGA", StreamUtils.toString(new ByteArrayInputStream(gbk), "gbk"));
        assertArrayEquals(gbk, StreamUtils.toByteArray(new ByteArrayInputStream(gbk)));
        byte[] large = new byte[20000];
        large[19999] = 7;
        assertArrayEquals(large, StreamUtils.toByteArray(new ByteArrayInputStream(large)));
        StreamUtils.closeQuietly(null);
        StreamUtils.closeQuietly(() -> { throw new IOException("忽略"); });
    }

    @Test
    public void writeCreatesParentsAndAppends() throws IOException {
        File file = new File(temp.getRoot(), "a/b/c.json");
        FileUtils.write(file, "{\"中\":1}");
        assertEquals("{\"中\":1}", FileUtils.readFileToString(file));
        FileUtils.write(file, "\n二", true);
        assertEquals("{\"中\":1}\n二", FileUtils.readFileToString(file));
        FileUtils.write(file, "覆盖");
        assertEquals("覆盖", new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        try {
            FileUtils.write(file.getParentFile(), "x");
            fail("目录不能写入");
        } catch (IOException expected) {
        }
        try {
            FileUtils.readFileToString(new File(temp.getRoot(), "missing"));
            fail("文件不存在");
        } catch (IOException expected) {
        }
    }

    @Test
    public void deleteDirectoryIsRecursiveAndToleratesMissing() throws IOException {
        File root = temp.newFolder("cache");
        FileUtils.write(new File(root, "1/2/3.txt"), "x");
        FileUtils.write(new File(root, "top.txt"), "y");
        FileUtils.deleteDirectory(root);
        assertFalse(root.exists());
        FileUtils.deleteDirectory(root);
    }

    @Test
    public void copyUrlToFileCreatesParents() throws IOException {
        File source = temp.newFile("source.bin");
        FileUtils.write(source, "图片数据");
        File target = new File(temp.getRoot(), "avatar/9/a.jpg");
        FileUtils.copyURLToFile(source.toURI().toURL(), target, 2000, 5000);
        assertTrue(target.isFile());
        assertEquals("图片数据", FileUtils.readFileToString(target));
    }

    @Test
    public void copyInputStreamToFileClosesSource() throws IOException {
        boolean[] closed = {false};
        ByteArrayInputStream input =
                new ByteArrayInputStream("缓存".getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public void close() {
                        closed[0] = true;
                    }
                };
        File target = new File(temp.getRoot(), "z/temp.zip");
        FileUtils.copyInputStreamToFile(input, target);
        assertEquals("缓存", FileUtils.readFileToString(target));
        assertTrue(closed[0]);
    }
}
