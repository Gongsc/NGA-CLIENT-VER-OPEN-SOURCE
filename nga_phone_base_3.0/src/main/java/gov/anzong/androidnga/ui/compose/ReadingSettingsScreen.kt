package gov.anzong.androidnga.ui.compose

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sp.phone.common.Constants
import sp.phone.common.PhoneConfiguration
import kotlin.math.roundToInt

/** 阅读大小的一组取值；[zoom] 为正文缩放百分比，其余与 [PhoneConfiguration] 中的设置一致。 */
data class ReadingSizes(val zoom: Int, val title: Int, val avatar: Int, val emoticon: Int) {
    companion object {
        val Default = ReadingSizes(Constants.WEBVIEW_DEFAULT_TEXT_ZOOM, Constants.TOPIC_TITLE_SIZE_DEFAULT,
            Constants.AVATAR_SIZE_DEFAULT, Constants.EMOTICON_SIZE_DEFAULT)

        // 可选范围：过小的取值会让正文或头像无法辨认，旧版本保存的越界值按范围显示。
        val ZoomRange = 40..100
        val TitleRange = 12..25
        val AvatarRange = 64..140
        val EmoticonRange = 40..200
    }
}

/**
 * 字体与阅读大小：顶部为实时预览（主题列表的一行与一个楼层），下方按“主题列表”“帖子正文”“楼层”分组调整。
 * 拖动时预览随之变化，松手后保存；“恢复默认”同时恢复全部四项。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsScreen(onBack: () -> Unit) {
    val config = PhoneConfiguration.getInstance()
    ReadingSettingsContent(
        initial = ReadingSizes(config.webViewTextZoom, config.topicTitleSize.roundToInt(), config.avatarSize, config.emoticonSize),
        contentSize = config.topicContentSize,
        onSave = { sizes ->
            config.webViewTextZoom = sizes.zoom
            config.setTopicTitleSize(sizes.title)
            config.setAvatarSize(sizes.avatar)
            config.setEmoticonSize(sizes.emoticon)
        },
        onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsContent(initial: ReadingSizes, contentSize: Int, onSave: (ReadingSizes) -> Unit, onBack: () -> Unit) {
    var zoom by rememberSaveable { mutableIntStateOf(initial.zoom.coerceIn(ReadingSizes.ZoomRange)) }
    var title by rememberSaveable { mutableIntStateOf(initial.title.coerceIn(ReadingSizes.TitleRange)) }
    var avatar by rememberSaveable { mutableIntStateOf(initial.avatar.coerceIn(ReadingSizes.AvatarRange)) }
    var emoticon by rememberSaveable { mutableIntStateOf(initial.emoticon.coerceIn(ReadingSizes.EmoticonRange)) }
    var resetDialog by remember { mutableStateOf(false) }
    val save = { onSave(ReadingSizes(zoom, title, avatar, emoticon)) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text("字体与阅读大小") }, navigationIcon = { ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, "返回", onBack) },
            actions = { ActionIcon(ActionIcons.Restore, "恢复默认大小", { resetDialog = true }) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 预览固定在顶部，调整下方任一项时都能看到效果
            ReadingPreview(articleFontSize(contentSize, zoom), title, articleAvatarSize(avatar).value, emoticon,
                Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 12.dp))
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReadingGroup("主题列表") {
                    ReadingSlider("标题字号", title, ReadingSizes.TitleRange, "${title}sp", { title = it }, save, step = 1)
                }
                ReadingGroup("帖子正文") {
                    ReadingSlider("正文大小", zoom, ReadingSizes.ZoomRange, "$zoom%", { zoom = it }, save, step = 5)
                    ReadingSlider("表情大小", emoticon, ReadingSizes.EmoticonRange, "${emoticon}px", { emoticon = it }, save, step = 10)
                }
                ReadingGroup("楼层") {
                    ReadingSlider("头像大小", avatar, ReadingSizes.AvatarRange, "${articleAvatarSize(avatar).value.roundToInt()}dp",
                        { avatar = it }, save, step = 4)
                }
                Text("正文大小同时作用于普通回复与富文本正文；修改后返回帖子即可看到效果。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (resetDialog) AlertDialog(onDismissRequest = { resetDialog = false }, title = { Text("恢复默认大小？") },
        text = { Text("标题、正文、表情与头像大小将恢复为默认值。") },
        confirmButton = {
            TextButton(onClick = {
                ReadingSizes.Default.let { zoom = it.zoom; title = it.title; avatar = it.avatar; emoticon = it.emoticon }
                save(); resetDialog = false
            }) { Text("恢复") }
        },
        dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("取消") } })
}

/** 按当前取值绘制的示例：主题列表中的一行，以及含头像、正文与表情的一个楼层。 */
@Composable
private fun ReadingPreview(bodySize: Float, titleSize: Int, avatarDp: Float, emoticonPx: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val avatar = remember { context.assetImage("default_avatar.png") }
    val emoticon = remember { context.assetImage("ac/-47218_5052bca81a77f.png") }
    Surface(modifier.fillMaxWidth().testTag("reading_preview"), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 1.dp) {
        Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("预览", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("[讨论] 周末一起开黑，有人吗", fontSize = titleSize.sp, fontWeight = FontWeight.Medium)
                Text("示例用户 · 最后回复 刚刚 · 12 回复", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(avatarDp.dp).clip(CircleShape)) {
                    avatar?.let { Image(it, "头像预览", Modifier.fillMaxSize()) }
                }
                Column {
                    Text("示例用户 · 楼主", style = MaterialTheme.typography.labelLarge)
                    Text("#1 · 刚刚", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("这是一段正文示例。调整上方大小时，帖子中的文字、表情与头像会按同样的比例显示。", fontSize = bodySize.sp,
                lineHeight = (bodySize * 1.5f).sp)
            // 网页正文中表情宽度以 CSS 像素设置，在默认缩放下与 dp 一致
            emoticon?.let { Image(it, "表情预览", Modifier.width(emoticonPx.dp).aspectRatio(it.width.toFloat() / it.height)) }
        }
    }
}

@Composable
private fun ReadingGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content)
        }
    }
}

/** 一项大小：名称与当前值、减小/增大按钮与滑块；[onFinished] 在松手或点按钮后保存。 */
@Composable
private fun ReadingSlider(label: String, value: Int, range: IntRange, valueText: String, onChange: (Int) -> Unit,
                          onFinished: () -> Unit, step: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Text(valueText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ActionIcon(ActionIcons.Remove, "减小$label", { onChange((value - step).coerceIn(range)); onFinished() }, enabled = value > range.first)
        Slider(value.toFloat(), { onChange(it.roundToInt().coerceIn(range)) }, Modifier.weight(1f).testTag("reading_slider_$label"),
            valueRange = range.first.toFloat()..range.last.toFloat(), onValueChangeFinished = onFinished)
        ActionIcon(ActionIcons.Add, "增大$label", { onChange((value + step).coerceIn(range)); onFinished() }, enabled = value < range.last)
    }
}

private fun android.content.Context.assetImage(path: String): ImageBitmap? =
    runCatching { assets.open(path).use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
