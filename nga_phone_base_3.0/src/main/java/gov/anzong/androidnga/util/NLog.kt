package gov.anzong.androidnga.util

import android.util.Log
import gov.anzong.androidnga.BuildConfig

/**
 * 调试版输出全部日志，正式版只输出错误。
 * Created by elrond on 2017/9/4.
 */
object NLog {

    const val TAG = "NGAClient"

    @JvmStatic
    fun d(tag: String, msg: String): Int = if (BuildConfig.DEBUG) Log.d(tag, msg) else 0

    @JvmStatic
    fun i(tag: String, msg: String): Int = if (BuildConfig.DEBUG) Log.i(tag, msg) else 0

    @JvmStatic
    fun w(tag: String, msg: String): Int = if (BuildConfig.DEBUG) Log.w(tag, msg) else 0

    @JvmStatic
    fun e(tag: String, msg: String): Int = Log.e(tag, msg)

    @JvmStatic
    fun e(msg: String?) {
        if (msg != null) {
            Log.e(TAG, msg)
        }
    }

    @JvmStatic
    fun e(tag: String, msg: String, throwable: Throwable?): Int =
        if (BuildConfig.DEBUG) Log.e(tag, msg, throwable) else 0

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
            Log.d(TAG, "${caller.className.substringAfterLast('.')}.${caller.methodName}():${caller.lineNumber} $msg")
        }
    }
}
