package gov.anzong.androidnga.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.io.IOException

/** Publishes app-created files without broad storage permissions. Android 12+. */
object SharedStorage {

    @JvmStatic
    @Throws(IOException::class)
    fun publish(context: Context, file: File, collection: Uri, folder: String, displayName: String, mimeType: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
        values.put(MediaStore.MediaColumns.IS_PENDING, 1)
        val uri = resolver.insert(collection, values) ?: throw IOException("无法创建文件")
        try {
            file.inputStream().use { input ->
                val output = resolver.openOutputStream(uri) ?: throw IOException("无法写入文件")
                output.use { input.copyTo(it) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
