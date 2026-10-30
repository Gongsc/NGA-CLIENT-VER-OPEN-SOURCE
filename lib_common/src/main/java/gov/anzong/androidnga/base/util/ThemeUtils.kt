package gov.anzong.androidnga.base.util

import android.app.Activity
import android.util.TypedValue
import androidx.core.content.ContextCompat

object ThemeUtils {

    @JvmStatic
    var accentColor = 0
        private set

    @JvmStatic
    var primaryColor = 0
        private set

    @JvmStatic
    fun init(activity: Activity) {
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        primaryColor = ContextCompat.getColor(activity, typedValue.resourceId)

        activity.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        accentColor = ContextCompat.getColor(activity, typedValue.resourceId)
    }
}
