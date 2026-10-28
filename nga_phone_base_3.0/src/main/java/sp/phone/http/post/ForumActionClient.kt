package sp.phone.http.post

import sp.phone.http.post.NgaWriteClient.ActionResult
import java.net.URLDecoder

/**
 * `nuke.php` 上的头像设置与主题投票，结果按 [NgaWriteClient.postAction] 判断。
 * 方法会发起阻塞请求，需在后台线程调用；失败时返回 [ActionResult]，不抛异常。
 */
class ForumActionClient(private val writer: NgaWriteClient) {

    /** 设置头像为 [iconUrl]，沿用原有的 `func=avatar` 接口。 */
    fun setAvatar(iconUrl: String): ActionResult {
        val url = writer.url("nuke.php").addQueryParameter("__inchst", "UTF8").build()
        val fields = listOf("lite" to "js", "noprefix" to "", "func" to "avatar", "icon" to iconUrl)
        return writer.tryPostAction(url, fields, referer = writer.baseUrl())
    }

    /**
     * 主题投票，参数来自正文中的投票脚本（`__lib=vote&__act=vote|settle&tid=…&voteid=…`）。
     * 参照 SNGA 以表单提交、Referer 指向主题；只接受投票，页面脚本不能借此提交其他操作。
     */
    fun vote(query: String): ActionResult {
        val params = parseVoteQuery(query) ?: return ActionResult(false, "不支持的操作")
        val tid = params.getValue("tid")
        val url = writer.url("nuke.php").addQueryParameter("__inchst", "UTF8").build()
        val fields = listOf(
            "__lib" to "vote",
            "__act" to params.getValue("__act"),
            "tid" to tid,
            "voteid" to params.getValue("voteid"),
            "raw" to "3",
        )
        val referer = writer.url("read.php").addQueryParameter("tid", tid).build().toString()
        return writer.tryPostAction(url, fields, referer)
    }

    companion object {

        @JvmStatic
        fun create(): ForumActionClient = ForumActionClient(NgaWriteClient.create())

        private val VOTE_ACTIONS = setOf("vote", "settle")
        private val DIGITS = Regex("\\d+")
        private val VOTE_IDS = Regex("[\\w,]+")

        /** 校验投票参数；不是投票或参数不合法时返回 null。 */
        internal fun parseVoteQuery(query: String): Map<String, String>? {
            val params = try {
                query.split('&').filter { it.isNotEmpty() }.associate {
                    val (name, value) = (it.split('=', limit = 2) + "").take(2)
                    URLDecoder.decode(name, "UTF-8") to URLDecoder.decode(value, "UTF-8")
                }
            } catch (e: IllegalArgumentException) {
                return null
            }
            if (params["__lib"] != "vote" || params["__act"] !in VOTE_ACTIONS) return null
            if (params["tid"]?.matches(DIGITS) != true) return null
            if (params["voteid"]?.matches(VOTE_IDS) != true) return null
            return params
        }
    }
}
