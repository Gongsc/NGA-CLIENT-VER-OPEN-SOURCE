package gov.anzong.androidnga.task

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** 视图坐标中的矩形。 */
data class CropBox(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    val centerX get() = (left + right) / 2
    val centerY get() = (top + bottom) / 2

    fun contains(x: Float, y: Float) = x in left..right && y in top..bottom

    fun intersect(other: CropBox) = CropBox(max(left, other.left), max(top, other.top), min(right, other.right), min(bottom, other.bottom))
}

/** 图片中的截取区域（像素）。 */
data class CropPixels(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * 头像截取的几何计算，与界面无关：
 * 图片以 [scale] 缩放、左上角位于 (offsetX, offsetY) 显示；截取框始终位于图片与视图的重叠区域内，
 * 移动或缩放图片时图片始终覆盖截取框（图片过小时截取框随之缩小）。
 */
object AvatarCrop {

    const val MAX_ZOOM = 8f

    enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    /** 完整显示图片的缩放比例，即最小缩放。 */
    fun fitScale(viewWidth: Float, viewHeight: Float, imageWidth: Int, imageHeight: Int) =
        min(viewWidth / imageWidth, viewHeight / imageHeight)

    fun displayed(imageWidth: Int, imageHeight: Int, scale: Float, offsetX: Float, offsetY: Float) =
        CropBox(offsetX, offsetY, offsetX + imageWidth * scale, offsetY + imageHeight * scale)

    /** 初始截取框：可用区域中央，边长为可用区域的 80%；正方形时取较短边。 */
    fun initial(bounds: CropBox, square: Boolean): CropBox {
        val w = if (square) min(bounds.width, bounds.height) * 0.8f else bounds.width * 0.8f
        val h = if (square) w else bounds.height * 0.8f
        return CropBox(bounds.centerX - w / 2, bounds.centerY - h / 2, bounds.centerX + w / 2, bounds.centerY + h / 2)
    }

    /** 切换为正方形：以原截取框中心、较短边为边长，并保持在可用区域内。 */
    fun square(crop: CropBox, bounds: CropBox): CropBox {
        val side = min(min(crop.width, crop.height), min(bounds.width, bounds.height))
        return move(CropBox(crop.centerX - side / 2, crop.centerY - side / 2, crop.centerX + side / 2, crop.centerY + side / 2), 0f, 0f, bounds)
    }

    /** 平移截取框，不超出可用区域。 */
    fun move(crop: CropBox, dx: Float, dy: Float, bounds: CropBox): CropBox {
        val x = (crop.left + dx).coerceIn(bounds.left, max(bounds.left, bounds.right - crop.width))
        val y = (crop.top + dy).coerceIn(bounds.top, max(bounds.top, bounds.bottom - crop.height))
        return CropBox(x, y, x + crop.width, y + crop.height)
    }

    /** 拖动 [corner] 调整大小，对角固定；正方形时宽高相等。边长不小于 [minSize]（可用区域更小时取可用区域）。 */
    fun resize(crop: CropBox, corner: Corner, dx: Float, dy: Float, square: Boolean, bounds: CropBox, minSize: Float): CropBox {
        val left = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT
        val top = corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT
        val fixedX = if (left) crop.right else crop.left
        val fixedY = if (top) crop.bottom else crop.top
        val availableW = if (left) fixedX - bounds.left else bounds.right - fixedX
        val availableH = if (top) fixedY - bounds.top else bounds.bottom - fixedY
        var w = (if (left) crop.width - dx else crop.width + dx)
        var h = (if (top) crop.height - dy else crop.height + dy)
        if (square) {
            val side = if (abs(dx) >= abs(dy)) w else h
            val maxSide = min(availableW, availableH)
            w = side.coerceIn(min(minSize, maxSide), maxSide)
            h = w
        } else {
            w = w.coerceIn(min(minSize, availableW), availableW)
            h = h.coerceIn(min(minSize, availableH), availableH)
        }
        val l = if (left) fixedX - w else fixedX
        val t = if (top) fixedY - h else fixedY
        return CropBox(l, t, l + w, t + h)
    }

    /** 让截取框落在可用区域内：过大时等比缩小（正方形保持正方形），再平移进区域。 */
    fun fitInto(crop: CropBox, bounds: CropBox, square: Boolean): CropBox {
        var w = crop.width
        var h = crop.height
        if (square) {
            val side = min(w, min(bounds.width, bounds.height))
            w = side; h = side
        } else {
            w = min(w, bounds.width); h = min(h, bounds.height)
        }
        val resized = CropBox(crop.centerX - w / 2, crop.centerY - h / 2, crop.centerX + w / 2, crop.centerY + h / 2)
        return move(resized, 0f, 0f, bounds)
    }

    /** 限制图片位置，使显示的图片覆盖截取框；图片在某个方向比截取框小时，在该方向上与截取框居中。 */
    fun clampOffset(offsetX: Float, offsetY: Float, displayedWidth: Float, displayedHeight: Float, crop: CropBox): Pair<Float, Float> {
        val x = if (displayedWidth >= crop.width) offsetX.coerceIn(crop.right - displayedWidth, crop.left) else crop.centerX - displayedWidth / 2
        val y = if (displayedHeight >= crop.height) offsetY.coerceIn(crop.bottom - displayedHeight, crop.top) else crop.centerY - displayedHeight / 2
        return x to y
    }

    /** 以 (pivotX, pivotY) 为中心把缩放从 [scale] 改为 [newScale] 后的图片位置。 */
    fun zoomOffset(offsetX: Float, offsetY: Float, scale: Float, newScale: Float, pivotX: Float, pivotY: Float): Pair<Float, Float> {
        val ratio = newScale / scale
        return pivotX - (pivotX - offsetX) * ratio to pivotY - (pivotY - offsetY) * ratio
    }

    /** 截取框对应的图片像素区域，限制在图片范围内，宽高至少 1；[square] 时宽高相等。 */
    fun toPixels(crop: CropBox, offsetX: Float, offsetY: Float, scale: Float, imageWidth: Int, imageHeight: Int,
                 square: Boolean = false): CropPixels {
        val x = ((crop.left - offsetX) / scale).roundToInt().coerceIn(0, imageWidth - 1)
        val y = ((crop.top - offsetY) / scale).roundToInt().coerceIn(0, imageHeight - 1)
        val w = (crop.width / scale).roundToInt().coerceIn(1, imageWidth - x)
        val h = (crop.height / scale).roundToInt().coerceIn(1, imageHeight - y)
        if (square) return min(w, h).let { CropPixels(x, y, it, it) }
        return CropPixels(x, y, w, h)
    }
}
