package gov.anzong.androidnga.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sp.phone.common.Constants
import sp.phone.common.PhoneConfiguration
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsScreen(onBack:()->Unit) {
    val config=PhoneConfiguration.getInstance()
    var zoom by rememberSaveable {mutableFloatStateOf(config.webViewTextZoom.toFloat())}
    Scaffold(containerColor=MaterialTheme.colorScheme.background,topBar={
        TopAppBar(title={Text("字体与阅读大小")},navigationIcon={ActionIcon(Icons.AutoMirrored.Filled.ArrowBack,"返回",onBack)},
            colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))
    }) {padding->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("正文缩放：${zoom.roundToInt()}%",style=MaterialTheme.typography.titleMedium)
            Slider(value=zoom.coerceIn(1f,100f),onValueChange={zoom=it},valueRange=1f..100f,onValueChangeFinished={config.webViewTextZoom=zoom.roundToInt()})
            Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.surface) {
                Text("正文预览：普通回复与富文本使用相同的基础字号。",Modifier.padding(16.dp),fontSize=articleFontSize(config.topicContentSize,zoom.roundToInt()).sp)
            }
            ReadingSizeControl("主题标题字号",config.topicTitleSize,Constants.TOPIC_TITLE_SIZE_MIN,Constants.TOPIC_TITLE_SIZE_MAX,config::setTopicTitleSize)
            ReadingSizeControl("头像大小",config.avatarSize.toFloat(),Constants.AVATAR_SIZE_MIN,Constants.AVATAR_SIZE_MAX,config::setAvatarSize)
            ReadingSizeControl("表情大小",config.emoticonSize.toFloat(),Constants.EMOTICON_SIZE_MIN,Constants.EMOTICON_SIZE_MAX,config::setEmoticonSize)
        }
    }
}

@Composable private fun ReadingSizeControl(label:String,initial:Float,minimum:Int,maximum:Int,save:(Int)->Unit) {
    var value by rememberSaveable {mutableFloatStateOf(initial.coerceIn(minimum.toFloat(),maximum.toFloat()))}
    Text("$label：${value.roundToInt()}",style=MaterialTheme.typography.titleMedium)
    Slider(value,onValueChange={value=it},valueRange=minimum.toFloat()..maximum.toFloat(),steps=(maximum-minimum-1).coerceAtLeast(0),onValueChangeFinished={save(value.roundToInt())})
    if(label=="主题标题字号") Text("NGA 玩家社区 · 标题预览",fontSize=value.sp)
}
