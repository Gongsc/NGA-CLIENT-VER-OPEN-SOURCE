package gov.anzong.androidnga.base.util;

import android.os.Handler;
import android.os.Looper;

/**
 * 主线程任务；后台任务使用协程（见 app 模块的 gov.anzong.androidnga.coroutines）。
 *
 * @author yangyihang
 */
public class ThreadUtils {

    private static Handler sHandler = new Handler(Looper.getMainLooper());

    public static boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }

    public static void postOnMainThread(Runnable runnable) {
        postOnMainThreadDelay(runnable, 0);
    }

    public static void postOnMainThreadDelay(Runnable runnable, long delay) {
        sHandler.postDelayed(runnable, delay);
    }

    public static void runOnMainThread(Runnable runnable) {
        if (isMainThread()) {
            runnable.run();
        } else {
            sHandler.post(runnable);
        }
    }

    public static void removeRunnable(Runnable runnable) {
        sHandler.removeCallbacks(runnable);
    }
}
