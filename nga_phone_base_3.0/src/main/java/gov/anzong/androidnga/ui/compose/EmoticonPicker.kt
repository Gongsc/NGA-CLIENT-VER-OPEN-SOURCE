package gov.anzong.androidnga.ui.compose

import android.widget.ImageView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import gov.anzong.androidnga.common.util.EmoticonUtils
import gov.anzong.androidnga.core.decode.ForumEmoticonDecoder

/**
 * 表情选择器：只提供网页版有表情代码的分类，选中后插入 `[s:分类:名字]`（与网页版一致），
 * 不再以 `[img]` 插入图片地址。图片仍从本地 assets 显示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoticonPicker(onSelect: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val categories = ForumEmoticonDecoder.CATEGORIES
    val labels = EmoticonUtils.EMOTICON_LABEL.associate { it[0] to it[1] }
    PrimaryScrollableTabRow(selectedTabIndex = tab) {
        categories.forEachIndexed { index, category ->
            Tab(selected = tab == index, onClick = { tab = index }, text = { Text(labels[category] ?: category) })
        }
    }
    val category = categories[tab]
    val options = remember(category) { ForumEmoticonDecoder.emoticons(category).map { (name, image) -> name to "$category/$image" } }
    LazyVerticalGrid(columns = GridCells.Adaptive(72.dp), modifier = Modifier.fillMaxWidth().height(320.dp), contentPadding = PaddingValues(8.dp)) {
        items(options, key = { "$category:${it.first}" }) { (name, asset) ->
            FilledTonalIconButton(onClick = { onSelect(ForumEmoticonDecoder.code(category, name)) },
                modifier = Modifier.padding(4.dp).size(64.dp).semantics { contentDescription = name }) {
                AndroidView(factory = { ImageView(it) }, modifier = Modifier.size(48.dp), update = { image ->
                    Glide.with(image).load("file:///android_asset/$asset").into(image)
                })
            }
        }
    }
}
