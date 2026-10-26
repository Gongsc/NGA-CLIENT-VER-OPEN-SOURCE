package gov.anzong.androidnga.ui.compose

import android.content.SharedPreferences
import androidx.compose.runtime.*
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.base.util.ContextUtils
import sp.phone.common.PhoneConfiguration

/** 原生正文与网页正文共享同一个阅读字号，保留现有正文缩放设置。 */
fun articleFontSize(baseSize: Int, zoom: Int): Float = (baseSize.coerceAtLeast(1) * zoom.coerceIn(1, 100) / 100f)

@Composable
fun rememberArticleFontSize(): Float {
    val preferences = remember { ContextUtils.getDefaultSharedPreferences() }
    fun current() = PhoneConfiguration.getInstance().let { articleFontSize(it.topicContentSize, it.webViewTextZoom) }
    var size by remember(preferences) { mutableFloatStateOf(current()) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == PreferenceKey.KEY_TOPIC_CONTENT_SIZE || key == PreferenceKey.KEY_WEBVIEW_TEXT_ZOOM) size = current()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return size
}
