package sp.phone.http.post

import sp.phone.http.post.NgaWriteClient.ActionResult
import sp.phone.mvp.model.entity.SubBoard
import java.io.IOException
import java.net.URLDecoder

/**
 * `nuke.php` 上的头像设置、主题投票与子版块显示开关，结果按 [NgaWriteClient.postAction] 判断。
 * 方法会发起阻塞请求，需在后台线程调用；头像与投票失败时返回 [ActionResult]，不抛异常。
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

    /**
     * 设置子版块 [subBoard] 的主题是否在父版块中显示，参数沿用原 `user_option` 接口：
     * `type=1` 的子版块以 tid 标识且 add/del 含义相反。失败或结果无法确认时抛出 [NgaWriteClient.PostException]。
     */
    @Throws(IOException::class)
    fun setSubBoardVisible(subBoard: SubBoard, visible: Boolean): String {
        val url = writer.url("nuke.php").addQueryParameter("__inchst", "UTF8").build()
        val fields = subBoardFields(subBoard, visible)
        val referer = writer.url("thread.php").addQueryParameter("fid", subBoard.parentFidStr.orEmpty()).build().toString()
        writer.postAction(url, fields, referer)
        return if (visible) "已取消屏蔽，刷新后生效" else "已屏蔽子版块，刷新后生效"
    }

    companion object {

        @JvmStatic
        fun create(): ForumActionClient = ForumActionClient(NgaWriteClient.create())

        private val VOTE_ACTIONS = setOf("vote", "settle")
        private val DIGITS = Regex("\\d+")
        private val VOTE_IDS = Regex("[\\w,]+")

        internal fun subBoardFields(subBoard: SubBoard, visible: Boolean): List<Pair<String, String>> {
            val type = subBoard.type
            // 论坛对 type=1 的子版块（如“优惠信息 购物指南”）add/del 的含义是反的。
            val action = if ((type == 1) == visible) "del" else "add"
            val id = if (type == 1) subBoard.tidStr.orEmpty() else subBoard.fid.toString()
            return listOf(
                "__lib" to "user_option",
                "__act" to "set",
                "raw" to "3",
                "type" to type.toString(),
                "__output" to "8",
                "fid" to subBoard.parentFidStr.orEmpty(),
                action to id,
            )
        }

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
