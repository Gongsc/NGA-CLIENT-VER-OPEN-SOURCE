package gov.anzong.androidnga.common.util

import android.util.Log
import gov.anzong.androidnga.common.BuildConfig

/**
 * 仅在调试版输出的日志。
 * Created by elrond on 2017/9/4.
 */
object LogUtils {

    const val TAG = "NGAClient"

    /** 输出调用位置与消息。 */
    @JvmStatic
    fun d(msg: Any?) {
        if (!BuildConfig.DEBUG) {
            return
        }
        val caller = Thread.currentThread().stackTrace.getOrNull(3)
        if (caller == null) {
            Log.d(TAG, msg.toString())
        } else {
            val className = caller.className.substringAfterLast('.')
            Log.d(TAG, "$className.${caller.methodName}():${caller.lineNumber} $msg")
        }
    }

    @JvmStatic
    fun print(throwable: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, Log.getStackTraceString(throwable))
        }
    }
}
