package gov.anzong.androidnga.mvvm.viewmodel

import androidx.lifecycle.SavedStateHandle
import gov.anzong.androidnga.coroutines.PendingResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Test
import gov.anzong.androidnga.mvvm.model.entity.SubBoard

@OptIn(ExperimentalCoroutinesApi::class)
class SubBoardViewModelTest {
    private val main = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + main)

    private fun board(fid: Int, visible: Boolean = true, type: Int = 0) =
        SubBoard().apply { this.fid = fid; name = "版块$fid"; isChecked = visible; this.type = type }

    @Test fun toggleFollowsServerResultAndAlternatesAction() {
        val calls = mutableListOf<Pair<Int, Boolean>>()
        val vm = SubBoardViewModel(listOf(board(1)), { b, visible -> calls += b.fid to visible; "完成" }, scope, main)
        vm.toggle(0)
        assertFalse(vm.rows[0].visible)
        assertTrue(vm.changed)
        assertEquals("完成", vm.message)
        vm.messageShown()
        assertNull(vm.message)
        // 旧页面成功后总把状态记为显示，第二次会再次发“屏蔽”；这里应改为取消屏蔽。
        vm.toggle(0)
        assertTrue(vm.rows[0].visible)
        assertEquals(listOf(1 to false, 1 to true), calls)
    }

    @Test fun failureKeepsStateAndShowsError() {
        val vm = SubBoardViewModel(listOf(board(1, visible = false)), { _, _ -> throw IllegalStateException("没有权限") }, scope, main)
        vm.toggle(0)
        assertFalse(vm.rows[0].visible)
        assertFalse(vm.rows[0].pending)
        assertFalse(vm.changed)
        assertEquals("没有权限", vm.message)
    }

    @Test fun pendingRowIgnoresRepeatedTogglesAndRowsWithoutSwitchAreSkipped() {
        val pending = PendingResponse<String>()
        var calls = 0
        val vm = SubBoardViewModel(listOf(board(1), board(2, type = -1)), { _, _ -> calls++; pending.await() }, scope, main)
        vm.toggle(0)
        vm.toggle(0)
        vm.toggle(1)
        vm.toggle(5)
        assertEquals(1, calls)
        assertTrue(vm.rows[0].pending)
        assertFalse(vm.rows[1].toggleable)
        pending.complete("完成")
        assertFalse(vm.rows[0].pending)
        assertFalse(vm.rows[0].visible)
    }

    @Test fun readsBoardsFromExtras() {
        // 不经过默认参数：JVM 上无法初始化 AppScope。
        val boards = SubBoardViewModel.boardsOf(SavedStateHandle(mapOf("subBoard" to arrayListOf(board(3, visible = false)))))
        val vm = SubBoardViewModel(boards, { _, _ -> "完成" }, scope, main)
        assertEquals(listOf(3), vm.rows.map { it.board.fid })
        assertFalse(vm.rows[0].visible)
        assertTrue(SubBoardViewModel.boardsOf(SavedStateHandle()).isEmpty())
    }
}
