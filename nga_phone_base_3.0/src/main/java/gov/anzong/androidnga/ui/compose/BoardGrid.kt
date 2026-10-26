package gov.anzong.androidnga.ui.compose

import android.widget.ImageView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import gov.anzong.androidnga.R
import sp.phone.common.ApiConstants
import sp.phone.mvp.model.entity.Board

// Snapshot mutable legacy models before publishing them to Compose.
data class BoardTile(val fid: Int, val stid: Int, val name: String, val head: String? = null) {
    val key: String get() = "$fid:$stid"
    fun toBoard() = Board(fid, stid, name).also { it.boardHead = head }
}
data class BoardSection(val title: String, val boards: List<BoardTile>)
fun Board.toTile() = BoardTile(fid, stid, name.orEmpty(), boardHead)

@Composable
fun BoardGrid(
    sections: List<BoardSection>,
    onBoard: (BoardTile) -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    onRemove: (BoardTile) -> Unit = {},
    enabled: Boolean = true,
    emptyMessage: String = "暂无版块",
    compact: Boolean = LocalAppearance.current.compact
) {
    if (sections.all { it.boards.isEmpty() }) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    LazyVerticalGrid(
        columns = GridCells.Adaptive(maxOf(if (compact) 144.dp else 160.dp, (120 * scale).dp)),
        modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 8.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 8.dp)
    ) {
        sections.forEachIndexed { sectionIndex, section ->
            if (section.title.isNotBlank()) item(key = "section:$sectionIndex", span = { GridItemSpan(maxLineSpan) }) {
                Text(section.title, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 12.dp))
            }
            itemsIndexed(section.boards, key = { _, board -> "$sectionIndex:${board.key}" }) { _, board ->
                if (compact) {
                    Column(Modifier.testTag("board:${board.key}")) {
                        Surface(onClick = { onBoard(board) }, color = androidx.compose.ui.graphics.Color.Transparent) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                BoardIcon(board, 28.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(board.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                if (editing) ActionIcon(Icons.Default.Close, "取消论坛收藏：${board.name}", { onRemove(board) }, enabled = enabled)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                } else {
                    OutlinedCard(onClick = { onBoard(board) }, modifier = Modifier.fillMaxWidth().testTag("board:${board.key}"),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            BoardIcon(board, 48.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(board.name, style = MaterialTheme.typography.bodyLarge)
                            if (editing) ActionIcon(Icons.Default.Close, "取消论坛收藏：${board.name}", { onRemove(board) }, enabled = enabled)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardIcon(board: BoardTile, size: androidx.compose.ui.unit.Dp) {
    AndroidView(
        factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER; importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO } },
        modifier = Modifier.size(size),
        update = { image ->
            val resourceName = if (board.fid > 0) "p${board.fid}" else "p_${kotlin.math.abs(board.fid)}"
            val resource = if (board.stid == 0) image.resources.getIdentifier(resourceName, "drawable", image.context.packageName) else 0
            val url = if (board.stid != 0) String.format(ApiConstants.URL_BOARD_ICON_STID, board.stid) else String.format(ApiConstants.URL_BOARD_ICON, board.fid)
            Glide.with(image).load(if (resource != 0) resource else url)
                .placeholder(R.drawable.default_board_icon).into(image)
        }
    )
}
