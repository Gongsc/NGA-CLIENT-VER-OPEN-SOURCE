package gov.anzong.androidnga.gallery

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryZoomStateTest {

    /** 1000×2000 容器中显示 1000×500 的横图，适配后图片高 500。 */
    private fun landscape() = GalleryZoomState().apply {
        setContainerSize(Size(1000f, 2000f))
        setIntrinsicSize(2000, 1000)
    }

    private fun assertOffset(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.01f)
        assertEquals(expected.y, actual.y, 0.01f)
    }

    @Test
    fun pinchKeepsContentUnderCentroidAndClampsScale() {
        val state = landscape()
        state.transform(zoom = 2f, pan = Offset.Zero, centroid = Offset(750f, 1000f))
        assertEquals(2f, state.scale)
        // 右侧 1/4 处的内容保持在手势中心下：偏移 = -(750-500)。
        assertOffset(Offset(-250f, 0f), state.offset)
        assertTrue(state.isZoomed)
        state.transform(zoom = 100f, pan = Offset.Zero, centroid = Offset(500f, 1000f))
        assertEquals(GalleryZoomState.MAX_SCALE, state.scale)
        state.transform(zoom = 0.001f, pan = Offset(30f, 30f), centroid = Offset(500f, 1000f))
        assertEquals(1f, state.scale)
        assertOffset(Offset.Zero, state.offset)
        assertFalse(state.isZoomed)
    }

    @Test
    fun panIsLimitedToDisplayedImage() {
        val state = landscape()
        state.snapTo(2f, Offset(5000f, 5000f))
        // 宽 1000×2 超出容器 1000，可平移 500；高 500×2=1000 小于容器 2000，保持居中。
        assertOffset(Offset(500f, 0f), state.offset)
        state.snapTo(4f, Offset(-5000f, -5000f))
        assertOffset(Offset(-1500f, 0f), state.offset)
    }

    @Test
    fun unknownImageSizeFallsBackToContainer() {
        val state = GalleryZoomState().apply { setContainerSize(Size(1000f, 2000f)) }
        state.snapTo(2f, Offset(5000f, -5000f))
        assertOffset(Offset(500f, -1000f), state.offset)
    }

    @Test
    fun doubleTapZoomsAroundTapThenRestores() {
        val state = landscape()
        val (scale, offset) = state.doubleTapTarget(Offset(900f, 1000f))
        assertEquals(GalleryZoomState.DOUBLE_TAP_SCALE, scale)
        // 理论偏移 -(900-500)×1.5 = -600，受限于 (1000×2.5-1000)/2 = 750 之内。
        assertOffset(Offset(-600f, 0f), offset)
        state.snapTo(scale, offset)
        assertEquals(1f to Offset.Zero, state.doubleTapTarget(Offset(100f, 100f)))
        state.reset()
        assertFalse(state.isZoomed)
    }

    @Test
    fun argumentsPickCurrentImage() {
        assertEquals(listOf("a", "b", "c") to 1, galleryArguments(arrayOf("a", "b", "c"), "b"))
        assertEquals(listOf("a", "b") to 0, galleryArguments(arrayOf("a", "b"), "x"))
        assertEquals(listOf("only") to 0, galleryArguments(null, "only"))
        assertEquals(emptyList<String>() to 0, galleryArguments(null, null))
    }
}
