package gov.anzong.androidnga

import android.app.Application
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.base.util.PreferenceUtils
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.db.AppDatabase
import gov.anzong.androidnga.settings.FilterKeywordsManagerImpl
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.settings.VersionUpgradeHelper
import gov.anzong.androidnga.util.NLog

class NgaClientApp : Application() {

    override fun onCreate() {
        NLog.w(TAG, "app nga android start")
        ContextUtils.setApplication(this)
        checkNewVersion()
        VersionUpgradeHelper.upgrade()
        AppDatabase.init(this)
        UserManagerImpl.getInstance().initialize(this)
        FilterKeywordsManagerImpl.getInstance().initialize(this)
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandlerProxy(Thread.getDefaultUncaughtExceptionHandler()))
    }

    private fun checkNewVersion() {
        val versionCode = PreferenceUtils.getData(PreferenceKey.VERSION_CODE, 0)
        if (BuildConfig.VERSION_CODE > versionCode) {
            PreferenceUtils.putData(PreferenceKey.PREVIOUS_VERSION_CODE, versionCode)
            PreferenceUtils.putData(PreferenceKey.VERSION_CODE, BuildConfig.VERSION_CODE)
            newVersion = true
            PreferenceUtils.putData(PreferenceKey.KEY_WEBVIEW_DATA_INDEX, 0)
        }
    }

    companion object {

        private const val TAG = "NgaClientApp"

        /** 本次启动是否为升级后的首次启动，用于显示更新内容。 */
        private var newVersion = false

        @JvmStatic
        fun isNewVersion(): Boolean = newVersion

        @JvmStatic
        fun setNewVersion(value: Boolean) {
            newVersion = value
        }
    }
}
