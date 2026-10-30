package gov.anzong.androidnga.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import gov.anzong.androidnga.R
import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.core.data.HtmlData
import gov.anzong.androidnga.core.decode.ForumDecoder
import gov.anzong.androidnga.mvvm.viewmodel.ProfileViewModel
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.ProfileAction
import gov.anzong.androidnga.ui.compose.ProfileScreen
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.network.bean.ProfileData
import gov.anzong.androidnga.mvvm.model.param.ParamKey
import gov.anzong.androidnga.util.FunctionUtils

/** 用户资料；extras 为 "uid" 或 "username"（“UID123” 形式按 uid 查询）。 */
class ProfileActivity : BaseActivity() {

    private val viewModel: ProfileViewModel by viewModels()
    private var blocked by mutableStateOf(false)

    /** 签名修改成功后更新签名显示。 */
    private val signLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) result.data?.let { viewModel.updateSignature(it.getStringExtra("sign")) }
    }

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        viewModel.start()
        setContent {
            NgaTheme {
                val profile = viewModel.profile
                val signature = viewModel.signature
                val signatureHtml = remember(signature) { signature?.takeIf { it.isNotBlank() }?.let(::signatureHtml) }
                val currentUser = profile != null && profile.uid == UserManagerImpl.getInstance().userId
                LaunchedEffect(profile?.uid) { if (profile != null) blocked = UserManagerImpl.getInstance().checkBlackList(profile.uid) }
                ProfileScreen(profile, signatureHtml, viewModel.loading, viewModel.error, currentUser, blocked,
                    onAction = { onAction(it, profile) }, onEditSignature = { editSignature(profile) },
                    onOpenForum = { forum ->
                        val fid = forum.fid?.toIntOrNull() ?: return@ProfileScreen
                        AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                            .withInt(ParamKey.KEY_FID, fid).withString(ParamKey.KEY_TITLE, forum.forumName).navigation(this)
                    }, onRefresh = viewModel::refresh, onBack = ::finish)
            }
        }
    }

    private fun profileUrl() = Utils.getNGAHost() + "nuke.php?func=ucp&" + viewModel.query.orEmpty()

    private fun onAction(action: ProfileAction, profile: ProfileData?) {
        when (action) {
            ProfileAction.CopyUrl -> FunctionUtils.copyToClipboard(this, profileUrl())
            ProfileAction.OpenInBrowser -> FunctionUtils.openUrlByDefaultBrowser(this, profileUrl())
            else -> profile ?: return
        }
        profile ?: return
        when (action) {
            ProfileAction.SearchTopics, ProfileAction.SearchReplies -> {
                val uid = profile.uid?.toIntOrNull() ?: return
                val intent = Intent(this, PhoneConfiguration.getInstance().topicActivityClass)
                    .putExtra(ParamKey.KEY_AUTHOR_ID, uid).putExtra(ParamKey.KEY_AUTHOR, profile.userName)
                if (action == ProfileAction.SearchReplies) intent.putExtra(ParamKey.KEY_SEARCH_POST, 1)
                startActivity(intent)
            }
            ProfileAction.SendMessage -> AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_MESSAGE_POST)
                .withString("to", profile.userName).withString(ParamKey.KEY_ACTION, "new").navigation(this)
            ProfileAction.ModifyAvatar -> startActivity(Intent(this, AvatarPostActivity::class.java))
            ProfileAction.ToggleBlock -> {
                val users = UserManagerImpl.getInstance()
                if (users.checkBlackList(profile.uid)) {
                    users.removeFromBlackList(profile.uid)
                    ToastUtils.success(R.string.remove_from_blacklist_success)
                } else {
                    users.addToBlackList(profile.userName, profile.uid)
                    ToastUtils.success(R.string.add_to_blacklist_success)
                }
                blocked = users.checkBlackList(profile.uid)
            }
            else -> Unit
        }
    }

    private fun editSignature(profile: ProfileData?) {
        profile ?: return
        signLauncher.launch(Intent(this, PhoneConfiguration.getInstance().signPostActivityClass).putExtra("prefix", viewModel.signature))
    }

    private fun signatureHtml(sign: String): String {
        val body = ForumDecoder.decode(sign, HtmlData.create(sign, Utils.getNGAHost()))
        return "<html><head><meta charset=\"utf-8\"></head><body>$body" +
            "<script type=\"text/javascript\" src=\"file:///android_asset/html/script.js\"></script></body></html>"
    }
}
