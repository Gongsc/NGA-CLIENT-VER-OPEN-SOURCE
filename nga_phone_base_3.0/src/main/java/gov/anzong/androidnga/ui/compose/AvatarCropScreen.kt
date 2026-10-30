package gov.anzong.androidnga.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.task.AvatarCrop
import gov.anzong.androidnga.task.CropBox
import gov.anzong.androidnga.task.CropPixels
import kotlin.math.hypot
import kotlin.math.roundToInt

/** 截取状态：图片缩放与位置、截取框；计算见 [AvatarCrop]。视图尺寸确定后由 [layout] 初始化。 */
class AvatarCropState(val imageWidth: Int, val imageHeight: Int) {
    var square by mutableStateOf(true); private set
    var scale by mutableFloatStateOf(1f); private set
    var offsetX by mutableFloatStateOf(0f); private set
    var offsetY by mutableFloatStateOf(0f); private set
    var crop by mutableStateOf<CropBox?>(null); private set
    private var view = CropBox(0f, 0f, 0f, 0f)
    private var minScale = 1f

    val zoom get() = scale / minScale

    private fun bounds() = AvatarCrop.displayed(imageWidth, imageHeight, scale, offsetX, offsetY).intersect(view)

    /** 视图尺寸变化时重新完整显示图片并放置截取框。 */
    fun layout(width: Float, height: Float) {
        if (width <= 0 || height <= 0 || (view.width == width && view.height == height && crop != null)) return
        view = CropBox(0f, 0f, width, height)
        minScale = AvatarCrop.fitScale(width, height, imageWidth, imageHeight)
        scale = minScale
        offsetX = (width - imageWidth * scale) / 2
        offsetY = (height - imageHeight * scale) / 2
        crop = AvatarCrop.initial(bounds(), square)
    }

    fun reset() {
        val (w, h) = view.width to view.height
        view = CropBox(0f, 0f, 0f, 0f)
        crop = null
        layout(w, h)
    }

    fun chooseSquare(value: Boolean) {
        square = value
        val current = crop ?: return
        if (value) crop = AvatarCrop.square(current, bounds())
    }

    /** 以 (pivotX, pivotY) 为中心缩放图片并平移 (panX, panY)；缩放范围为完整显示到 [AvatarCrop.MAX_ZOOM] 倍。 */
    fun transform(pivotX: Float, pivotY: Float, zoomFactor: Float, panX: Float, panY: Float) {
        val current = crop ?: return
        val newScale = (scale * zoomFactor).coerceIn(minScale, minScale * AvatarCrop.MAX_ZOOM)
        val (zx, zy) = AvatarCrop.zoomOffset(offsetX, offsetY, scale, newScale, pivotX, pivotY)
        scale = newScale
        offsetX = zx + panX
        offsetY = zy + panY
        // 截取框保持不动、限制图片位置使其覆盖截取框；只有图片缩小到比截取框还小时才缩小截取框。
        val displayedWidth = imageWidth * scale
        val displayedHeight = imageHeight * scale
        var fitted = current
        if (displayedWidth < current.width || displayedHeight < current.height) {
            val w = minOf(displayedWidth, view.width)
            val h = minOf(displayedHeight, view.height)
            val area = AvatarCrop.move(CropBox(current.centerX - w / 2, current.centerY - h / 2, current.centerX + w / 2,
                current.centerY + h / 2), 0f, 0f, view)
            fitted = AvatarCrop.fitInto(current, area, square)
        }
        crop = fitted
        val (cx, cy) = AvatarCrop.clampOffset(offsetX, offsetY, imageWidth * scale, imageHeight * scale, fitted)
        offsetX = cx
        offsetY = cy
    }

    /** 以截取框中心缩放，供按钮使用。 */
    fun zoomBy(factor: Float) {
        val current = crop ?: return
        transform(current.centerX, current.centerY, factor, 0f, 0f)
    }

    fun moveCrop(dx: Float, dy: Float) {
        crop = AvatarCrop.move(crop ?: return, dx, dy, bounds())
    }

    fun resizeCrop(corner: AvatarCrop.Corner, dx: Float, dy: Float, minSize: Float) {
        crop = AvatarCrop.resize(crop ?: return, corner, dx, dy, square, bounds(), minSize)
    }

    /** 按下位置命中的角（[radius] 内），否则 null。 */
    fun cornerAt(x: Float, y: Float, radius: Float): AvatarCrop.Corner? {
        val c = crop ?: return null
        return listOf(
            AvatarCrop.Corner.TOP_LEFT to Offset(c.left, c.top), AvatarCrop.Corner.TOP_RIGHT to Offset(c.right, c.top),
            AvatarCrop.Corner.BOTTOM_LEFT to Offset(c.left, c.bottom), AvatarCrop.Corner.BOTTOM_RIGHT to Offset(c.right, c.bottom),
        ).filter { (_, p) -> hypot(p.x - x, p.y - y) <= radius }.minByOrNull { (_, p) -> hypot(p.x - x, p.y - y) }?.first
    }

    fun pixels(): CropPixels? = crop?.let { AvatarCrop.toPixels(it, offsetX, offsetY, scale, imageWidth, imageHeight, square) }
}

