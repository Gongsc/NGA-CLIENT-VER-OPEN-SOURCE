package gov.anzong.androidnga.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ContextUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/** 头像加载与上传图片的缩放。 */
object ImageUtils {

    private var defaultAvatar: Drawable? = null

    private fun computeSampleSize(options: BitmapFactory.Options, minSideLength: Int, maxNumOfPixels: Int): Int {
        val initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels)
        if (initialSize > 8) {
            return (initialSize + 7) / 8 * 8
        }
        var roundedSize = 1
        while (roundedSize < initialSize) {
            roundedSize = roundedSize shl 1
        }
        return roundedSize
    }

    private fun computeInitialSampleSize(options: BitmapFactory.Options, minSideLength: Int, maxNumOfPixels: Int): Int {
        val w = options.outWidth.toDouble()
        val h = options.outHeight.toDouble()
        val lowerBound = if (maxNumOfPixels == -1) 1 else ceil(sqrt(w * h / maxNumOfPixels)).toInt()
        val upperBound = if (minSideLength == -1) 128 else min(floor(w / minSideLength), floor(h / minSideLength)).toInt()
        return when {
            // return the larger one when there is no overlapping zone.
            upperBound < lowerBound -> lowerBound
            maxNumOfPixels == -1 && minSideLength == -1 -> 1
            minSideLength == -1 -> lowerBound
            else -> upperBound
        }
    }

    /**
     * 缩小过大的图片后以 PNG 上传。两个流读取同一图片：第一个只读尺寸，第二个解码。
     */
    @JvmStatic
    @Throws(IOException::class)
    fun fitImageToUpload(input: InputStream?, input2: InputStream?): ByteArray {
        if (input == null || input2 == null || input === input2) throw IOException("无法读取图片")
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        input.use { BitmapFactory.decodeStream(it, null, opts) }
        opts.inSampleSize = computeSampleSize(opts, 512, 1024 * 1024)
        opts.inJustDecodeBounds = false
        val bitmap = input2.use { BitmapFactory.decodeStream(it, null, opts) } ?: throw IOException("无法读取图片")
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    /** 居中裁成正方形后画成圆角，[ratio] 为边长与圆角半径之比。 */
    @JvmStatic
    fun toRoundCorner(source: Bitmap, ratio: Float): Bitmap {
        val size = min(source.width, source.height)
        val bitmap = Bitmap.createBitmap(source, (source.width - size) / 2, (source.height - size) / 2, size, size)
        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(RectF(rect), bitmap.width / ratio, bitmap.height / ratio, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
    }

    private fun defaultAvatar(): Drawable = defaultAvatar ?: run {
        val resources = ContextUtils.getContext().resources
        val avatar = BitmapFactory.decodeResource(resources, R.drawable.default_avatar)
        toRoundCorner(avatar, 2f).toDrawable(resources).also { defaultAvatar = it }
    }

    @JvmStatic
    @JvmOverloads
    fun loadRoundCornerAvatar(imageView: ImageView, url: String?, onlyRetrieveFromCache: Boolean = false) {
        Glide.with(ContextUtils.getContext())
            .load(url)
            .placeholder(defaultAvatar())
            .circleCrop()
            .onlyRetrieveFromCache(onlyRetrieveFromCache)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(imageView)
    }

    @JvmStatic
    fun loadAvatar(imageView: ImageView, url: String?) {
        Glide.with(ContextUtils.getContext())
            .load(url)
            .placeholder(defaultAvatar())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(imageView)
    }
}
