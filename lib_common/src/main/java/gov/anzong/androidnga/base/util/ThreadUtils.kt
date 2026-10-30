package gov.anzong.androidnga.base.util

import android.os.Handler
import android.os.Looper

/**
 * 主线程任务；后台任务使用协程（见 app 模块的 gov.anzong.androidnga.coroutines）。
 *
 * @author yangyihang
 */
object ThreadUtils {

    private val handler = Handler(Looper.getMainLooper())

    @JvmStatic
    val isMainThread: Boolean
        get() = Thread.currentThread() === Looper.getMainLooper().thread

    @JvmStatic
    fun postOnMainThread(runnable: Runnable) {
        postOnMainThreadDelay(runnable, 0)
    }

    @JvmStatic
    fun postOnMainThreadDelay(runnable: Runnable, delay: Long) {
        handler.postDelayed(runnable, delay)
    }

    @JvmStatic
    fun runOnMainThread(runnable: Runnable) {
        if (isMainThread) {
            runnable.run()
        } else {
            handler.post(runnable)
        }
    }

    @JvmStatic
    fun removeRunnable(runnable: Runnable) {
        handler.removeCallbacks(runnable)
    }
}
