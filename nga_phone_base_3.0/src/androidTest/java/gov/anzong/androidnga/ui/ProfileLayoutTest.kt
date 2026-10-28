package gov.anzong.androidnga.ui

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.card.MaterialCardView
import gov.anzong.androidnga.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** 个人资料页布局在应用的各套主题及深浅色下都能加载卡片；只加载布局，不发起请求。 */
class ProfileLayoutTest {

    private val themes = listOf(
        R.style.AppThemeDayNightBrown, R.style.AppThemeDayNightGreen, R.style.AppThemeDayNightBlack,
        R.style.AppThemeDayNightBrown_NoActionBar, R.style.AppThemeDayNightGreen_NoActionBar,
        R.style.AppThemeDayNightBlack_NoActionBar,
    )

    @Test fun profileCardsInflateUnderEveryAppTheme() {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        fun withNightMode(mode: Int) = target.createConfigurationContext(Configuration(target.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or mode
        })
        val base = withNightMode(Configuration.UI_MODE_NIGHT_NO)
        // 深色模式下卡片改用深色底，与卡片内网页正文一致
        val night = withNightMode(Configuration.UI_MODE_NIGHT_YES)
        assertEquals(0xFFFFF5D7.toInt(), base.getColor(R.color.profilebgcolor))
        assertEquals(night.getColor(R.color.profilebgcolor), night.getColor(R.color.background_color))
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (theme in themes) for (context in listOf(base, night)) {
                val root = LayoutInflater.from(ContextThemeWrapper(context, theme))
                    .inflate(R.layout.activity_user_profile_content, null, false)
                val cards = collect(root).filterIsInstance<MaterialCardView>()
                assertEquals("主题 $theme 下的卡片数", 4, cards.size)
                cards.forEach { card ->
                    assertEquals(context.getColor(R.color.profilebgcolor), card.cardBackgroundColor.defaultColor)
                    assertEquals(0, card.strokeWidth)
                }
            }
        }
    }

    private fun collect(view: View): List<View> =
        if (view is ViewGroup) listOf(view) + (0 until view.childCount).flatMap { collect(view.getChildAt(it)) } else listOf(view)
}
