package gov.anzong.androidnga.ui.compose

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.common.PreferenceKey as Keys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(preferences: SharedPreferences, onBack: () -> Unit, onNavigate: (String) -> Unit, onClearCache: () -> Unit) {
    var values by remember(preferences) { mutableStateOf(preferences.all.toMap()) }
    DisposableEffect(preferences) {
        val listener=SharedPreferences.OnSharedPreferenceChangeListener {_,_->values=preferences.all.toMap()}
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose{preferences.unregisterOnSharedPreferenceChangeListener(listener)}
    }
    var choice by remember {mutableStateOf<String?>(null)}
    var clearDialog by remember {mutableStateOf(false)}
    val imageOptions=listOf("始终加载","始终关闭","仅 Wi-Fi 下加载")
    val domains=listOf("https://bbs.ngacn.cc","https://bbs.nga.cn","https://nga.178.com","https://nga.donews.com","https://ngabbs.com")
    @Composable fun Entry(title:String, summary:String?=null, enabled:Boolean=true, click:()->Unit) {
        ListItem(headlineContent={Text(title)}, supportingContent=summary?.let{{Text(it)}},
            modifier=Modifier.fillMaxWidth().clickable(enabled=enabled,onClick=click),
            colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.surface))
    }
    @Composable fun Toggle(title:String,key:String,default:Boolean=false,enabled:Boolean=true) {
        val checked=values[key] as? Boolean ?: default
        ListItem(headlineContent={Text(title, color=if(enabled)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)},
            trailingContent={Switch(checked,onCheckedChange=null,enabled=enabled)},
            modifier=Modifier.fillMaxWidth().toggleable(checked,enabled=enabled,role=Role.Switch){preferences.edit().putBoolean(key,it).apply()},
            colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.surface))
    }
    @Composable fun Group(title:String, content:@Composable ColumnScope.()->Unit) {
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(title,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(start=12.dp).semantics { heading() })
            Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {Column(content=content)}
        }
    }
    Scaffold(modifier=Modifier.testTag("settings_screen"),containerColor=MaterialTheme.colorScheme.background,topBar={
        TopAppBar(title={Text("设置")},navigationIcon={ActionIcon(Icons.AutoMirrored.Filled.ArrowBack,"返回",onBack)},
            colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background))
    }) {padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("settings_list"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
            item {Group("阅读与浏览") {
                Entry("字体与阅读大小","调整正文、标题、头像和表情大小"){onNavigate("reading")}
                Toggle("左手模式",Keys.LEFT_HAND)
                Entry("帖子图片",imageOptions.getOrElse((values[Keys.KEY_LOAD_IMAGE_STRATEGY] as? String ?: "0").toIntOrNull()?:0){imageOptions[0]}){choice=Keys.KEY_LOAD_IMAGE_STRATEGY}
                Entry("用户头像",imageOptions.getOrElse((values[Keys.KEY_LOAD_AVATAR_STRATEGY] as? String ?: "0").toIntOrNull()?:0){imageOptions[0]}){choice=Keys.KEY_LOAD_AVATAR_STRATEGY}
                Toggle("显示签名",Keys.SHOW_SIGNATURE)
            }}
            item {Group("主题列表") {
                Toggle("按发帖时间排序",Keys.SORT_BY_POST)
                Toggle("过滤版主标记的低质量主题",Keys.FILTER_SUB_BOARD)
                Entry("关键词屏蔽"){onNavigate("keywords")}
            }}
            item {Group("消息通知") {
                Toggle("回复提醒",Keys.ENABLE_NOTIFIACTION,true)
                Toggle("提醒声音",Keys.NOTIFIACTION_SOUND,true,(values[Keys.ENABLE_NOTIFIACTION] as? Boolean ?: true))
            }}
            item {Group("发帖") {
                Toggle("启用彩色文字",Keys.SHOW_COLORTXT)
                Toggle("发帖后刷新",Keys.REFRESH_AFTERPOST_SETTING_MODE,true)
            }}
            item {Group("账号与屏蔽") {
                Entry("账号管理"){onNavigate("accounts")}
                Entry("用户黑名单"){onNavigate("blacklist")}
            }}
            item {Group("网络与存储") {
                val index=(values[Keys.KEY_NGA_DOMAIN] as? String ?: "1").toIntOrNull()?:1
                Entry("论坛地址",domains.getOrElse(index){domains[1]}){choice=Keys.KEY_NGA_DOMAIN}
                Entry("清除缓存","清除图片与头像缓存，保留账号和论坛收藏"){clearDialog=true}
            }}
        }
    }
    choice?.let{key->
        val options=if(key==Keys.KEY_NGA_DOMAIN) domains else imageOptions
        val selected=(values[key] as? String ?: if(key==Keys.KEY_NGA_DOMAIN)"1" else "0").toIntOrNull()?:0
        AlertDialog(onDismissRequest={choice=null},title={Text(if(key==Keys.KEY_NGA_DOMAIN)"论坛地址" else "加载方式")},text={
            LazyColumn(Modifier.heightIn(max=400.dp)) {itemsIndexed(options) {index,title->
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp).clickable{preferences.edit().putString(key,index.toString()).apply();choice=null}.padding(vertical=8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected==index,onClick=null);Text(title,modifier=Modifier.padding(start=12.dp).semantics { heading() })
                }
            }}
        },confirmButton={TextButton(onClick={choice=null}){Text("取消")}})
    }
    if(clearDialog) AlertDialog(onDismissRequest={clearDialog=false},title={Text("清除缓存")},text={Text("是否清除图片与头像缓存？账号和论坛收藏将保留。")},
        confirmButton={TextButton(onClick={clearDialog=false;onClearCache()}){Text("清除")}},dismissButton={TextButton(onClick={clearDialog=false}){Text("取消")}})
}
