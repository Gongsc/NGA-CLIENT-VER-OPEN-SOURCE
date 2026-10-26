package gov.anzong.androidnga.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import gov.anzong.androidnga.ui.compose.*
import sp.phone.common.PhoneConfiguration
import sp.phone.mvp.model.ForumsListModel

class ForumListActivity : BaseActivity() {
    override fun usesComposeContent() = true

    private lateinit var model: ForumDirectoryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[ForumDirectoryViewModel::class.java]
        model.load()
        setContent {
            NgaTheme {
                ForumDirectoryScreen(model.state, onBack = { finish() }, onRetry = { model.load() }) { forum ->
                    startActivity(Intent(this, PhoneConfiguration.getInstance().topicActivityClass).apply {
                        putExtra("tab", "1")
                        putExtra("fid", forum.fid)
                        putExtra("stid", forum.stid)
                        putExtra("board_name", forum.name)
                    })
                }
            }
        }
    }

    // Retained for the legacy task's callback while other callers migrate.
    fun notifyResult(result: ForumsListModel) { model.accept(result) }
}
