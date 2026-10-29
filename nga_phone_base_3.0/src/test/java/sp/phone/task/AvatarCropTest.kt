package sp.phone.task

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sp.phone.task.AvatarCrop.Corner

class AvatarCropTest {
    private val bounds = CropBox(0f, 0f, 400f, 300f)
    private fun assertBox(expected: CropBox, actual: CropBox) {
        listOf(expected.left to actual.left, expected.top to actual.top, expected.right to actual.right, expected.bottom to actual.bottom)
            .forEach { (e, a) -> assertEquals("$expected vs $actual", e, a, 0.01f) }
    }

    @Test fun initialCropIsCenteredAndSquareByDefault() {
        assertBox(CropBox(80f, 30f, 320f, 270f), AvatarCrop.initial(bounds, square = true))
        assertBox(CropBox(40f, 30f, 360f, 270f), AvatarCrop.initial(bounds, square = false))
        assertEquals(0.5f, AvatarCrop.fitScale(400f, 300f, 800, 400), 0.0001f)
    }

    @Test fun moveStaysInsideBounds() {
        val crop = CropBox(100f, 100f, 200f, 200f)
        assertBox(CropBox(300f, 200f, 400f, 300f), AvatarCrop.move(crop, 500f, 500f, bounds))
        assertBox(CropBox(0f, 0f, 100f, 100f), AvatarCrop.move(crop, -500f, -500f, bounds))
        assertBox(CropBox(110f, 90f, 210f, 190f), AvatarCrop.move(crop, 10f, -10f, bounds))
    }

    @Test fun resizeKeepsOppositeCornerAndLimits() {
        val crop = CropBox(100f, 100f, 200f, 200f)
        assertBox(CropBox(100f, 100f, 250f, 180f), AvatarCrop.resize(crop, Corner.BOTTOM_RIGHT, 50f, -20f, false, bounds, 48f))
        // 正方形：以变化更大的方向为准，并保持宽高相等
        assertBox(CropBox(100f, 100f, 250f, 250f), AvatarCrop.resize(crop, Corner.BOTTOM_RIGHT, 50f, -20f, true, bounds, 48f))
        assertBox(CropBox(60f, 60f, 200f, 200f), AvatarCrop.resize(crop, Corner.TOP_LEFT, -40f, -30f, true, bounds, 48f))
        // 不小于最小边长，不超出可用区域
        assertBox(CropBox(100f, 152f, 148f, 200f), AvatarCrop.resize(crop, Corner.TOP_RIGHT, -1000f, 1000f, false, bounds, 48f))
        val grown = AvatarCrop.resize(crop, Corner.BOTTOM_RIGHT, 1000f, 1000f, true, bounds, 48f)
        assertBox(CropBox(100f, 100f, 300f, 300f), grown)
    }

    @Test fun squareAndFitIntoShrinkTowardsCenter() {
        assertBox(CropBox(150f, 100f, 250f, 200f), AvatarCrop.square(CropBox(100f, 100f, 300f, 200f), bounds))
        assertBox(CropBox(150f, 100f, 250f, 200f), AvatarCrop.fitInto(CropBox(100f, 50f, 300f, 250f), CropBox(150f, 100f, 250f, 200f), true))
    }

    @Test fun offsetKeepsImageCoveringCrop() {
        val crop = CropBox(100f, 100f, 200f, 200f)
        assertEquals(100f to 100f, AvatarCrop.clampOffset(150f, 150f, 300f, 300f, crop))
        assertEquals(-100f to -100f, AvatarCrop.clampOffset(-500f, -500f, 300f, 300f, crop))
        assertEquals(125f to 0f, AvatarCrop.clampOffset(0f, 0f, 50f, 300f, crop))
        assertEquals(-100f to -100f, AvatarCrop.zoomOffset(0f, 0f, 1f, 2f, 100f, 100f))
    }

    @Test fun cropMapsToImagePixels() {
        val crop = CropBox(100f, 50f, 200f, 150f)
        assertEquals(CropPixels(50, 25, 50, 50), AvatarCrop.toPixels(crop, 0f, 0f, 2f, 400, 300))
        assertEquals(CropPixels(40, 0, 60, 60), AvatarCrop.toPixels(CropBox(80f, -20f, 200f, 100f), 0f, 0f, 2f, 100, 100, square = true))
        val pixels = AvatarCrop.toPixels(CropBox(0f, 0f, 1000f, 1000f), 0f, 0f, 1f, 100, 80)
        assertTrue(pixels.x + pixels.width <= 100 && pixels.y + pixels.height <= 80)
    }
}
