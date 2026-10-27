package sp.phone.mvp.model

import android.text.TextUtils
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.base.util.ThreadUtils
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.common.util.FileUtils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.coroutines.scopeOf
import gov.anzong.androidnga.http.OnHttpCallBack
import sp.phone.common.UserManagerImpl
import sp.phone.http.article.ArticlePageLoader
import sp.phone.http.bean.ThreadData
import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.contract.ArticleListContract
import sp.phone.mvp.model.convert.ArticleConvertFactory
import sp.phone.mvp.model.convert.ErrorConvertFactory
import sp.phone.param.ArticleListParam
import java.io.File
import java.io.IOException

/**
 * 加载帖子内容
 * Created by Justwen on 2017/7/10.
 */
open class ArticleListModel : BaseModel(), ArticleListContract.Model {

    fun getUrl(param: ArticleListParam): String {
        var url = availableDomain + "/read.php?" + "page=" + maxOf(1, param.page)
        if (param.tid != 0) url += "&tid=" + param.tid
        if (param.pid != 0) url += "&pid=" + param.pid
        if (param.authorId != 0) url += "&authorid=" + param.authorId
        return url
    }

    override fun loadPage(param: ArticleListParam, callBack: OnHttpCallBack<ThreadData>) {
        loadPage(param, null, callBack)
    }

    /** 随页面销毁取消，取消后不再回调。 */
    override fun loadPage(param: ArticleListParam, header: MutableMap<String, String>?, callBack: OnHttpCallBack<ThreadData>) {
        val url = getUrl(param)
        val requestHeaders = HashMap<String, String>()
        if (header != null) requestHeaders.putAll(header)
        if (!requestHeaders.containsKey("Cookie")) {
            requestHeaders["Cookie"] = UserManagerImpl.getInstance().cookie
        }
        scopeOf(lifecycleOwner).launchWithCallback({ threadData: ThreadData ->
            callBack.onSuccess(threadData)
            UserManagerImpl.getInstance().putAvatarUrl(threadData)
        }, { throwable ->
            callBack.onError(ErrorConvertFactory.getErrorMessage(throwable), throwable)
        }) {
            ArticlePageLoader({ url, headers -> NgaApi.get().getArticleResponse(url, headers) },
                ArticleConvertFactory::getArticleInfo).load(url, requestHeaders)
        }
    }

    override fun cachePage(param: ArticleListParam, rawData: String?) {
        if (TextUtils.isEmpty(param.topicInfo)) {
            ToastUtils.error("缓存失败！")
            return
        }
        ThreadUtils.postOnSubThread {
            try {
                val path = ContextUtils.getContext().filesDir.absolutePath + "/cache/" + param.tid
                FileUtils.write(File(path, param.tid.toString() + ".json"), param.topicInfo)
                FileUtils.write(File(path, param.page.toString() + ".json"), rawData)
                ToastUtils.success("缓存成功！")
            } catch (e: IOException) {
                ToastUtils.error("缓存失败！")
                e.printStackTrace()
            }
        }
    }

    /** 与原先一致不随页面取消。 */
    override fun loadCachePage(param: ArticleListParam, callBack: OnHttpCallBack<ThreadData>) {
        AppScope.launchWithCallback({ threadData: ThreadData -> callBack.onSuccess(threadData) }, { callBack.onError("读取缓存失败！") }) {
            val cachePath = ContextUtils.getContext().filesDir.absolutePath + "/cache/" + param.tid + "/" + param.page + ".json"
            ArticleConvertFactory.getArticleInfo(FileUtils.readFileToString(File(cachePath))) ?: throw IllegalStateException("缓存无法解析")
        }
    }
}
