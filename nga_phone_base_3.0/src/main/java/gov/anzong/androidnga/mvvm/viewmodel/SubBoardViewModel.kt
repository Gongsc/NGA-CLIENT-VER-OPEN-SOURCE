package gov.anzong.androidnga.mvvm.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.network.post.ForumActionClient
import gov.anzong.androidnga.mvvm.model.entity.SubBoard

/**
 * 子版块列表与显示开关。开关请求在 [AppScope] 中执行，离开页面也不会中途放弃；
 * 开关状态以服务端结果为准，同一子版块同一时间只发一个请求。
 */
class SubBoardViewModel(
    boards: List<SubBoard>,
    private val setVisible: suspend (SubBoard, Boolean) -> String = { board, visible -> ForumActionClient.create().setSubBoardVisible(board, visible) },
    private val scope: CoroutineScope = AppScope,
    private val worker: CoroutineDispatcher = NgaDispatchers.io,
) : ViewModel() {

    /** 由 Activity 的 extras（"subBoard"）创建。 */
    constructor(state: SavedStateHandle) : this(boardsOf(state))

    /**
     * [visible] 为当前是否显示；[toggleable] 为 false 的子版块（类型小于 0）没有开关。
     * 不用 data class：[SubBoard.equals] 在没有版块键时会抛异常。
     */
    class Row(val board: SubBoard, val visible: Boolean, val pending: Boolean) {
        val toggleable get() = board.type >= 0
        fun copy(visible: Boolean = this.visible, pending: Boolean = this.pending) = Row(board, visible, pending)
    }

    companion object {
        internal fun boardsOf(state: SavedStateHandle): List<SubBoard> = state.get<ArrayList<SubBoard>>("subBoard").orEmpty()
    }

    var rows by mutableStateOf(boards.map { Row(it, it.isChecked, false) })
        private set
    /** 未显示的提示，显示后调用 [messageShown]。 */
    var message by mutableStateOf<String?>(null)
        private set
    /** 有开关修改成功时为 true，返回主题列表后需要刷新。 */
    var changed by mutableStateOf(false)
        private set

    fun toggle(index: Int) {
        val row = rows.getOrNull(index) ?: return
        if (row.pending || !row.toggleable) return
        val target = !row.visible
        update(index) { it.copy(pending = true) }
        scope.launch {
            val error = try {
                message = withContext(worker) { setVisible(row.board, target) }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: "操作失败，请稍后重试"
            }
            if (error == null) {
                row.board.isChecked = target
                changed = true
                update(index) { it.copy(visible = target, pending = false) }
            } else {
                message = error
                update(index) { it.copy(pending = false) }
            }
        }
    }

    fun messageShown() {
        message = null
    }

    private fun update(index: Int, change: (Row) -> Row) {
        rows = rows.toMutableList().also { it[index] = change(it[index]) }
    }
}
