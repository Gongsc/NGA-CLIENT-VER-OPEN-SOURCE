package gov.anzong.androidnga.ui.compose

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import gov.anzong.androidnga.common.PreferenceKey

const val PALETTE_KEY = "compose_palette"
const val COMPACT_BOARDS_KEY = "compact_boards"
enum class AppPalette(val title: String) { GREEN("松绿"), BLUE("雾蓝"), SAND("暖砂"), GRAPHITE("石墨") }
enum class AppNightMode(val title: String) { SYSTEM("跟随系统"), LIGHT("浅色"), DARK("深色") }
fun savedPalette(name: String?, legacy: String?): AppPalette = AppPalette.entries.firstOrNull { it.name == name }
    ?: when (legacy) { "0" -> AppPalette.SAND; "2" -> AppPalette.GRAPHITE; else -> AppPalette.GREEN }

@Stable
class AppearanceSettings(private val preferences: SharedPreferences) {
    var palette by mutableStateOf(AppPalette.GREEN); private set
    var compact by mutableStateOf(true); private set
    var nightMode by mutableStateOf(AppNightMode.SYSTEM); private set
    init { reload() }
    fun reload() {
        palette = savedPalette(preferences.getString(PALETTE_KEY, null), preferences.getString(PreferenceKey.MATERIAL_THEME, "1"))
        compact = preferences.getBoolean(COMPACT_BOARDS_KEY, true)
        nightMode = when {
            preferences.getBoolean(PreferenceKey.KEY_NIGHT_MODE_FOLLOW_SYSTEM, !preferences.contains(PreferenceKey.NIGHT_MODE)) -> AppNightMode.SYSTEM
            preferences.getBoolean(PreferenceKey.NIGHT_MODE, false) -> AppNightMode.DARK
            else -> AppNightMode.LIGHT
        }
    }
    fun choosePalette(value: AppPalette) {
        preferences.edit().putString(PALETTE_KEY, value.name).putString(PreferenceKey.MATERIAL_THEME,
            when (value) { AppPalette.SAND -> "0"; AppPalette.GRAPHITE -> "2"; else -> "1" }).apply()
    }
    fun chooseCompact(value: Boolean) { preferences.edit().putBoolean(COMPACT_BOARDS_KEY, value).apply() }
    fun chooseNightMode(value: AppNightMode) {
        preferences.edit().putBoolean(PreferenceKey.KEY_NIGHT_MODE_FOLLOW_SYSTEM, value == AppNightMode.SYSTEM)
            .putBoolean(PreferenceKey.NIGHT_MODE, value == AppNightMode.DARK).apply()
    }
    fun reset() {
        preferences.edit().putString(PALETTE_KEY, AppPalette.GREEN.name).putString(PreferenceKey.MATERIAL_THEME, "1")
            .putBoolean(COMPACT_BOARDS_KEY, true).putBoolean(PreferenceKey.KEY_NIGHT_MODE_FOLLOW_SYSTEM, true)
            .putBoolean(PreferenceKey.NIGHT_MODE, false).apply()
    }
}
val LocalAppearance = staticCompositionLocalOf<AppearanceSettings> { error("缺少外观配置") }

@Composable
fun rememberAppearanceSettings(): AppearanceSettings {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { context.getSharedPreferences(PreferenceKey.PERFERENCE, android.content.Context.MODE_PRIVATE) }
    val settings = remember(preferences) { AppearanceSettings(preferences) }
    DisposableEffect(preferences, settings) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> settings.reload() }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return settings
}
