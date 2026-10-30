package gov.anzong.androidnga.ui.compose

import android.widget.ImageView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import gov.anzong.androidnga.R
import gov.anzong.androidnga.network.ApiConstants
import gov.anzong.androidnga.mvvm.model.entity.Board

// Snapshot mutable legacy models before publishing them to Compose.
data class BoardTile(val fid: Int, val stid: Int, val name: String, val head: String? = null) {
    val key: String get() = "$fid:$stid"
    fun toBoard() = Board(fid, stid, name).also { it.boardHead = head }
}
data class BoardSection(val title: String, val boards: List<BoardTile>)
fun Board.toTile() = BoardTile(fid, stid, name.orEmpty(), boardHead)

// 分类数据可能在同一分组里重复列出同一版面（如游戏专版的魔力宝贝），重复 key 会让 LazyGrid 滚动到该处时崩溃。
internal fun List<BoardSection>.withUniqueBoards() = map { it.copy(boards = it.boards.distinctBy(BoardTile::key)) }

@Composable
fun BoardGrid(
    allSections: List<BoardSection>,
    onBoard: (BoardTile) -> Unit,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    onRemove: (BoardTile) -> Unit = {},
    enabled: Boolean = true,
    emptyMessage: String = "暂无版块",
    compact: Boolean = LocalAppearance.current.compact,
    onControlsExpandedChange: (Boolean) -> Unit = {}
) {
    val sections = remember(allSections) { allSections.withUniqueBoards() }
    if (sections.all { it.boards.isEmpty() }) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val gridState = rememberLazyGridState()
    val scrollConnection = rememberScrollControlsConnection({ gridState.canScrollBackward }, onControlsExpandedChange)
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(maxOf(if (compact) 144.dp else 160.dp, (120 * scale).dp)),
        modifier = modifier.fillMaxSize().nestedScroll(scrollConnection),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalHomeBottomPadding.current),
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val model = remember(board.fid, board.stid) { boardIconModel(context, board) }
    AndroidView(
        factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER; importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO } },
        modifier = Modifier.size(size),
        update = { image ->
            // 重组或复用时图标不变就不再发起加载，滚动时避免重复的 Glide 请求。
            if (image.tag != model) {
                image.tag = model
                Glide.with(image).load(model).placeholder(R.drawable.default_board_icon).into(image)
            }
        }
    )
}

private val boardIconResources = java.util.concurrent.ConcurrentHashMap<String, Int>()

/** 内置图标优先；资源查找较慢，按名称缓存结果。 */
private fun boardIconModel(context: android.content.Context, board: BoardTile): Any {
    if (board.stid != 0) return String.format(ApiConstants.URL_BOARD_ICON_STID, board.stid)
    val name = if (board.fid > 0) "p${board.fid}" else "p_${kotlin.math.abs(board.fid)}"
    val resource = boardIconResources.getOrPut(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    return if (resource != 0) resource else String.format(ApiConstants.URL_BOARD_ICON, board.fid)
}
