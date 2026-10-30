package gov.anzong.androidnga.ui.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.task.CropPixels

/** 头像截取页面；使用合成图片，上传只触发回调。 */
class AvatarCropScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(gif: Boolean = false, uploads: MutableList<CropPixels> = mutableListOf(), events: MutableList<String> = mutableListOf()) {
        compose.setContent {
            NgaTheme {
                AvatarCropScreen(ImageBitmap(800, 600), gif, false, { uploads += it }, { events += "original" }, { events += "cancel" })
            }
        }
        compose.waitForIdle()
    }

    @Test fun defaultCropIsSquareInsideImage() {
        val uploads = mutableListOf<CropPixels>()
        show(uploads = uploads)
        compose.onNodeWithText("原图上传（保留动画）").assertDoesNotExist()
        compose.onNodeWithText("截取并上传").performClick()
        compose.runOnIdle {
            val area = uploads.single()
            assertEquals(area.width, area.height)
            assertTrue(area.x >= 0 && area.y >= 0 && area.x + area.width <= 800 && area.y + area.height <= 600)
            // 初始截取框为可用区域较短边的 80%
            assertEquals(480f, area.width.toFloat(), 4f)
        }
    }

    @Test fun zoomButtonsEnlargeTheImageInsideTheCrop() {
        val uploads = mutableListOf<CropPixels>()
        show(uploads = uploads)
        compose.onNodeWithContentDescription("放大").performClick()
        compose.onNodeWithContentDescription("放大").performClick()
        compose.onNodeWithText("截取并上传").performClick()
        compose.onNodeWithContentDescription("缩小").performClick()
        compose.onNodeWithText("截取并上传").performClick()
        compose.runOnIdle {
            // 每次放大 1.25 倍，截取框不变，对应的图片像素区域按比例变小
            assertEquals(480f / 1.5625f, uploads[0].width.toFloat(), 4f)
            assertEquals(480f / 1.25f, uploads[1].width.toFloat(), 4f)
            assertEquals(uploads[0].width, uploads[0].height)
        }
    }

    @Test fun pinchZoomShrinksPixelAreaAndResetRestores() {
        val uploads = mutableListOf<CropPixels>()
        show(uploads = uploads)
        compose.onNodeWithTag("avatar_crop").performTouchInput {
            pinch(center - Offset(40f, 0f), center - Offset(200f, 0f), center + Offset(40f, 0f), center + Offset(200f, 0f), 300)
        }
        compose.onNodeWithText("截取并上传").performClick()
        compose.onNodeWithContentDescription("重置").performClick()
        compose.onNodeWithText("截取并上传").performClick()
        compose.runOnIdle {
            assertTrue("双指放大后截取区域变小：${uploads[0]}", uploads[0].width < 400)
            assertEquals(480f, uploads[1].width.toFloat(), 4f)
        }
    }

    @Test fun draggingCornerResizesFreelyOrKeepsSquare() {
        val uploads = mutableListOf<CropPixels>()
        show(uploads = uploads)
        var scale = 1f
        // 截取框为图片较短边 80% 的正方形，居中；把右下角向左拖 100 像素
        val dragCorner: TouchInjectionScope.() -> Unit = {
            scale = minOf(width / 800f, height / 600f)
            val corner = center + Offset(240f * scale, 240f * scale)
            swipe(corner, corner - Offset(100f, 0f), 300)
        }
        compose.onNodeWithText("自由").performClick()
        compose.onNodeWithTag("avatar_crop").performTouchInput(dragCorner)
        compose.onNodeWithText("截取并上传").performClick()
        compose.runOnIdle {
            assertEquals(480f - 100f / scale, uploads[0].width.toFloat(), 6f)
            assertEquals(480f, uploads[0].height.toFloat(), 4f)
        }
        compose.onNodeWithText("正方形").performClick()
        compose.onNodeWithText("截取并上传").performClick()
        compose.runOnIdle { assertEquals(uploads[1].width, uploads[1].height) }
    }

    @Test fun gifOffersOriginalUploadAndBackCancels() {
        val events = mutableListOf<String>()
        show(gif = true, events = events)
        compose.onNodeWithText("原图上传（保留动画）").performClick()
        compose.onNodeWithContentDescription("取消截取").performClick()
        compose.runOnIdle { assertEquals(listOf("original", "cancel"), events) }
    }
}
