package gov.anzong.androidnga.common.util;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import gov.anzong.androidnga.base.util.ContextUtils;

/**
 * @author yangyihang
 */
public class FileUtils {

    /** 以 UTF-8 读取整个文件。 */
    public static String readFileToString(File file) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return new String(StreamUtils.toByteArray(input), StandardCharsets.UTF_8);
        }
    }

    /** 以 UTF-8 覆盖写入，自动创建上级目录。 */
    public static void write(File file, CharSequence content) throws IOException {
        write(file, content, false);
    }

    /** 以 UTF-8 写入，自动创建上级目录；内容为 null 时与 commons-io 一致只创建文件、不写入。 */
    public static void write(File file, CharSequence content, boolean append) throws IOException {
        try (OutputStream output = openOutputStream(file, append)) {
            if (content != null) {
                output.write(content.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /** 递归删除目录；目录不存在时直接返回，任一文件删除失败时抛出 IOException。 */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " 不是目录");
        }
        File[] children = directory.listFiles();
        if (children == null) {
            throw new IOException("无法列出目录 " + directory);
        }
        for (File child : children) {
            if (child.isDirectory()) {
                deleteDirectory(child);
            } else if (!child.delete()) {
                throw new IOException("无法删除文件 " + child);
            }
        }
        if (!directory.delete()) {
            throw new IOException("无法删除目录 " + directory);
        }
    }

    /** 写入流的全部内容并关闭该流，自动创建上级目录。 */
    public static void copyInputStreamToFile(InputStream source, File destination) throws IOException {
        try (InputStream input = source;
                OutputStream output = openOutputStream(destination, false)) {
            StreamUtils.copy(input, output);
        }
    }

    private static OutputStream openOutputStream(File file, boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException(file + " 是目录");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("无法创建目录 " + parent);
            }
        }
        return new FileOutputStream(file, append);
    }

    public static String readAssetToString(String path) {
        AssetManager assetManager = ContextUtils.getContext().getAssets();
        try (InputStream is = assetManager.open(path)) {
            int length = is.available();
            byte[] buffer = new byte[length];
            is.read(buffer);
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static boolean zipFiles(String scrPath, String destPath) {
        File destFile = new File(destPath);
        destFile.getParentFile().mkdirs();
        try (ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(destFile.getAbsolutePath()))) {
            //压缩
            zipFiles(scrPath, null, outZip);
            return true;
        } catch (IOException e) {
            LogUtils.print(e);
        }
        return false;
    }

    private static void zipFiles(String srcPath, String destZipPath, ZipOutputStream zos) throws IOException {
        if (zos == null) {
            return;
        }
        if (destZipPath == null) {
            destZipPath = "";
        }
        File scrFile = new File(srcPath);
        String fileName = scrFile.getName();
        if (scrFile.isFile()) {
            try (FileInputStream is = new FileInputStream(scrFile)) {
                ZipEntry zipEntry = new ZipEntry(destZipPath + fileName);
                zos.putNextEntry(zipEntry);
                int len;
                byte[] buffer = new byte[1024];
                while ((len = is.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
            }
        } else {
            //文件夹
            String[] fileList = scrFile.list();
            if (fileList == null) {
                return;
            }
            String currentZipDir = destZipPath + fileName + File.separator;
            ZipEntry zipEntry = new ZipEntry(currentZipDir);
            zos.putNextEntry(zipEntry);
            zos.closeEntry();
            //子文件和递归
            for (String child : fileList) {
                zipFiles(srcPath + File.separator + child, currentZipDir, zos);
            }

        }
    }

    public static void unzip(String scrZip, String destPath) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(new File(scrZip)))) {
            ZipFile zipFile = new ZipFile(scrZip);
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                String outputPath = destPath + File.separator + zipEntry.getName();
                File outputFile = new File(outputPath);
                if (zipEntry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    byte[] buffer = new byte[1024];
                    try (OutputStream os = new FileOutputStream(outputFile);
                         // 通过ZipFile的getInputStream方法拿到具体的ZipEntry的输入流
                         InputStream is = zipFile.getInputStream(zipEntry)) {
                        int len = 0;
                        while ((len = is.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                    }
                }

            }
        } catch (IOException e) {
            LogUtils.print(e);
        }
    }
}