/**
 * 截取头像：双指缩放、拖动图片，拖动截取框内部移动、拖动四角调整大小；可切换正方形或自由比例。
 * [image] 为空时显示加载中；[gif] 时额外提供不截取的原图上传以保留动画。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarCropScreen(image: ImageBitmap?, gif: Boolean, sending: Boolean, onUpload: (CropPixels) -> Unit,
                     onUploadOriginal: () -> Unit, onCancel: () -> Unit) {
    BackHandler(enabled = !sending, onBack = onCancel)
    val state = remember(image) { image?.let { AvatarCropState(it.width, it.height) } }
    Scaffold(containerColor = Color.Black, contentColor = Color.White, topBar = {
        TopAppBar(title = { Text("截取头像") },
            navigationIcon = { ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "取消截取", onCancel, enabled = !sending) },
            actions = { ActionIcon(Icons.Default.Refresh, "重置", { state?.reset() }, enabled = state != null && !sending) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = Color.White,
                navigationIconContentColor = Color.White, actionIconContentColor = Color.White))
    }, bottomBar = {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    listOf(true to "正方形", false to "自由").forEachIndexed { index, (value, label) ->
                        SegmentedButton(selected = state?.square == value, onClick = { state?.chooseSquare(value) }, enabled = state != null && !sending,
                            shape = SegmentedButtonDefaults.itemShape(index, 2), colors = SegmentedButtonDefaults.colors(
                                inactiveContainerColor = Color.Transparent, inactiveContentColor = Color.White,
                                inactiveBorderColor = Color.White.copy(alpha = 0.6f), activeBorderColor = Color.White.copy(alpha = 0.6f))) { Text(label) }
                    }
                }
                FilledTonalIconButton(onClick = { state?.zoomBy(1 / 1.25f) }, enabled = state != null && !sending) {
                    Text("−", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { contentDescription = "缩小" })
                }
                FilledTonalIconButton(onClick = { state?.zoomBy(1.25f) }, enabled = state != null && !sending) {
                    Icon(Icons.Default.Add, "放大")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (gif) TextButton(onClick = onUploadOriginal, enabled = state != null && !sending,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("原图上传（保留动画）") }
                Button(onClick = { state?.pixels()?.let(onUpload) }, enabled = state?.crop != null && !sending) {
                    if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text("截取并上传")
                }
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
            if (image == null || state == null) CircularProgressIndicator(color = Color.White)
            else CropCanvas(image, state, enabled = !sending)
        }
    }
}

@Composable
private fun CropCanvas(image: ImageBitmap, state: AvatarCropState, enabled: Boolean) {
    val density = LocalDensity.current
    val handleRadius = with(density) { 28.dp.toPx() }
    val minSize = with(density) { 48.dp.toPx() }
    val stroke = with(density) { 2.dp.toPx() }
    val handle = with(density) { 6.dp.toPx() }
    Canvas(Modifier.fillMaxSize().testTag("avatar_crop")
        .semantics { contentDescription = "截取区域，双指缩放图片，拖动截取框或其四角调整" }
        .onSizeChanged { state.layout(it.width.toFloat(), it.height.toFloat()) }
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .pointerInput(state, enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                val corner = state.cornerAt(down.position.x, down.position.y, handleRadius)
                var target = when {
                    corner != null -> 1
                    state.crop?.contains(down.position.x, down.position.y) == true -> 2
                    else -> 0
                }
                do {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.count { it.pressed }
                    if (pressed >= 2) {
                        // 双指始终缩放、平移图片
                        target = 0
                        val centroid = event.calculateCentroid()
                        val pan = event.calculatePan()
                        state.transform(centroid.x, centroid.y, event.calculateZoom(), pan.x, pan.y)
                    } else {
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.first()
                        val delta = change.positionChange()
                        when (target) {
                            1 -> state.resizeCrop(corner!!, delta.x, delta.y, minSize)
                            2 -> state.moveCrop(delta.x, delta.y)
                            else -> state.transform(0f, 0f, 1f, delta.x, delta.y)
                        }
                    }
                    event.changes.forEach { if (it.positionChange() != Offset.Zero) it.consume() }
                } while (event.changes.any { it.pressed })
            }
        }) {
        drawImage(image, dstOffset = IntOffset(state.offsetX.roundToInt(), state.offsetY.roundToInt()),
            dstSize = IntSize((image.width * state.scale).roundToInt(), (image.height * state.scale).roundToInt()),
            filterQuality = FilterQuality.Medium)
        val crop = state.crop ?: return@Canvas
        val topLeft = Offset(crop.left, crop.top)
        val cropSize = Size(crop.width, crop.height)
        drawRect(Color.Black.copy(alpha = 0.55f))
        drawRect(Color.Transparent, topLeft, cropSize, blendMode = BlendMode.Clear)
        drawImage(image, dstOffset = IntOffset(state.offsetX.roundToInt(), state.offsetY.roundToInt()),
            dstSize = IntSize((image.width * state.scale).roundToInt(), (image.height * state.scale).roundToInt()),
            filterQuality = FilterQuality.Medium, blendMode = BlendMode.DstOver)
        for (i in 1..2) {
            val x = crop.left + crop.width * i / 3
            val y = crop.top + crop.height * i / 3
            drawLine(Color.White.copy(alpha = 0.5f), Offset(x, crop.top), Offset(x, crop.bottom), 1f)
            drawLine(Color.White.copy(alpha = 0.5f), Offset(crop.left, y), Offset(crop.right, y), 1f)
        }
        drawRect(Color.White, topLeft, cropSize, style = Stroke(stroke))
        listOf(topLeft, Offset(crop.right, crop.top), Offset(crop.left, crop.bottom), Offset(crop.right, crop.bottom)).forEach {
            drawCircle(Color.White, handle, it)
        }
    }
}
