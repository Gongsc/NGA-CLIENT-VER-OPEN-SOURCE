package sp.phone.http.post

import gov.anzong.androidnga.base.json.NgaJsonObject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import sp.phone.http.post.NgaWriteClient.ActionResult
import sp.phone.http.post.NgaWriteClient.PostException
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
     * 上传图片并设为 [uid] 的头像，流程与网页“设置头像”（common_res/js_imageEdit.js 的 setAvatar2）一致：
     * 1. `__lib=set_avatar&__act=get&edit=1` 取得上传校验码与上传地址；
     * 2. 向上传地址提交 `func=upload&avatar=1`，图片为 `n{uid}_0` 字段中的 data URL（替换全部头像）；
     * 3. `__lib=set_avatar&__act=set` 把上传结果中的 `url` 设为头像。
     *
     * 前两步失败时头像未更改；第 3 步按 [NgaWriteClient.postAction] 判断，结果不明时提示刷新确认。
     * 成功时返回论坛的提示文字，失败时抛出 [PostException]。
     */
    @Throws(IOException::class)
    fun uploadAvatar(uid: String, image: String): String {
        val referer = writer.url("nuke.php").addQueryParameter("func", "ucp").addQueryParameter("uid", uid).build().toString()
        val nuke = writer.url("nuke.php").addQueryParameter("__inchst", "UTF8").build()
        val target = try {
            writer.execute(nuke, listOf("__lib" to "set_avatar", "__act" to "get", "uid" to uid, "edit" to "1", "raw" to "3"), referer)
        } catch (e: PostException) {
            throw e
        } catch (e: IOException) {
            throw PostException("网络错误，头像未更改，请稍后重试")
        }.let { parseAvatarTarget(it.text, writer.baseUrl()) }
        val uploaded = try {
            writer.execute(target.url, listOf("func" to "upload", "avatar" to "1", "uid" to uid, "lite" to "js",
                "checksum" to target.checksum, "n${uid}_0" to image), referer)
        } catch (e: PostException) {
            throw PostException("${e.message}，头像未更改", e.requiresLogin)
        } catch (e: IOException) {
            throw PostException("上传失败，头像未更改，请稍后重试")
        }.let { parseAvatarUpload(it.text) }
        return writer.postAction(nuke, listOf("__lib" to "set_avatar", "__act" to "set", "uid" to uid, "avatar" to uploaded,
            "disable" to "", "raw" to "3"), referer)
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

        /** 头像上传地址只接受论坛自己的域名，凭据不会发往其他站点。 */
        private val FORUM_DOMAINS = listOf("nga.cn", "ngacn.cc", "ngabbs.com", "nga.178.com")

        class AvatarUploadTarget(val checksum: String, val url: HttpUrl)

        /** 解析 `set_avatar` 的 `get` 结果：`data[1]` 为校验码，`data[2]` 为上传地址（去掉其中的 checksum 参数）。 */
        internal fun parseAvatarTarget(text: String, base: String): AvatarUploadTarget {
            val root = NgaWriteClient.parseJson(text) as? NgaJsonObject ?: throw PostException("无法取得头像上传地址，头像未更改")
            errorText(root)?.let { throw PostException(it, requiresLogin = it.contains("登录")) }
            val data = root.getJSONObject("data") ?: throw PostException("无法取得头像上传地址，头像未更改")
            val checksum = data.getString("1")?.takeIf { it.isNotBlank() } ?: throw PostException("无法取得头像上传校验码，头像未更改")
            val address = data.getString("2")?.replace(Regex("checksum=[a-z0-9]+&?"), "")?.trim()
                ?.takeIf { it.isNotEmpty() } ?: throw PostException("无法取得头像上传地址，头像未更改")
            val absolute = when {
                address.startsWith("//") -> "https:$address"
                address.startsWith("/") -> base.trimEnd('/') + address
                address.startsWith("http://") -> "https://" + address.removePrefix("http://")
                else -> address
            }
            val url = absolute.toHttpUrlOrNull()
            if (url == null || !url.isHttps || FORUM_DOMAINS.none { url.host == it || url.host.endsWith(".$it") }) {
                throw PostException("头像上传地址不是论坛域名，已取消上传")
            }
            return AvatarUploadTarget(checksum, url)
        }

        /** 解析上传结果，返回 `data[0].url`（或 `data.url`）；论坛返回错误或无法识别时抛出 [PostException]。 */
        internal fun parseAvatarUpload(text: String): String {
            val root = NgaWriteClient.parseJson(text) as? NgaJsonObject ?: throw PostException("无法识别上传结果，头像未更改")
            errorText(root)?.let { error ->
                val code = root.getString("error_code")?.takeIf { it.isNotBlank() && it != "0" }
                throw PostException(listOfNotNull(code, error).joinToString(" ") + "，头像未更改", requiresLogin = error.contains("登录"))
            }
            val data = root["data"]
            val entry = (data as? NgaJsonObject)?.let { it.getJSONObject("0") ?: it }
            return entry?.getString("url")?.takeIf { it.isNotBlank() } ?: throw PostException("无法识别上传结果，头像未更改")
        }

        private fun errorText(root: NgaJsonObject): String? = when (val error = root["error"]) {
            null, false, "" -> null
            is NgaJsonObject -> error.values.joinToString(" ") { it.toString() }.trim().takeIf { it.isNotEmpty() }
            is Iterable<*> -> error.joinToString(" ") { it.toString() }.trim().takeIf { it.isNotEmpty() }
            else -> error.toString().takeIf { it.isNotBlank() && it != "0" && it != "false" }
        }
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
