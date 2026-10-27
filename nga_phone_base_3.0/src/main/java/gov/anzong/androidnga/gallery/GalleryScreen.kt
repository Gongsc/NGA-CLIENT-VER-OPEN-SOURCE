package gov.anzong.androidnga.gallery

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import gov.anzong.androidnga.ui.compose.ActionIcon
import gov.anzong.androidnga.ui.compose.ActionIcons
import kotlinx.coroutines.launch

/**
 * 图片浏览页：横向分页，双指缩放、双击放大或还原。放大后单指拖动平移图片，分页滑动暂停；离开某页时该页还原。
 *
 * [image] 绘制单张图片，默认用 Glide 加载；测试可替换为本地内容。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    urls: List<String>,
    initialPage: Int,
    onBack: () -> Unit,
    onDownload: (String) -> Unit,
    onShare: (String) -> Unit,
    onDownloadAll: () -> Unit,
    image: @Composable (url: String, state: GalleryZoomState) -> Unit = { url, state -> GlideGalleryImage(url, state) },
) {
    val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, maxOf(urls.size - 1, 0))) { urls.size }
    val zoomStates = remember(urls) { List(urls.size) { GalleryZoomState() } }
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDownloadAll by remember { mutableStateOf(false) }
    val currentUrl = urls.getOrNull(pagerState.currentPage)

    LaunchedEffect(pagerState.settledPage) {
        zoomStates.forEachIndexed { index, state -> if (index != pagerState.settledPage) state.reset() }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("${pagerState.currentPage + 1} / ${urls.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    ActionIcon(ActionIcons.Download, "下载保存", { currentUrl?.let(onDownload) }, enabled = currentUrl != null)
                    IconButton(onClick = { currentUrl?.let(onShare) }, enabled = currentUrl != null) {
                        Icon(Icons.Default.Share, "分享")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "更多") }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("下载全部") }, onClick = {
                                menuExpanded = false
                                confirmDownloadAll = true
                            })
                        }
                    }
                },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = zoomStates.getOrNull(pagerState.currentPage)?.isZoomed != true,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize().padding(padding).testTag("gallery_pager"),
        ) { page ->
            ZoomablePage(zoomStates[page], Modifier.testTag("gallery_page_$page")) { image(urls[page], zoomStates[page]) }
        }
    }

    if (confirmDownloadAll) {
        AlertDialog(
            onDismissRequest = { confirmDownloadAll = false },
            text = { Text("是否要下载全部图片？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDownloadAll = false
                    onDownloadAll()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { confirmDownloadAll = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ZoomablePage(state: GalleryZoomState, modifier: Modifier, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { state.setContainerSize(it.toSize()) }
            .pointerInput(state) {
                detectTapGestures(onDoubleTap = { tap ->
                    val (targetScale, targetOffset) = state.doubleTapTarget(tap)
                    val startScale = state.scale
                    val startOffset = state.offset
                    scope.launch {
                        animate(0f, 1f, animationSpec = tween(220)) { fraction, _ ->
                            state.snapTo(lerp(startScale, targetScale, fraction), lerp(startOffset, targetOffset, fraction))
                        }
                    }
                })
            }
            .pointerInput(state) { detectZoomAndPan(state) },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                },
        ) { content() }
    }
}

/** 双指始终处理缩放；单指只在已放大时平移，未放大时交给分页滑动。 */
private suspend fun PointerInputScope.detectZoomAndPan(state: GalleryZoomState) = awaitEachGesture {
    awaitFirstDown(requireUnconsumed = false)
    do {
        val event = awaitPointerEvent()
        val pressed = event.changes.count { it.pressed }
        if (pressed >= 2 || (pressed == 1 && state.isZoomed)) {
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            if (zoom != 1f || pan != Offset.Zero) {
                state.transform(zoom, pan, event.calculateCentroid(useCurrent = true))
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        }
    } while (event.changes.any { it.pressed })
}

private enum class ImageStatus { Loading, Ready, Failed }

/** 以原始分辨率加载，放大后仍保持清晰；GIF 循环播放。 */
@Composable
fun GlideGalleryImage(url: String, state: GalleryZoomState) {
    var attempt by remember(url) { mutableIntStateOf(0) }
    var status by remember(url, attempt) { mutableStateOf(ImageStatus.Loading) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER } },
            update = { view ->
                val key = "$url#$attempt"
                if (view.tag != key) {
                    view.tag = key
                    Glide.with(view).load(url).override(Target.SIZE_ORIGINAL)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>,
                                                      isFirstResource: Boolean): Boolean {
                                status = ImageStatus.Failed
                                return false
                            }

                            override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>?,
                                                         dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                if (resource is GifDrawable) resource.setLoopCount(GifDrawable.LOOP_FOREVER)
                                state.setIntrinsicSize(resource.intrinsicWidth, resource.intrinsicHeight)
                                status = ImageStatus.Ready
                                return false
                            }
                        })
                        .into(view)
                }
            },
            onRelease = { view -> Glide.with(view).clear(view) },
            modifier = Modifier.fillMaxSize(),
        )
        when (status) {
            ImageStatus.Loading -> CircularProgressIndicator()
            ImageStatus.Failed -> Column(
                Modifier.background(Color.Black.copy(alpha = 0.6f)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("图片加载失败", color = Color.White)
                TextButton(onClick = { attempt++ }) { Text("重试") }
            }
            ImageStatus.Ready -> Unit
        }
    }
}
