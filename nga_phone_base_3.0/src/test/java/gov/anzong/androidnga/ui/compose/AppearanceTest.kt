package gov.anzong.androidnga.ui.compose

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class AppearanceTest {
    @Test fun legacyThemeAndUnknownValuesHaveSafePaletteFallbacks() {
        assertEquals(AppPalette.GREEN, savedPalette(null, "1"))
        assertEquals(AppPalette.SAND, savedPalette(null, "0"))
        assertEquals(AppPalette.GRAPHITE, savedPalette("obsolete", "2"))
        assertEquals(AppPalette.BLUE, savedPalette("BLUE", "0"))
        assertEquals(AppPalette.GREEN, savedPalette("invalid", "999"))
    }
    @Test fun everyPaletteChangesBackgroundAndReadingSurfaceInBothModes() {
        for (dark in listOf(false, true)) {
            val schemes = AppPalette.entries.map { ngaColorScheme(it, dark) }
            assertEquals(4, schemes.map { it.background }.distinct().size)
            assertEquals(4, schemes.map { it.surface }.distinct().size)
            schemes.forEach { assertNotEquals(it.background, it.surface) }
        }
        AppPalette.entries.forEach { assertNotEquals(ngaColorScheme(it, false).background, ngaColorScheme(it, true).background) }
    }
    private fun luminance(color: Color): Double {
        fun channel(value: Float) = if (value <= 0.04045f) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
    @Test fun readingTextAndSecondaryLabelsKeepReadableContrast() {
        for (palette in AppPalette.entries) for (dark in listOf(false, true)) {
            val scheme = ngaColorScheme(palette, dark)
            for (ink in listOf(scheme.onSurface, scheme.onSurfaceVariant)) for (bg in listOf(scheme.surface, scheme.background)) {
                val a = luminance(ink); val b = luminance(bg)
                val contrast = (maxOf(a,b) + .05) / (minOf(a,b) + .05)
                assertTrue("$palette dark=$dark contrast=$contrast", contrast >= 4.5)
            }
        }
    }
    @Test fun webThemeOverridesContainerWhilePreservingPostMarkupAndExplicitTextColor() {
        val original = "<html><head></head><body><span style='color:red'>正文</span><div class='quote'>引用</div></body></html>"
        val result = themedArticleHtml(original,"#DDDDDD","#141E19","#9EC9AB","#1C2821","#304137")
        assertTrue(result.indexOf("nga-compose-theme") < result.indexOf("</head>"))
        assertTrue(result.contains("background:#141E19!important"))
        assertTrue(result.contains("<span style='color:red'>正文</span>"))
        assertTrue(themedArticleHtml("正文", "#111111", "#FFFFFF", "#333333", "#EEEEEE", "#AAAAAA").endsWith("正文"))
    }
}
