package gov.anzong.androidnga.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DensityToggle(compact: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)).padding(2.dp)) {
        listOf(false, true).forEach { value ->
            val selected = value == compact
            Surface(shape = RoundedCornerShape(10.dp), color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent) {
                ActionIcon(if (value) ActionIcons.Compact else ActionIcons.Grid,
                    if (value) "紧凑模式" else "标准模式", { onChange(value) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    var reading by rememberSaveable { mutableStateOf(false) }
    BackHandler(reading) { reading = false }
    if (reading) {
        ReadingSettingsScreen { reading = false }
        return
    }
    val settings = LocalAppearance.current
    var modeMenu by remember { mutableStateOf(false) }
    var resetDialog by remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.testTag("appearance_screen"), containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text("外观与布局") }, navigationIcon = {
            ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack)
        }, actions = { ActionIcon(ActionIcons.Restore, "恢复默认设置", { resetDialog = true }) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(ActionIcons.Appearance, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text("让界面成为你喜欢的样子", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("颜色应用于背景、内容与操作区域", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("主题配色", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppPalette.entries.forEach { palette ->
                    val colors = ngaColorScheme(palette, false)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(onClick = { settings.choosePalette(palette) }, shape = RoundedCornerShape(12.dp), color = colors.background,
                            border = BorderStroke(if (settings.palette == palette) 2.dp else 1.dp,
                                if (settings.palette == palette) colors.primary else colors.outlineVariant), modifier = Modifier.fillMaxWidth().height(88.dp)) {
                            Box(Modifier.padding(12.dp)) {
                                Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Box(Modifier.fillMaxWidth().height(6.dp).background(colors.primary, RoundedCornerShape(2.dp)))
                                    repeat(2) { Box(Modifier.fillMaxWidth().height(6.dp).background(colors.primaryContainer, RoundedCornerShape(2.dp))) }
                                }
                                if (settings.palette == palette) Icon(Icons.Default.Check, "已选择${palette.title}", Modifier.size(16.dp).align(Alignment.BottomEnd), tint = colors.primary)
                            }
                        }
                        TextButton(onClick = { settings.choosePalette(palette) }) { Text(palette.title, style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column {
                    Box {
                        TextButton(onClick = { modeMenu = true }, contentPadding = PaddingValues(20.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("深色模式", modifier = Modifier.weight(1f), textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurface)
                            Text(settings.nightMode.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(ActionIcons.Right, null, Modifier.padding(start = 8.dp).size(16.dp))
                        }
                        DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                            AppNightMode.entries.forEach { mode ->
                                DropdownMenuItem(text = { Text(mode.title) }, onClick = { settings.chooseNightMode(mode); modeMenu = false })
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("阅读背景", Modifier.weight(1f))
                        Text("随主题配色", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Surface(onClick = { reading = true }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                ListItem(headlineContent = { Text("字体与阅读大小") },
                    supportingContent = { Text("调整正文、标题、头像和表情大小") },
                    trailingContent = { Icon(ActionIcons.Right, null) })
            }
            Text("列表显示", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("版块密度", style = MaterialTheme.typography.titleSmall)
                    Text("紧凑模式减少留白，保留清晰点击区域", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (settings.compact) "紧凑" else "标准")
                        DensityToggle(settings.compact, settings::chooseCompact)
                    }
                }
            }
        }
    }
    if (resetDialog) AlertDialog(onDismissRequest = { resetDialog = false }, title = { Text("恢复默认外观？") },
        text = { Text("将恢复松绿配色、紧凑列表及跟随系统的深色模式。") },
        confirmButton = { TextButton(onClick = { settings.reset(); resetDialog = false }) { Text("恢复") } },
        dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("取消") } })
}
