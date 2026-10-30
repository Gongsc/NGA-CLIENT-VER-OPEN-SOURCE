package gov.anzong.androidnga.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.base.util.ThreadUtils
import gov.anzong.androidnga.common.PreferenceKey

/** 日夜模式与正文网页的颜色；配色由 Compose 的 NgaTheme 管理。 */
class ThemeManager private constructor() : SharedPreferences.OnSharedPreferenceChangeListener {

    private val appThemes = intArrayOf(
        R.style.AppThemeDayNightBrown_NoActionBar,
        R.style.AppThemeDayNightGreen_NoActionBar,
        R.style.AppThemeDayNightBlack_NoActionBar,
    )

    private val appThemesActionBar = intArrayOf(
        R.style.AppThemeDayNightBrown,
        R.style.AppThemeDayNightGreen,
        R.style.AppThemeDayNightBlack,
    )

    private val context: Context = ContextUtils.getContext()

    private var themeIndex: Int

    private var nightMode: Boolean

    var isNightModeFollowSystem: Boolean
        private set

    private var webViewTheme: WebViewTheme? = null

    init {
        val sp = context.getSharedPreferences(PreferenceKey.PERFERENCE, Context.MODE_PRIVATE)
        sp.registerOnSharedPreferenceChangeListener(this)
        nightMode = sp.getBoolean(PreferenceKey.NIGHT_MODE, false)
        themeIndex = sp.getString(PreferenceKey.MATERIAL_THEME, "1")!!.toInt()
        isNightModeFollowSystem = sp.getBoolean(PreferenceKey.KEY_NIGHT_MODE_FOLLOW_SYSTEM, !sp.contains(PreferenceKey.NIGHT_MODE))
        applyDayNightDelay(0)
    }

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String?) {
        when (key) {
            PreferenceKey.NIGHT_MODE -> {
                nightMode = sp.getBoolean(key, false)
                applyDayNightDelay(200)
            }
            PreferenceKey.MATERIAL_THEME -> themeIndex = sp.getString(key, "0")!!.toInt()
            PreferenceKey.KEY_NIGHT_MODE_FOLLOW_SYSTEM -> {
                isNightModeFollowSystem = sp.getBoolean(key, !sp.contains(PreferenceKey.NIGHT_MODE))
                applyDayNightDelay(200)
            }
        }
        webViewTheme = null
    }

    private fun applyDayNightDelay(delay: Long) {
        ThreadUtils.postOnMainThreadDelay({
            AppCompatDelegate.setDefaultNightMode(
                when {
                    isNightModeFollowSystem -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    nightMode -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_NO
                }
            )
        }, delay)
    }

    fun initializeWebTheme(context: Context) {
        if (webViewTheme == null) {
            webViewTheme = WebViewTheme(context)
        }
    }

    @ColorRes
    fun getForegroundColor(): Int = R.color.foreground_color

    @ColorRes
    fun getBackgroundColor(position: Int): Int = if (position % 2 == 1) R.color.background_color2 else R.color.background_color

    /** 跟随系统时以当前资源的夜间配置为准；设置后写入偏好。 */
    var isNightMode: Boolean
        get() = if (isNightModeFollowSystem) ContextUtils.getResources().getBoolean(R.bool.night_mode) else nightMode
        set(value) {
            ContextUtils.getSharedPreferences(PreferenceKey.PERFERENCE).edit { putBoolean(PreferenceKey.NIGHT_MODE, value) }
        }

    private fun webTheme(): WebViewTheme = webViewTheme ?: WebViewTheme(context).also { webViewTheme = it }

    @get:ColorInt
    val webTextColor: Int
        get() = webTheme().webTextColor

    @get:ColorInt
    val webQuoteBackgroundColor: Int
        get() = webTheme().quoteBackgroundColor

    @StyleRes
    fun getTheme(toolbarEnabled: Boolean): Int {
        val index = if (isNightMode) 0 else themeIndex
        return if (toolbarEnabled) appThemes[index] else appThemesActionBar[index]
    }

    companion object {

        private val INSTANCE by lazy { ThemeManager() }

        @JvmStatic
        fun getInstance(): ThemeManager = INSTANCE
    }
}
