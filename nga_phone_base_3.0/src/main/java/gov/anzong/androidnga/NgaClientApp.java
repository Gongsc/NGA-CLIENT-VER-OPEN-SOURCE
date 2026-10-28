package gov.anzong.androidnga;

import android.app.Application;

import gov.anzong.androidnga.arouter.AppNavigator;



import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.util.PreferenceUtils;
import gov.anzong.androidnga.common.PreferenceKey;
import gov.anzong.androidnga.db.AppDatabase;
import sp.phone.common.FilterKeywordsManagerImpl;
import sp.phone.common.UserManagerImpl;
import sp.phone.common.VersionUpgradeHelper;
import sp.phone.util.NLog;

public class NgaClientApp extends Application {

    private static final String TAG = NgaClientApp.class.getSimpleName();

    private static boolean sNewVersion;

    @Override
    public void onCreate() {
        NLog.w(TAG, "app nga android start");
        ContextUtils.setApplication(this);
        checkNewVersion();
        VersionUpgradeHelper.upgrade();
        AppDatabase.init(this);
        initCoreModule();
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandlerProxy(Thread.getDefaultUncaughtExceptionHandler()));
    }

    private void initCoreModule() {
        UserManagerImpl.getInstance().initialize(this);
        FilterKeywordsManagerImpl.getInstance().initialize(this);
//        // 注册crashHandler
//        CrashHandler.getInstance().init(this);

    }

    private void checkNewVersion() {
        int versionCode = PreferenceUtils.getData(PreferenceKey.VERSION_CODE, 0);
        if (BuildConfig.VERSION_CODE > versionCode) {
            PreferenceUtils.putData(PreferenceKey.PREVIOUS_VERSION_CODE, versionCode);
            PreferenceUtils.putData(PreferenceKey.VERSION_CODE, BuildConfig.VERSION_CODE);
            sNewVersion = true;
            PreferenceUtils.putData(PreferenceKey.KEY_WEBVIEW_DATA_INDEX, 0);
        }
    }

    public static boolean isNewVersion() {
        return sNewVersion;
    }

    public static void setNewVersion(boolean newVersion) {
        sNewVersion = newVersion;
    }

}
