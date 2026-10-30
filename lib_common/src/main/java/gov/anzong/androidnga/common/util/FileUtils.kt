package gov.anzong.androidnga.common.util

import gov.anzong.androidnga.base.util.ContextUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * @author yangyihang
 */
object FileUtils {

    /** 以 UTF-8 读取整个文件。 */
    @JvmStatic
    @Throws(IOException::class)
    fun readFileToString(file: File): String = file.readText()

    /** 以 UTF-8 写入，自动创建上级目录；内容为 null 时与 commons-io 一致只创建文件、不写入。 */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun write(file: File, content: CharSequence?, append: Boolean = false) {
        openOutputStream(file, append).use { output ->
            if (content != null) {
                output.write(content.toString().toByteArray())
            }
        }
    }

    /** 递归删除目录；目录不存在时直接返回，任一文件删除失败时抛出 IOException。 */
    @JvmStatic
    @Throws(IOException::class)
    fun deleteDirectory(directory: File) {
        if (!directory.exists()) {
            return
        }
        require(directory.isDirectory) { "$directory 不是目录" }
        val children = directory.listFiles() ?: throw IOException("无法列出目录 $directory")
        for (child in children) {
            if (child.isDirectory) {
                deleteDirectory(child)
            } else if (!child.delete()) {
                throw IOException("无法删除文件 $child")
            }
        }
        if (!directory.delete()) {
            throw IOException("无法删除目录 $directory")
        }
    }

    /** 写入流的全部内容并关闭该流，自动创建上级目录。 */
    @JvmStatic
    @Throws(IOException::class)
    fun copyInputStreamToFile(source: InputStream, destination: File) {
        source.use { input -> openOutputStream(destination, false).use { input.copyTo(it) } }
    }

    @Throws(IOException::class)
    private fun openOutputStream(file: File, append: Boolean): OutputStream {
        if (file.exists()) {
            if (file.isDirectory) {
                throw IOException("$file 是目录")
            }
        } else {
            val parent = file.parentFile
            if (parent != null && !parent.mkdirs() && !parent.isDirectory) {
                throw IOException("无法创建目录 $parent")
            }
        }
        return FileOutputStream(file, append)
    }

    /** 以 UTF-8 读取 assets 中的文件，失败时返回空串。 */
    @JvmStatic
    fun readAssetToString(path: String): String = try {
        ContextUtils.getContext().assets.open(path).use { String(it.readBytes()) }
    } catch (e: IOException) {
        e.printStackTrace()
        ""
    }

    /** 将文件或目录打包到 [destPath]，目录本身作为压缩包的顶层条目。 */
    @JvmStatic
    fun zipFiles(srcPath: String, destPath: String): Boolean {
        val destFile = File(destPath)
        destFile.parentFile?.mkdirs()
        try {
            ZipOutputStream(FileOutputStream(destFile)).use { zipFiles(File(srcPath), "", it) }
            return true
        } catch (e: IOException) {
            LogUtils.print(e)
        }
        return false
    }

    @Throws(IOException::class)
    private fun zipFiles(source: File, destZipPath: String, zos: ZipOutputStream) {
        if (source.isFile) {
            FileInputStream(source).use { input ->
                zos.putNextEntry(ZipEntry(destZipPath + source.name))
                input.copyTo(zos)
                zos.closeEntry()
            }
        } else {
            val children = source.list() ?: return
            val currentZipDir = destZipPath + source.name + File.separator
            zos.putNextEntry(ZipEntry(currentZipDir))
            zos.closeEntry()
            for (child in children) {
                zipFiles(File(source, child), currentZipDir, zos)
            }
        }
    }

    /** 解压到 [destPath]；指向目标目录之外的条目会被跳过。 */
    @JvmStatic
    fun unzip(srcZip: String, destPath: String) {
        val destDir = File(destPath).canonicalFile
        try {
            ZipInputStream(FileInputStream(srcZip)).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val outputFile = File(destDir, entry.name).canonicalFile
                    if (!outputFile.path.startsWith(destDir.path + File.separator)) {
                        continue
                    }
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { zis.copyTo(it) }
                    }
                }
            }
        } catch (e: IOException) {
            LogUtils.print(e)
        }
    }
}
