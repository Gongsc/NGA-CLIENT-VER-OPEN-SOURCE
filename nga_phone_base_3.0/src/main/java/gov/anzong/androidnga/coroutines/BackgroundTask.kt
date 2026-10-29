package gov.anzong.androidnga.coroutines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** 后台串行任务：按提交顺序在 [NgaDispatchers.io] 上逐个执行，不随页面取消。 */
object BackgroundTask {

    private val serialQueue = Channel<Runnable>(Channel.UNLIMITED)
    private val serialWorker by lazy {
        AppScope.launch(NgaDispatchers.io) {
            for (work in serialQueue) {
                try {
                    work.run()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 按提交顺序逐个在后台执行 [work]，用于先后写入同一份数据（如保存账号列表），
     * 后提交的不会被先提交的覆盖。某个任务失败不影响之后的任务。
     */
    @JvmStatic
    fun runSerially(work: Runnable) {
        serialWorker
        serialQueue.trySend(work)
    }
}
