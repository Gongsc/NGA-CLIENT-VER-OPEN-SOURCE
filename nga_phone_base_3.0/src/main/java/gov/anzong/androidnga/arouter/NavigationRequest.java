package gov.anzong.androidnga.arouter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import gov.anzong.androidnga.activity.LoginActivity;
import gov.anzong.androidnga.base.util.ContextUtils;
import sp.phone.common.UserManagerImpl;

public final class NavigationRequest {
    private final String path;
    private final Bundle extras = new Bundle();

    NavigationRequest(String path) { this.path = path; }
    public NavigationRequest withInt(String key, int value) { extras.putInt(key, value); return this; }
    public NavigationRequest withString(String key, String value) { extras.putString(key, value); return this; }
    public NavigationRequest withParcelable(String key, Parcelable value) { extras.putParcelable(key, value); return this; }
    public void navigation() { navigation(ContextUtils.getContext()); }
    public void navigation(Context context) {
        Context target = context != null ? context : ContextUtils.getContext();
        Intent intent = intent(target);
        if (!(target instanceof Activity)) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        target.startActivity(intent);
    }

    /** 目标页面的 Intent，供 {@code registerForActivityResult} 的启动器使用；需要登录时指向登录页。 */
    public Intent intent(Context context) {
        boolean needsLogin = AppNavigator.requiresLogin(path) && UserManagerImpl.getInstance().getActiveUser() == null;
        Intent intent = new Intent(context, needsLogin ? LoginActivity.class : AppNavigator.destination(path));
        if (!needsLogin) intent.putExtras(extras);
        return intent;
    }
}
