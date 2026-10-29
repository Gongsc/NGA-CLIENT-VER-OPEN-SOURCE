package gov.anzong.androidnga.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.core.content.IntentCompat
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.Lifecycle
import gov.anzong.androidnga.ui.compose.ArticleCacheScreen
import gov.anzong.androidnga.ui.compose.NgaTheme
import gov.anzong.androidnga.ui.compose.cachedArticlePages
import sp.phone.param.ArticleListParam
import sp.phone.param.ParamKey
import sp.phone.ui.fragment.ArticleListFragment
import java.io.File

/** 离线缓存阅读；extras 为 [ParamKey.KEY_PARAM]（loadCache 的 [ArticleListParam]），每个缓存页为一个分页。 */
class ArticleCacheActivity : BaseActivity() {

    override fun usesComposeContent() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        val request = IntentCompat.getParcelableExtra(intent, ParamKey.KEY_PARAM, ArticleListParam::class.java) ?: run { finish(); return }
        val pages = cachedArticlePages(File(filesDir, "cache/${request.tid}"), request.tid.toString())
        setContent {
            NgaTheme {
                ArticleCacheScreen(request.title.orEmpty(), pages, mConfig.isShowBottomTab, onBack = ::finish) { page, active ->
                    val args = remember(page) {
                        Bundle().apply { putParcelable(ParamKey.KEY_PARAM, (request.clone() as ArticleListParam).also { it.page = page }) }
                    }
                    AndroidFragment<ArticleListFragment>(arguments = args, maxLifecycle = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED)
                }
            }
        }
    }
}
