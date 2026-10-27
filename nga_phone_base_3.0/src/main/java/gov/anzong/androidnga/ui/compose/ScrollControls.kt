package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// 仅跟随用户实际滚动；方向累计超过阈值再切换，停止及惯性滚动不会反复展开。
@Composable
internal fun rememberScrollControlsConnection(
    list: LazyListState,
    onControlsExpandedChange: (Boolean) -> Unit,
): NestedScrollConnection {
    val onExpandedChange by rememberUpdatedState(onControlsExpandedChange)
    val threshold = with(LocalDensity.current) { 12.dp.toPx() }
    val scrollConnection = remember(threshold) {
        object : NestedScrollConnection {
            var distance = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && consumed.y != 0f) {
                    if (distance * consumed.y < 0f) distance = 0f
                    distance += consumed.y
                    if (kotlin.math.abs(distance) >= threshold) {
                        onExpandedChange(distance > 0f)
                        distance = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(list) {
        snapshotFlow { !list.canScrollBackward }.collect { atTop ->
            if (atTop) onExpandedChange(true)
        }
    }
    return scrollConnection
}
