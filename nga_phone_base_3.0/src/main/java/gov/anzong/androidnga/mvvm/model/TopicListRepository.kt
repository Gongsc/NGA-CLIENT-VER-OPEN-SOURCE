package gov.anzong.androidnga.mvvm.model

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.util.FileUtils
import gov.anzong.androidnga.util.SharedStorage
import gov.anzong.androidnga.network.retrofit.NgaApi
import gov.anzong.androidnga.mvvm.model.convert.ErrorConvertFactory
import gov.anzong.androidnga.mvvm.model.convert.TopicConvertFactory
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.mvvm.model.entity.TopicListInfo
import gov.anzong.androidnga.mvvm.model.param.TopicListParam
import gov.anzong.androidnga.util.ForumUtils
import gov.anzong.androidnga.util.NLog
import gov.anzong.androidnga.util.StringUtils
import java.io.File
import java.io.IOException
import java.net.URLEncoder

/** 主题列表、收藏主题与离线缓存的读写；均为阻塞或挂起调用，由调用方切到后台线程执行。 */
class TopicListRepository(private val api: () -> NgaApi = NgaApi::get) {

    private val convertFactory = TopicConvertFactory()

    /** 请求一页主题列表；24 小时热帖由多页合并，条目不记录所在页码。 */
    suspend fun loadPage(page: Int, param: TopicListParam): TopicListInfo {
        val js = api().get(topicListUrl(ForumUtils.getAvailableDomain(), page, param))
        val entryPage = if (param.twentyfour == 1) 0 else page
        return convertFactory.getTopicListInfo(js, entryPage) ?: throw Exception(ErrorConvertFactory.getErrorMessage(js))
    }

    /** 取消收藏主题；返回服务器是否确认成功。 */
    suspend fun removeFavorite(info: ThreadPageInfo): Boolean {
        val tidArray = if (info.pid != 0) "${info.tid}_${info.pid}" else info.tid.toString()
        val fields = mapOf(
            "__lib" to "topic_favor", "__act" to "topic_favor", "__output" to "8", "action" to "del",
            "page" to info.page.toString(), "tidarray" to tidArray,
        )
        return api().post(fields).contains("操作成功")
    }

    private val context: Context get() = ContextUtils.getContext()

    private val cacheRoot get() = File(context.filesDir, "cache")

    /** 读取离线缓存的主题；无法解析的条目跳过。 */
    fun loadCache(): TopicListInfo {
        val dirs = cacheRoot.listFiles() ?: throw IOException("没有缓存目录")
        val listInfo = TopicListInfo()
        for (dir in dirs) {
            val infoFile = File(dir, dir.name + ".json")
            if (!infoFile.exists()) continue
            val pageInfo = NgaJson.parseObject(FileUtils.readFileToString(infoFile), ThreadPageInfo::class.java)
            if (pageInfo == null) NLog.e("TopicListRepository", "缓存解析失败：" + infoFile.path) else listInfo.addThreadPage(pageInfo)
        }
        return listInfo
    }

    /** 删除一个主题的离线缓存；没有该缓存时返回 false。 */
    fun deleteCache(tid: Int): Boolean {
        val dir = File(cacheRoot, tid.toString())
        if (!dir.isDirectory) return false
        FileUtils.deleteDirectory(dir)
        return true
    }

    /** 把全部离线缓存打包导出到下载目录的 NGA 文件夹。 */
    fun exportCache() {
        val name = "nga_cache_" + System.currentTimeMillis() + ".zip"
        val archive = File(context.cacheDir, name)
        try {
            if (!FileUtils.zipFiles(cacheRoot.absolutePath + "/", archive.absolutePath)) throw IOException("打包失败")
            SharedStorage.publish(context, archive, MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                Environment.DIRECTORY_DOWNLOADS + "/NGA", name, "application/zip")
        } finally {
            archive.delete()
        }
    }

    /** 导入之前导出的缓存压缩包；不是压缩包时抛出 [IllegalArgumentException]。 */
    fun importCache(uri: Uri) {
        val cr = context.contentResolver
        if (cr.getType(uri)?.contains("zip") != true) throw IllegalArgumentException("选择非法文件")
        val destDir = context.filesDir
        val tempZip = File(destDir, "temp.zip")
        try {
            val input = cr.openInputStream(uri) ?: throw IOException("无法读取文件")
            input.use { FileUtils.copyInputStreamToFile(it, tempZip) }
            FileUtils.unzip(tempZip.absolutePath, destDir.absolutePath)
        } finally {
            tempZip.delete()
        }
    }
}

/** 主题列表地址：作者或回复搜索优先，其次合集、版块、关键词与版块组。 */
fun topicListUrl(domain: String, page: Int, param: TopicListParam): String {
    val url = StringBuilder("$domain/thread.php?")
    if (param.authorId != 0) url.append("authorid=").append(param.authorId).append("&")
    if (param.searchPost != 0) url.append("searchpost=").append(param.searchPost).append("&")
    if (param.favor != 0) url.append("favor=").append(param.favor).append("&")
    if (param.content != 0) url.append("content=").append(param.content).append("&")
    val author = param.author
    if (!author.isNullOrEmpty()) {
        if (author.endsWith("&searchpost=1")) {
            url.append("author=").append(URLEncoder.encode(author.removeSuffix("&searchpost=1"), "GBK")).append("&searchpost=1&")
        } else {
            url.append("author=").append(URLEncoder.encode(author, "GBK")).append("&")
        }
    } else {
        if (param.stid != 0) {
            url.append("stid=").append(param.stid).append("&")
        } else if (param.fid != 0) {
            url.append("fid=").append(param.fid).append("&")
        }
        if (!param.key.isNullOrEmpty()) url.append("key=").append(StringUtils.encodeUrl(param.key, "UTF-8")).append("&")
        if (!param.fidGroup.isNullOrEmpty()) url.append("fidgroup=").append(param.fidGroup).append("&")
    }
    url.append("page=").append(page).append("&lite=js&noprefix")
    if (param.recommend == 1) url.append("&recommend=1&order_by=postdatedesc&user=1")
    return url.toString()
}
