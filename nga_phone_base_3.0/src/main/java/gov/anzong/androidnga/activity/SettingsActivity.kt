package gov.anzong.androidnga.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import gov.anzong.androidnga.mvvm.viewmodel.SettingsListsViewModel
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.util.FunctionUtils
import com.bumptech.glide.Glide
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.ui.compose.*
import gov.anzong.androidnga.account.UserManagerImpl

class SettingsActivity : BaseActivity() {
    override fun usesComposeContent()=true
    override fun onCreate(savedInstanceState:Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val preferences=getSharedPreferences(PreferenceKey.PERFERENCE,Context.MODE_PRIVATE)
        setContent {NgaTheme {
            // 关键词屏蔽、黑名单与账号管理是设置页内的子页面，返回键回到设置
            var page by rememberSaveable {mutableStateOf<String?>(null)}
            // 列表位置放在子页面切换之外，从子页面返回时停在原处
            val settingsList=rememberLazyListState()
            BackHandler(page!=null) {page=null}
            val back={page=null}
            when(page) {
                "keywords"->KeywordFilterScreen(lists.keywords,lists::addKeyword,lists::toggleKeyword,lists::removeKeyword,back)
                "blacklist"->BlacklistScreen(lists.blacklist,{showProfile(it.name)},{lists.unblock(it.uid)},lists::clearBlacklist,back)
                "accounts"->AccountsScreen(lists.accounts,{FunctionUtils.parseAvatarUrl(it.avatarUrl)},lists::activateAccount,
                    lists::moveAccount,{showProfile(it.name)},lists::removeAccount,{startActivity(Intent(this,LoginActivity::class.java))},back)
                else->SettingsScreen(preferences,{finish()},{lists.reload();page=it},::clearCache,settingsList)
            }
        }}
    }

    private val lists:SettingsListsViewModel by viewModels()

    /** 从登录或资料页返回后刷新账号与黑名单。 */
    override fun onResume() {
        super.onResume()
        lists.reload()
    }

    private fun showProfile(userName:String) {
        startActivity(Intent(this,PhoneConfiguration.getInstance().profileActivityClass)
            .putExtra("mode","username").putExtra("username",userName))
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
