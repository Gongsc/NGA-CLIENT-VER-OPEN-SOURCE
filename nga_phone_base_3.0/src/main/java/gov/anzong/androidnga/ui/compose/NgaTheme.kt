package gov.anzong.androidnga.ui.compose

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun ngaColorScheme(palette: AppPalette, dark: Boolean): ColorScheme {
    val colors = when (palette) {
        AppPalette.GREEN -> if (dark) listOf(0xFF9EC9AB,0xFF141E19,0xFF1C2821,0xFFDFEBE1,0xFF8FA697,0xFF304137,0xFF304C3A)
            else listOf(0xFF3C6C55,0xFFEDF4EE,0xFFF9FCF8,0xFF26372D,0xFF586B5C,0xFFDCE5DD,0xFFDFECE1)
        AppPalette.BLUE -> if (dark) listOf(0xFFA6C7EA,0xFF141D27,0xFF1D2936,0xFFDFEAF5,0xFF9BAEC2,0xFF344353,0xFF2D4663)
            else listOf(0xFF466B91,0xFFEDF3FA,0xFFF9FBFF,0xFF293A4D,0xFF596E83,0xFFDCE5EF,0xFFDCE8F5)
        AppPalette.SAND -> if (dark) listOf(0xFFE1BE90,0xFF211B14,0xFF2C241C,0xFFF0E4D2,0xFFC1AE96,0xFF4B3F30,0xFF5C4328)
            else listOf(0xFF84613E,0xFFF6F0E6,0xFFFFFAF2,0xFF493C2C,0xFF74634E,0xFFE9DFCF,0xFFEEDDC6)
        AppPalette.GRAPHITE -> if (dark) listOf(0xFFBEC9D5,0xFF181C21,0xFF222830,0xFFE3E8EE,0xFFA7B2BE,0xFF3B444F,0xFF3D4B5B)
            else listOf(0xFF576473,0xFFEEF0F2,0xFFFAFBFC,0xFF303B46,0xFF596773,0xFFDDE2E8,0xFFE0E6ED)
    }.map { Color(it) }
    val (primary, bg, surface, ink, muted, line, tint) = colors.let { PaletteColors(it) }
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(primary = primary, onPrimary = if (dark) bg else Color.White,
        primaryContainer = tint, onPrimaryContainer = ink, secondary = primary, onSecondary = if (dark) bg else Color.White,
        secondaryContainer = tint, onSecondaryContainer = ink, tertiary = primary,
        background = bg, onBackground = ink, surface = surface, onSurface = ink,
        surfaceVariant = tint, onSurfaceVariant = muted, outline = muted, outlineVariant = line,
        surfaceTint = primary, inverseSurface = ink, inverseOnSurface = bg,
        surfaceContainerLowest = bg, surfaceContainerLow = bg, surfaceContainer = tint,
        surfaceContainerHigh = tint, surfaceContainerHighest = tint, surfaceBright = surface, surfaceDim = bg)
}
private data class PaletteColors(val primary: Color, val bg: Color, val surface: Color, val ink: Color, val muted: Color, val line: Color, val tint: Color) {
    constructor(values: List<Color>) : this(values[0],values[1],values[2],values[3],values[4],values[5],values[6])
}
private tailrec fun Context.hostActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.hostActivity(); else -> null }

@Composable
fun NgaTheme(content: @Composable () -> Unit) {
    val settings = rememberAppearanceSettings()
    val dark = when (settings.nightMode) { AppNightMode.SYSTEM -> isSystemInDarkTheme(); AppNightMode.DARK -> true; AppNightMode.LIGHT -> false }
    val scheme = ngaColorScheme(settings.palette, dark)
    val view = LocalView.current
    SideEffect {
        view.context.hostActivity()?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalAppearance provides settings) {
        MaterialTheme(colorScheme = scheme) { Surface(color = scheme.background) { content() } }
    }
}
