package gov.anzong.androidnga.gallery

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.max
import kotlin.math.min

/**
 * 图片浏览页单张图片的缩放与平移。以容器中心为变换原点，缩放时保持手势中心下的内容不动；
 * 平移量按适配后的图片尺寸限制，图片边缘不会离开容器，小于容器的方向保持居中。
 */
@Stable
class GalleryZoomState(private val maxScale: Float = MAX_SCALE) {

    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    private var containerSize = Size.Zero
    private var intrinsicSize = Size.Unspecified

    /** 已放大时由图片处理单指拖动，分页滑动暂停。 */
    val isZoomed: Boolean get() = scale > 1f + EPSILON

    fun setContainerSize(size: Size) {
        containerSize = size
        offset = clamp(scale, offset)
    }

    /** 图片原始尺寸；未知时按占满容器处理。 */
    fun setIntrinsicSize(width: Int, height: Int) {
        intrinsicSize = if (width > 0 && height > 0) Size(width.toFloat(), height.toFloat()) else Size.Unspecified
        offset = clamp(scale, offset)
    }

    /** 双指缩放与拖动：[centroid] 为手势中心在容器中的坐标。 */
    fun transform(zoom: Float, pan: Offset, centroid: Offset) {
        val newScale = (scale * zoom).coerceIn(1f, maxScale)
        val center = containerSize.center()
        val anchor = (centroid - center - offset) / scale
        snapTo(newScale, centroid - center - anchor * newScale + pan)
    }

    /** 双击：已放大时还原，否则以点击位置为中心放大到 [DOUBLE_TAP_SCALE]。 */
    fun doubleTapTarget(tap: Offset): Pair<Float, Offset> {
        if (isZoomed) return 1f to Offset.Zero
        val center = containerSize.center()
        val anchor = (tap - center - offset) / scale
        val target = min(DOUBLE_TAP_SCALE, maxScale)
        return target to clamp(target, tap - center - anchor * target)
    }

    fun snapTo(newScale: Float, newOffset: Offset) {
        scale = newScale.coerceIn(1f, maxScale)
        offset = if (scale <= 1f + EPSILON) Offset.Zero else clamp(scale, newOffset)
    }

    fun reset() = snapTo(1f, Offset.Zero)

    private fun clamp(scale: Float, offset: Offset): Offset {
        val content = displayedSize()
        val maxX = max(0f, (content.width * scale - containerSize.width) / 2)
        val maxY = max(0f, (content.height * scale - containerSize.height) / 2)
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }

    /** 图片按 fitCenter 适配容器后的显示尺寸。 */
    private fun displayedSize(): Size {
        if (intrinsicSize == Size.Unspecified || containerSize.width <= 0f || containerSize.height <= 0f) {
            return containerSize
        }
        val ratio = min(containerSize.width / intrinsicSize.width, containerSize.height / intrinsicSize.height)
        return Size(intrinsicSize.width * ratio, intrinsicSize.height * ratio)
    }

    private fun Size.center() = Offset(width / 2, height / 2)

    companion object {
        const val MAX_SCALE = 10f
        const val DOUBLE_TAP_SCALE = 2.5f
        private const val EPSILON = 0.01f
    }
}
