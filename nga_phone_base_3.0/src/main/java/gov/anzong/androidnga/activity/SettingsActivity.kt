package gov.anzong.androidnga.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import com.bumptech.glide.Glide
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
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
            SettingsScreen(preferences,{finish()}, {destination->
                val fragment=when(destination) {
                    "accounts"->"sp.phone.ui.fragment.SettingsUserFragment"
                    "blacklist"->"sp.phone.ui.fragment.SettingsBlackListFragment"
                    else->"sp.phone.ui.fragment.FilterKeywordsFragment"
                }
                startActivity(Intent(this,LauncherSubActivity::class.java).putExtra("fragment",fragment))
            },::clearCache)
        }}
    }
    /** 清除图片与网页缓存；离开设置页也会继续完成。 */
    private fun clearCache() {
        val app = applicationContext
        AppScope.launch {
            try {
                withContext(NgaDispatchers.io) {
                    Glide.get(app).clearDiskCache()
                    UserManagerImpl.getInstance().clearAvatarUrl()
                    app.getDir("awp",Context.MODE_PRIVATE).deleteRecursively()
                    app.getDir("sogou_webview",Context.MODE_PRIVATE).deleteRecursively()
                }
                Glide.get(app).clearMemory()
                ToastUtils.success("缓存已清除")
            } catch(error:CancellationException) {
                throw error
            } catch(error:Exception) {
                ToastUtils.error("缓存清除失败，请稍后重试")
            }
        }
    }
}
