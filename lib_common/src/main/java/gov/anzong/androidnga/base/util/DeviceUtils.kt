package gov.anzong.androidnga.base.util

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/** Created by Justwen on 2017/7/16. */
object DeviceUtils {

    private const val DEVICE_NAME_MEIZU = "Meizu"

    @JvmStatic
    val isMeizu: Boolean
        get() = DEVICE_NAME_MEIZU.equals(Build.MANUFACTURER, ignoreCase = true)

    @JvmStatic
    fun isWifiConnected(context: Context): Boolean {
        try {
            val conMan = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val capabilities = conMan?.getNetworkCapabilities(conMan.activeNetwork)
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    @JvmStatic
    fun isLandscape(context: Context): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    @JvmStatic
    val isFullScreenDevice: Boolean
        get() {
            val dm = ContextUtils.getResources().displayMetrics
            return dm.heightPixels.toFloat() / dm.widthPixels >= 1.97f
        }
}
