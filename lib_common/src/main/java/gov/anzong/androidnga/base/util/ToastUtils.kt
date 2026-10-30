package gov.anzong.androidnga.base.util

import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Created by Justwen on 2018/8/11.
 */
object ToastUtils {

    @Deprecated("使用 info")
    @JvmStatic
    fun showShortToast(text: String?) {
        info(text)
    }

    @Deprecated("使用 info")
    @JvmStatic
    fun showShortToast(@StringRes strId: Int) {
        info(ContextUtils.getString(strId))
    }

    @Deprecated("使用 info")
    @JvmStatic
    fun showToast(text: String?) {
        info(text)
    }

    @Deprecated("使用 info")
    @JvmStatic
    fun showToast(@StringRes strId: Int) {
        info(ContextUtils.getString(strId))
    }

    @JvmStatic
    fun show(text: String?) {
        ThreadUtils.runOnMainThread { Toast.makeText(ContextUtils.getContext(), text, Toast.LENGTH_SHORT).show() }
    }

    @JvmStatic
    fun success(@StringRes id: Int) {
        success(ContextUtils.getString(id))
    }

    @JvmStatic
    fun error(@StringRes id: Int) {
        error(ContextUtils.getString(id))
    }

    @JvmStatic
    fun info(@StringRes id: Int) {
        info(ContextUtils.getString(id))
    }

    @JvmStatic
    fun success(text: String?) = show(text)

    @JvmStatic
    fun error(text: String?) = show(text)

    @JvmStatic
    fun info(text: String?) = show(text)

    @JvmStatic
    fun warn(text: String?) = show(text)
}
