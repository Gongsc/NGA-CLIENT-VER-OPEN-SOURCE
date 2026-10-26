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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import gov.anzong.androidnga.common.util.EmoticonUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmoticonPicker(onSelect: (String) -> Unit) {
    var category by rememberSaveable { mutableIntStateOf(0) }
    val names = listOf("常用") + EmoticonUtils.EMOTICON_LABEL.map { it[1] }
    PrimaryScrollableTabRow(selectedTabIndex = category) {
        names.forEachIndexed { index, label -> Tab(selected = category == index, onClick = { category = index }, text = { Text(label) }) }
    }
    val classic = listOf(1, 2, 3, 24, 25, 26, 27, 28, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39, 4, 40, 41, 42, 43, 5, 6, 7, 8)
    val options = if (category == 0) classic.map { "[s:$it]" to "a$it.gif" }
        else EmoticonUtils.EMOTICON_URL[category - 1].map { url -> "[img]$url[/img]" to EmoticonUtils.getPathByURI(url) }
    LazyVerticalGrid(columns = GridCells.Adaptive(72.dp), modifier = Modifier.fillMaxWidth().height(320.dp), contentPadding = PaddingValues(8.dp)) {
        items(options, key = { it.first }) { (code, asset) ->
            FilledTonalIconButton(onClick = { onSelect(code) }, modifier = Modifier.padding(4.dp).size(64.dp)) {
                AndroidView(factory = { ImageView(it) }, modifier = Modifier.size(48.dp), update = { image ->
                    image.contentDescription = code
                    Glide.with(image).load("file:///android_asset/$asset").into(image)
                })
            }
        }
    }
}
