package gov.anzong.androidnga.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import com.bumptech.glide.Glide
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.base.util.ThreadUtils
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.ui.compose.*
import sp.phone.common.UserManagerImpl

class SettingsActivity : BaseActivity() {
    override fun usesComposeContent()=true
    override fun onCreate(savedInstanceState:Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val preferences=getSharedPreferences(PreferenceKey.PERFERENCE,Context.MODE_PRIVATE)
        setContent {NgaTheme {
            var reading by remember {mutableStateOf(false)}
            BackHandler(reading){reading=false}
            if(reading) ReadingSettingsScreen {reading=false}
            else SettingsScreen(preferences,{finish()}, {destination->
                if(destination=="reading") reading=true
                else {
                    val fragment=when(destination) {
                        "accounts"->"sp.phone.ui.fragment.SettingsUserFragment"
                        "blacklist"->"sp.phone.ui.fragment.SettingsBlackListFragment"
                        else->"sp.phone.ui.fragment.FilterKeywordsFragment"
                    }
                    startActivity(Intent(this,LauncherSubActivity::class.java).putExtra("fragment",fragment))
                }
            },::clearCache)
        }}
    }
    private fun clearCache() {
        ThreadUtils.postOnSubThread {
            try {
                Glide.get(applicationContext).clearDiskCache()
                UserManagerImpl.getInstance().clearAvatarUrl()
                getDir("awp",Context.MODE_PRIVATE).deleteRecursively()
                getDir("sogou_webview",Context.MODE_PRIVATE).deleteRecursively()
                runOnUiThread {Glide.get(applicationContext).clearMemory();ToastUtils.success("缓存已清除")}
            } catch(error:Exception) {runOnUiThread {ToastUtils.error("缓存清除失败，请稍后重试")}}
        }
    }
}
