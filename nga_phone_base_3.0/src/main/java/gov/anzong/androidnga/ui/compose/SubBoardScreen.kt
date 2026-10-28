package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.mvvm.viewmodel.SubBoardViewModel.Row

/** 子版块：点击进入版块，开关控制该子版块的主题是否在父版块中显示。 */
@Composable
fun SubBoardScreen(title: String, rows: List<Row>, onOpen: (Row) -> Unit, onToggle: (Int) -> Unit, onBack: () -> Unit) {
    SettingsSubPage(title, onBack) { padding ->
        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text("这个版块没有子版块", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@SettingsSubPage
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("sub_boards")) {
            item {
                Text("关闭开关后，该子版块的主题不在父版块中显示", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            itemsIndexed(rows, key = { index, row -> "$index:${row.board.fid}:${row.board.tidStr}" }) { index, row ->
                ListItem(headlineContent = { Text(row.board.name.orEmpty()) },
                    supportingContent = row.board.description?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
                    trailingContent = if (row.toggleable) ({
                        Box(Modifier.size(52.dp, 32.dp), contentAlignment = Alignment.Center) {
                            if (row.pending) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            else Switch(row.visible, onCheckedChange = { onToggle(index) },
                                modifier = Modifier.semantics { contentDescription = "在父版块显示${row.board.name.orEmpty()}" })
                        }
                    }) else null,
                    modifier = Modifier.clickable { onOpen(row) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface))
            }
        }
    }
}
