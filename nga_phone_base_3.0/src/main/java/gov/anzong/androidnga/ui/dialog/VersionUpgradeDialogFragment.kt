package gov.anzong.androidnga.ui.dialog

import android.os.Bundle
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.BuildConfig
import gov.anzong.androidnga.NgaClientApp
import gov.anzong.androidnga.settings.VersionUpgradeTips
import gov.anzong.androidnga.ui.compose.NgaDialogLayout

/** 当前版本的更新内容（[VersionUpgradeTips] 中的 TIPS_版本号）；没有内容时直接关闭。 */
class VersionUpgradeDialogFragment : BaseDialogFragment() {

    private val tip: String? by lazy { upgradeTip(BuildConfig.VERSION_CODE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (tip == null) dismissAllowingStateLoss()
    }

    @Composable
    override fun DialogContent() {
        NgaDialogLayout("提示", buttons = { TextButton(onClick = { dismiss() }) { Text("确定") } }) {
            Text(tip.orEmpty(), Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()))
        }
    }

    override fun onDestroy() {
        NgaClientApp.setNewVersion(false)
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        fun upgradeTip(versionCode: Int): String? = try {
            VersionUpgradeTips::class.java.getDeclaredField("TIPS_$versionCode").get(null) as? String
        } catch (e: ReflectiveOperationException) {
            null
        }
    }
}
