package gov.anzong.androidnga.base.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

object ContextUtils {

    /** Application 与进程同生命周期，静态持有不会泄漏。 */
    @SuppressLint("StaticFieldLeak")
    private lateinit var application: Application

    /** 弱引用，页面销毁后不会因这里的引用而无法回收。 */
    private var resumedActivity: WeakReference<Activity>? = null

    /** 当前在前台的 Activity，没有时为 Application。 */
    @JvmStatic
    fun getContext(): Context = resumedActivity?.get() ?: application

    @JvmStatic
    fun setApplication(application: Application) {
        this.application = application
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                resumedActivity = WeakReference(activity)
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                resumedActivity = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity === resumedActivity?.get()) {
                    resumedActivity = null
                }
            }

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activity === resumedActivity?.get()) {
                    resumedActivity = null
                }
            }
        })
    }

    @JvmStatic
    fun getApplication(): Application = application

    @JvmStatic
    fun getDrawable(@DrawableRes id: Int): Drawable? = ContextCompat.getDrawable(getContext(), id)

    @JvmStatic
    @ColorInt
    fun getColor(@ColorRes id: Int): Int = ContextCompat.getColor(getContext(), id)

    @JvmStatic
    fun getString(@StringRes id: Int): String = getContext().getString(id)

    @JvmStatic
    fun getDimension(@DimenRes id: Int): Int = getContext().resources.getDimensionPixelSize(id)

    @JvmStatic
    fun getResources(): Resources = getContext().resources

    @JvmStatic
    fun getDefaultSharedPreferences(): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext())

    @JvmStatic
    fun getSharedPreferences(name: String): SharedPreferences =
        getContext().getSharedPreferences(name, Context.MODE_PRIVATE)
}
