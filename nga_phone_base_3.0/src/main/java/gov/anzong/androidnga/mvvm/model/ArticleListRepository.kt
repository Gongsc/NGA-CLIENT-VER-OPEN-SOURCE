package gov.anzong.androidnga.mvvm.model

import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.util.FileUtils
import sp.phone.common.UserManagerImpl
import sp.phone.http.article.ArticlePageLoader
import sp.phone.http.bean.ThreadData
import sp.phone.http.bean.ThreadRowInfo
import sp.phone.http.retrofit.NgaApi
import sp.phone.mvp.model.convert.ArticleConvertFactory
import sp.phone.param.ArticleListParam
import sp.phone.util.ForumUtils
import sp.phone.util.FunctionUtils
import sp.phone.util.StringUtils
import java.io.File

/** 帖子内容的网络请求与离线缓存；均为阻塞或挂起调用，由调用方切到后台线程执行。 */
object ArticleListRepository {

    /** 请求一页帖子内容（依次尝试各数据源，见 [ArticlePageLoader]），并记录楼层作者的头像。 */
    suspend fun loadPage(param: ArticleListParam): ThreadData {
        val headers = mapOf("Cookie" to UserManagerImpl.getInstance().cookie.orEmpty())
        val data = ArticlePageLoader({ url, h -> NgaApi.get().getArticleResponse(url, h) }, ArticleConvertFactory::getArticleInfo)
            .load(articleUrl(ForumUtils.getAvailableDomain(), param), headers)
        UserManagerImpl.getInstance().putAvatarUrl(data)
        return data
    }

    private fun cacheDir(tid: Int) = File(ContextUtils.getContext().filesDir, "cache/$tid")

    fun loadCache(param: ArticleListParam): ThreadData =
        ArticleConvertFactory.getArticleInfo(FileUtils.readFileToString(File(cacheDir(param.tid), "${param.page}.json")))
            ?: throw IllegalStateException("缓存无法解析")

    /** 缓存一页内容及主题信息，供离线阅读。 */
    fun saveCache(param: ArticleListParam, rawData: String?) {
        val dir = cacheDir(param.tid)
        FileUtils.write(File(dir, "${param.tid}.json"), param.topicInfo)
        FileUtils.write(File(dir, "${param.page}.json"), rawData)
    }
}

fun articleUrl(domain: String, param: ArticleListParam): String {
    var url = "$domain/read.php?page=${maxOf(1, param.page)}"
    if (param.tid != 0) url += "&tid=" + param.tid
    if (param.pid != 0) url += "&pid=" + param.pid
    if (param.authorId != 0) url += "&authorid=" + param.authorId
    return url
}

/**
 * 回复中引用的楼层：去掉其中原有的引用与“Reply to”标记，注明楼层链接、作者（匿名时为楼层号）与发帖时间。
 * [cleanContent] 整理正文（默认处理不完整的标签并反转义 HTML）。
 */
@JvmOverloads
fun quoteBlock(param: ArticleListParam, row: ThreadRowInfo,
               cleanContent: (String) -> String = { StringUtils.unEscapeHtml(FunctionUtils.checkContent(it)) }): String {
    val content = cleanContent(row.content.orEmpty()
        .replace(Regex("\\[quote\\]([\\s\\S])*\\[/quote\\]"), "")
        .replace(Regex("\\[b\\]Reply to \\[pid=\\d+,\\d+,\\d+\\]Reply\\[/pid\\] Post by .+?\\[/b\\]"), ""))
    val tid = if (param.tid != 0) param.tid else row.tid
    val author = if (row.getISANONYMOUS()) {
        "[uid=-1]${row.author}[/uid][color=gray](${row.lou}楼)[/color]"
    } else {
        "[uid=${row.authorid}]${row.author}[/uid]"
    }
    return "[quote][pid=${row.pid},$tid,${param.page}]Reply[/pid] [b]Post by $author (${row.postdate}):[/b]\n$content[/quote]\n"
}
