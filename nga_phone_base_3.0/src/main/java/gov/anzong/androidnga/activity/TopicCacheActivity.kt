package gov.anzong.androidnga.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.mvvm.viewmodel.TopicCacheViewModel
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.TopicCacheScreen
import gov.anzong.androidnga.ui.compose.observedValue
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.mvvm.model.param.ArticleListParam
import gov.anzong.androidnga.mvvm.model.param.ParamKey
import gov.anzong.androidnga.util.StringUtils

/** 离线缓存的主题列表：点击阅读缓存，确认后删除，菜单中导入或导出缓存。 */
class TopicCacheActivity : BaseActivity() {

    private val viewModel: TopicCacheViewModel by viewModels()
    private var topics by mutableStateOf(emptyList<ThreadPageInfo>())

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importFile(uri)
    }

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        viewModel.topics.observe(this) { topics = it.threadPageList.orEmpty().toList() }
        viewModel.removedTopic.observe(this) { removed -> topics = topics.filterNot { it.tid == removed.tid } }
        viewModel.start()
        setContent {
            NgaTheme {
                val loading = viewModel.loading.observedValue() == true
                val notice = viewModel.notice.observedValue()
                LaunchedEffect(notice) {
                    notice ?: return@LaunchedEffect
                    if (notice.success) ToastUtils.success(notice.text) else ToastUtils.error(notice.text)
                    viewModel.noticeShown()
                }
                TopicCacheScreen(topics, loading, onOpen = ::open, onDelete = viewModel::delete, onImport = ::chooseImportFile,
                    onExport = viewModel::exportFile, onBack = ::finish)
            }
        }
    }

    private fun chooseImportFile() {
        try {
            importLauncher.launch(arrayOf("application/zip"))
        } catch (error: ActivityNotFoundException) {
            ToastUtils.warn("系统不支持导入")
        }
    }

    private fun open(topic: ThreadPageInfo) {
        val param = ArticleListParam().apply {
            tid = topic.tid
            loadCache = true
            title = StringUtils.unEscapeHtml(topic.subject)
        }
        startActivity(Intent(this, ArticleCacheActivity::class.java).putExtra(ParamKey.KEY_PARAM, param))
    }
}
