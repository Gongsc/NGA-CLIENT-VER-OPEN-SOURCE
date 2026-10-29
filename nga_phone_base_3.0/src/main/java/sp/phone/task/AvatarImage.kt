package sp.phone.task

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 把选择的图片转为头像上传用的 data URL，规则与网页“设置头像”一致：
 * GIF 原样上传（保留动画），其他图片等比缩小到不超过 [MAX_WIDTH]×[MAX_HEIGHT] 后以 PNG 上传；文件不能超过 15 MB。
 */
object AvatarImage {

    const val MAX_WIDTH = 180
    const val MAX_HEIGHT = 255
    private const val MAX_BYTES = 15360 * 1024

    /** 等比缩放后的尺寸，只缩小不放大。 */
    @JvmStatic
    fun fit(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return width to height
        val scale = minOf(1.0, MAX_WIDTH.toDouble() / width, MAX_HEIGHT.toDouble() / height)
        return maxOf(1, Math.round(width * scale).toInt()) to maxOf(1, Math.round(height * scale).toInt())
    }

    /** 读取并编码 [uri]；不是图片、过大或无法读取时抛出带提示的 IllegalArgumentException。在后台线程调用。 */
    @JvmStatic
    fun dataUrl(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val type = resolver.getType(uri).orEmpty()
        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        } ?: throw IllegalArgumentException("无法读取所选图片，请换一个应用选择")
        if (bytes.size > MAX_BYTES) throw IllegalArgumentException("图片不能超过 15 MB")
        if (type == "image/gif") return "data:image/gif;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IllegalArgumentException("所选择的不是图片")
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_WIDTH && bounds.outHeight / (sample * 2) >= MAX_HEIGHT) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw IllegalArgumentException("所选择的不是图片")
        val (width, height) = fit(decoded.width, decoded.height)
        val scaled = if (width == decoded.width && height == decoded.height) decoded else Bitmap.createScaledBitmap(decoded, width, height, true)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.PNG, 100, output)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        return "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}
