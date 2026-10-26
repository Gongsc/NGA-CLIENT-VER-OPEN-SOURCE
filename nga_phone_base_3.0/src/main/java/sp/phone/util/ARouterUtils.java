package sp.phone.util;

import android.content.Context;

import gov.anzong.androidnga.arouter.NavigationRequest;
import gov.anzong.androidnga.arouter.AppNavigator;

import gov.anzong.androidnga.base.util.ContextUtils;

/**
 * Created by Justwen on 2019/3/12.
 */
public class ARouterUtils {

    private ARouterUtils() {

    }

    public static NavigationRequest build(String path) {
        return AppNavigator.getInstance().build(path);
    }

    public static void navigation(String path) {
        navigation(null, path);
    }

    public static void navigation(Context context, String path) {
        AppNavigator.getInstance().build(path).navigation(context);
    }

}
