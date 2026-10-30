package gov.anzong.androidnga

import android.os.DeadSystemException
import android.os.Process
import gov.anzong.androidnga.base.util.PreferenceUtils
import gov.anzong.androidnga.common.PreferenceKey
import java.util.concurrent.TimeoutException

/**
 * 未捕获异常的处理：忽略已知的系统线程异常，WebView 数据目录冲突时下次启动换用新目录，其余交给系统默认处理。
 */
class ExceptionHandlerProxy(private val origExceptionHandler: Thread.UncaughtExceptionHandler?) :
    Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (ex is TimeoutException && thread.name == "FinalizerWatchdogDaemon" ||
            ex is IllegalStateException && thread.name == "GoogleApiHandler"
        ) {
            return
        }
        if (ex is RuntimeException && ex.message?.contains(WEBVIEW_DIRECTORY_CONFLICT) == true) {
            val index = PreferenceUtils.getData(PreferenceKey.KEY_WEBVIEW_DATA_INDEX, 0) + 1
            PreferenceUtils.edit().putInt(PreferenceKey.KEY_WEBVIEW_DATA_INDEX, index).commit()
        }
        if (origExceptionHandler == null || ex is DeadSystemException) {
            Process.killProcess(Process.myPid())
        } else {
            origExceptionHandler.uncaughtException(thread, ex)
        }
    }

    private companion object {
        const val WEBVIEW_DIRECTORY_CONFLICT =
            "Using WebView from more than one process at once with the same data directory is not supported"
    }
}
