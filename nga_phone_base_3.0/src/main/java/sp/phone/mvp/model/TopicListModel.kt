package sp.phone.mvp.model

import com.justwen.androidnga.cloud.CloudServerManager
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.base.util.ThreadUtils
import gov.anzong.androidnga.common.util.FileUtils
import gov.anzong.androidnga.coroutines.AppScope
import gov.anzong.androidnga.coroutines.NgaDispatchers
import gov.anzong.androidnga.coroutines.launchWithCallback
import gov.anzong.androidnga.http.OnHttpCallBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.contract.TopicListContract
import sp.phone.mvp.model.convert.ErrorConvertFactory
import sp.phone.mvp.model.convert.TopicConvertFactory
import sp.phone.mvp.model.entity.ThreadPageInfo
import sp.phone.mvp.model.entity.TopicListInfo
import sp.phone.param.TopicListParam
import sp.phone.util.NLog
import sp.phone.util.StringUtils
import java.io.File
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

/**
 * Created by Justwen on 2017/11/21.
 *
 * 请求均不随页面取消，与原先一致。
 */
open class TopicListModel @JvmOverloads constructor(private val api: () -> NgaApi = NgaApi::get) :
    BaseModel(), TopicListContract.Model {

    private val convertFactory = TopicConvertFactory()

    override fun loadCache(callBack: OnHttpCallBack<TopicListInfo>) {
        AppScope.launchWithCallback({ info: TopicListInfo -> callBack.onSuccess(info) }, { callBack.onError("读取缓存失败！") }) {
            val path = ContextUtils.getContext().filesDir.absolutePath + "/cache/"
            val cacheDirs = File(path).listFiles() ?: throw IOException("没有缓存目录")
            val listInfo = TopicListInfo()
            for (dir in cacheDirs) {
                val infoFile = File(dir, dir.name + ".json")
                if (!infoFile.exists()) continue
                val rawData = FileUtils.readFileToString(infoFile)
                val pageInfo = NgaJson.parseObject(rawData, ThreadPageInfo::class.java)
                if (pageInfo == null) {
                    CloudServerManager.putCrashData(ContextUtils.getContext(), "rawData", rawData)
                } else {
                    listInfo.addThreadPage(pageInfo)
                }
            }
            listInfo
        }
    }

    /** 请求失败只记录日志，与原先一致。 */
    override fun removeTopic(info: ThreadPageInfo, callBack: OnHttpCallBack<String>) {
        var tidArray = info.tid.toString()
        if (info.pid != 0) tidArray = tidArray + "_" + info.pid
        val fields = mapOf(
            "__lib" to "topic_favor", "__act" to "topic_favor", "__output" to "8", "action" to "del",
            "page" to info.page.toString(), "tidarray" to tidArray,
        )
        AppScope.launchWithCallback({ s: String ->
            if (s.contains("操作成功")) callBack.onSuccess("操作成功！") else callBack.onError("操作失败!")
        }, { NLog.e(it.message) }) { api().post(fields) }
    }

    override fun loadTopicList(page: Int, param: TopicListParam, callBack: OnHttpCallBack<TopicListInfo>) {
        val url = getUrl(page, param)
        AppScope.launchWithCallback({ info: TopicListInfo -> callBack.onSuccess(info) },
            { callBack.onError(ErrorConvertFactory.getErrorMessage(it)) }) { fetchPage(url, page) }
    }

    /** 依次请求各页，每页解析后回调一次；任一页失败即停止。 */
    override fun loadTwentyFourList(param: TopicListParam, callBack: OnHttpCallBack<TopicListInfo>, pageCount: Int) {
        val urls = (1..pageCount).map { getUrl(it, param) }
        AppScope.launch(Dispatchers.Main.immediate) {
            try {
                for (url in urls) {
                    val info = withContext(NgaDispatchers.io) { fetchPage(url, 0, log = true) }
                    callBack.onSuccess(info)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                callBack.onError(ErrorConvertFactory.getErrorMessage(e))
            }
        }
    }

    private suspend fun fetchPage(url: String, page: Int, log: Boolean = false): TopicListInfo {
        val js = api().get(url)
        if (log) NLog.d(js)
        return convertFactory.getTopicListInfo(js, page) ?: throw Exception(ErrorConvertFactory.getErrorMessage(js))
    }

    override fun removeCacheTopic(info: ThreadPageInfo, callBack: OnHttpCallBack<String>) {
        ThreadUtils.postOnSubThread {
            val path = ContextUtils.getContext().filesDir.absolutePath + "/cache/"
            val cacheDirs = File(path).listFiles()
            if (cacheDirs == null) {
                callBack.onError(null)
                return@postOnSubThread
            }
            try {
                for (dir in cacheDirs) {
                    if (dir.name == info.tid.toString()) {
                        FileUtils.deleteDirectory(dir)
                        callBack.onSuccess(null)
                        return@postOnSubThread
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            callBack.onError(null)
        }
    }

    private fun getUrl(page: Int, requestInfo: TopicListParam): String {
        val jsonUri = StringBuilder("$availableDomain/thread.php?")
        if (0 != requestInfo.authorId) jsonUri.append("authorid=").append(requestInfo.authorId).append("&")
        if (requestInfo.searchPost != 0) jsonUri.append("searchpost=").append(requestInfo.searchPost).append("&")
        if (requestInfo.favor != 0) jsonUri.append("favor=").append(requestInfo.favor).append("&")
        if (requestInfo.content != 0) jsonUri.append("content=").append(requestInfo.content).append("&")
        if (!StringUtils.isEmpty(requestInfo.author)) {
            try {
                if (requestInfo.author.endsWith("&searchpost=1")) {
                    jsonUri.append("author=").append(URLEncoder.encode(
                        requestInfo.author.substring(0, requestInfo.author.length - 13), "GBK")).append("&searchpost=1&")
                } else {
                    jsonUri.append("author=").append(URLEncoder.encode(requestInfo.author, "GBK")).append("&")
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        } else {
            if (requestInfo.stid != 0) {
                jsonUri.append("stid=").append(requestInfo.stid).append("&")
            } else if (0 != requestInfo.fid) {
                jsonUri.append("fid=").append(requestInfo.fid).append("&")
            }
            if (!StringUtils.isEmpty(requestInfo.key)) {
                jsonUri.append("key=").append(StringUtils.encodeUrl(requestInfo.key, "UTF-8")).append("&")
            }
            if (!StringUtils.isEmpty(requestInfo.fidGroup)) {
                jsonUri.append("fidgroup=").append(requestInfo.fidGroup).append("&")
            }
        }
        jsonUri.append("page=").append(page).append("&lite=js&noprefix")
        if (requestInfo.recommend == 1) jsonUri.append("&recommend=1&order_by=postdatedesc&user=1")
        return jsonUri.toString()
    }
}
