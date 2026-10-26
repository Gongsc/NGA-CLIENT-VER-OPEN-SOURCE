package sp.phone.common;

import android.content.SharedPreferences;

import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.common.PreferenceKey;

/**
 * @author yangyihang
 */
public class VersionUpgradeHelper {

    public static void upgrade() {
        upgradeSettings();
    }

    private static void upgradeSettings() {
        SharedPreferences sp = ContextUtils.getSharedPreferences(PreferenceKey.PERFERENCE);
        SharedPreferences.Editor editor = sp.edit();
        if (sp.contains(PreferenceKey.DOWNLOAD_AVATAR_NO_WIFI)) {
            boolean value = sp.getBoolean(PreferenceKey.DOWNLOAD_AVATAR_NO_WIFI, true);
            String newValue = value ? "0" : "2";
            editor.putString(
                            ContextUtils.getString(
                                    gov.anzong.androidnga.common.R.string
                                            .pref_load_avatar_strategy),
                            newValue)
                    .remove(PreferenceKey.DOWNLOAD_AVATAR_NO_WIFI)
                    .apply();
        }

        if (sp.contains(PreferenceKey.DOWNLOAD_IMG_NO_WIFI)) {
            boolean value = sp.getBoolean(PreferenceKey.DOWNLOAD_IMG_NO_WIFI, true);
            String newValue = value ? "0" : "2";
            editor.putString(
                            ContextUtils.getString(
                                    gov.anzong.androidnga.common.R.string.pref_load_pic_strategy),
                            newValue)
                    .remove(PreferenceKey.DOWNLOAD_IMG_NO_WIFI)
                    .apply();
        }
    }
}
