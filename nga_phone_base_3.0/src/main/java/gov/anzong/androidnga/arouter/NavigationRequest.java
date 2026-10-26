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
    public void navigation(Context context) { launch(context, null); }
    public void navigation(Activity activity, int requestCode) { launch(activity, requestCode); }

    private void launch(Context requestedContext, Integer requestCode) {
        Context context = requestedContext != null ? requestedContext : ContextUtils.getContext();
        boolean needsLogin = AppNavigator.requiresLogin(path) && UserManagerImpl.getInstance().getActiveUser() == null;
        Intent intent = new Intent(context, needsLogin ? LoginActivity.class : AppNavigator.destination(path));
        if (!needsLogin) intent.putExtras(extras);
        if (context instanceof Activity) {
            if (requestCode == null) context.startActivity(intent);
            else ((Activity) context).startActivityForResult(intent, requestCode);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
