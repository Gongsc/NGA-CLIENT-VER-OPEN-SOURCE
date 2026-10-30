package gov.anzong.androidnga.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import gov.anzong.androidnga.arouter.TopicNavigation
import gov.anzong.androidnga.ui.compose.HomeHistoryScreen
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.SettingsSubPage
import gov.anzong.androidnga.mvvm.model.TopicHistoryManager
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo

/** 主题列表菜单中的浏览历史；与首页“历史”使用同一个列表。 */
class TopicHistoryActivity : BaseActivity() {

    private var history by mutableStateOf(emptyList<ThreadPageInfo>(), neverEqualPolicy())

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val manager = TopicHistoryManager.getInstance()
        setContent {
            NgaTheme {
                SettingsSubPage("浏览历史", ::finish) { padding ->
                    Box(Modifier.padding(padding)) {
                        HomeHistoryScreen(history, { TopicNavigation.openHistory(this@TopicHistoryActivity, it) },
                            onRemove = { manager.removeTopicHistory(it); refresh() },
                            onClear = { manager.removeAllTopicHistory(); refresh() },
                            onControlsExpandedChange = {})
                    }
                }
            }
        }
    }

    /** 阅读主题会更新历史，返回时重新读取。 */
    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        history = TopicHistoryManager.getInstance().topicHistoryList.toList()
    }
}
