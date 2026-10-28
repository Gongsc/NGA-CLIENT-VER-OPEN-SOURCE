package gov.anzong.androidnga.ui

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.card.MaterialCardView
import gov.anzong.androidnga.R
import org.junit.Assert.assertEquals
import org.junit.Test

/** 个人资料页布局在应用的各套主题下都能加载卡片；只加载布局，不发起请求。 */
class ProfileLayoutTest {

    private val themes = listOf(
        R.style.AppThemeDayNightBrown, R.style.AppThemeDayNightGreen, R.style.AppThemeDayNightBlack,
        R.style.AppThemeDayNightBrown_NoActionBar, R.style.AppThemeDayNightGreen_NoActionBar,
        R.style.AppThemeDayNightBlack_NoActionBar,
    )

    @Test fun profileCardsInflateUnderEveryAppTheme() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (theme in themes) {
                val root = LayoutInflater.from(ContextThemeWrapper(context, theme))
                    .inflate(R.layout.activity_user_profile_content, null, false)
                val cards = collect(root).filterIsInstance<MaterialCardView>()
                assertEquals("主题 $theme 下的卡片数", 4, cards.size)
                cards.forEach { card ->
                    assertEquals(0xFFFFF5D7.toInt(), card.cardBackgroundColor.defaultColor)
                    assertEquals(0, card.strokeWidth)
                }
            }
        }
    }

    private fun collect(view: View): List<View> =
        if (view is ViewGroup) listOf(view) + (0 until view.childCount).flatMap { collect(view.getChildAt(it)) } else listOf(view)
}
