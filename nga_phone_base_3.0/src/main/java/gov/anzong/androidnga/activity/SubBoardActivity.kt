package gov.anzong.androidnga.activity

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import gov.anzong.androidnga.arouter.ARouterConstants
import gov.anzong.androidnga.arouter.AppNavigator
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.mvvm.viewmodel.SubBoardViewModel
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.SubBoardScreen
import gov.anzong.androidnga.mvvm.model.param.ParamKey

/** 主题列表菜单中的子版块；extras 为 [ParamKey.KEY_TITLE] 与 "subBoard"。修改过开关时返回 RESULT_OK。 */
class SubBoardActivity : BaseActivity() {

    private val viewModel: SubBoardViewModel by viewModels()

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val title = "${intent.getStringExtra(ParamKey.KEY_TITLE).orEmpty()} - 子版块"
        setContent {
            NgaTheme {
                SubBoardScreen(title, viewModel.rows, onOpen = { row ->
                    AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                        .withString(ParamKey.KEY_TITLE, row.board.name)
                        .withInt(ParamKey.KEY_FID, row.board.fid)
                        .withInt(ParamKey.KEY_STID, row.board.stid)
                        .navigation(this)
                }, onToggle = viewModel::toggle, onBack = ::finish)
                LaunchedEffect(viewModel.message) {
                    viewModel.message?.let { ToastUtils.show(it); viewModel.messageShown() }
                }
                LaunchedEffect(viewModel.changed) { if (viewModel.changed) setResult(Activity.RESULT_OK) }
            }
        }
    }
}
