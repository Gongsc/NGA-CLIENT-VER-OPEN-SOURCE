package gov.anzong.androidnga.base.util;

import android.graphics.Color;
import android.widget.Toast;

import androidx.annotation.StringRes;


/**
 * Created by Justwen on 2018/8/11.
 */
public class ToastUtils {

    @Deprecated
    public static void showShortToast(String text) {
        info(text);
    }

    @Deprecated
    public static void showShortToast(@StringRes int strId) {
        showShortToast(ContextUtils.getString(strId));
    }

    @Deprecated
    public static void showToast(String text) {
        showShortToast(text);
    }

    public static void show(String text) {
        ThreadUtils.runOnMainThread(() -> Toast.makeText(ContextUtils.getContext(), text, Toast.LENGTH_SHORT).show());
    }

    @Deprecated
    public static void showToast(@StringRes int strId) {
        showShortToast(strId);
    }

    public static void success(@StringRes int id) {
        success(ContextUtils.getString(id));
    }

    public static void error(@StringRes int id) {
        error(ContextUtils.getString(id));
    }

    public static void info(@StringRes int id) {
        info(ContextUtils.getString(id));
    }

    public static void success(String text) { show(text); }
    public static void error(String text) { show(text); }
    public static void info(String text) { show(text); }
    public static void warn(String text) { show(text); }
}
