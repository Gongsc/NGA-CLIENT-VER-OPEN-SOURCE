package gov.anzong.androidnga.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable
import java.util.function.Consumer

/**
 * 替代 AsyncTask：[work] 在 [NgaDispatchers.io] 执行阻塞操作，结果在主线程交给 [onResult]。
 *
 * 不随页面取消，与原 AsyncTask 一致；[work] 抛出的异常不被吞掉，与 AsyncTask 的 doInBackground 相同。
 * 多个任务并行执行，不再像 AsyncTask.execute 那样全局串行。
 */
object BackgroundTask {

    @JvmStatic
    fun <T> run(work: Callable<T>, onResult: Consumer<T>): Job =
        AppScope.launch(Dispatchers.Main.immediate) {
            val result = withContext(NgaDispatchers.io) { work.call() }
            onResult.accept(result)
        }
}
