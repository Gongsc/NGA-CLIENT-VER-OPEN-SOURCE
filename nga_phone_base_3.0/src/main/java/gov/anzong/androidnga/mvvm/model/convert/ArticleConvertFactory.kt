package gov.anzong.androidnga.mvvm.model.convert

import gov.anzong.androidnga.Utils
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonArray
import gov.anzong.androidnga.base.json.NgaJsonObject
import gov.anzong.androidnga.core.HtmlConvertFactory
import gov.anzong.androidnga.core.data.AttachmentData
import gov.anzong.androidnga.core.data.CommentData
import gov.anzong.androidnga.core.data.HtmlData
import gov.anzong.androidnga.network.ForumConstants
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.network.article.ArticleResponse
import gov.anzong.androidnga.network.bean.DiceData
import gov.anzong.androidnga.network.bean.ThreadData
import gov.anzong.androidnga.network.bean.ThreadRowInfo
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.ui.theme.ThemeManager
import gov.anzong.androidnga.util.FunctionUtils
import gov.anzong.androidnga.util.HttpUtil
import gov.anzong.androidnga.util.NLog
import gov.anzong.androidnga.util.StringUtils
import java.util.regex.Pattern
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * 归一化后的主题数据（见 [ArticleResponse]）转换为楼层与正文网页。
 * Created by Justwen on 2017/12/3.
 */
object ArticleConvertFactory {

    private const val TAG = "ArticleConvertFactory"

    /** 匿名用户名的姓氏表，末尾的空格对应序号 255。 */
    private const val ANONYMOUS_SURNAMES = AnonymousName.SURNAMES + " "

    private const val DICE = "\\[dice].+?\\[/dice\\]"

    /** 无法解析时返回 null。 */
    @JvmStatic
    fun getArticleInfo(js: String): ThreadData? = try {
        val obj = ArticleResponse.decode(js)
        val data = ThreadData()
        data.rawData = js
        data.threadInfo = buildThreadPageInfo(obj)
        if (data.threadInfo == null) {
            null
        } else {
            val rows = buildThreadRowList(obj)
            data.rowList = rows
            data.hotReplyList = buildHotReplies(obj)
            data.__ROWS = obj.getIntValue("__ROWS")
            data.rowNum = rows.size
            data
        }
    } catch (e: Exception) {
        NLog.e(TAG, "主题响应解析失败")
        e.printStackTrace()
        null
    }

    private fun buildThreadPageInfo(obj: NgaJsonObject): ThreadPageInfo? {
        val subObj = obj["__T"] as NgaJsonObject? ?: return null
        return try {
            NgaJson.toJavaObject(subObj, ThreadPageInfo::class.java)
        } catch (e: RuntimeException) {
            NLog.e(TAG, subObj.toJSONString())
            null
        }
    }

    private fun buildThreadRowList(obj: NgaJsonObject): List<ThreadRowInfo> {
        val subObj = obj["__R"] as NgaJsonObject?
        val rows = obj.getIntValue("__R__ROWS")
        val userInfoMap = obj["__U"] as NgaJsonObject?
        return if (subObj == null) ArrayList() else convertJsObjToList(subObj, rows, userInfoMap)
    }

    /** 主楼（lou 与 pid 均为 0）的 hotreply 中的热点回复，按序号排列并去重。 */
    private fun buildHotReplies(data: NgaJsonObject): List<ThreadRowInfo> {
        val rows = data.getJSONObject("__R") ?: return ArrayList()
        val users = data.getJSONObject("__U")
        for (value in rows.values) {
            if (value !is NgaJsonObject) continue
            if (value.getIntValue("lou") != 0 || value.getIntValue("pid") != 0) continue
            val values: Collection<*> = when (val hot = value["hotreply"]) {
                is NgaJsonObject -> hot.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }.map { hot[it] }
                is NgaJsonArray -> hot
                else -> continue
            }
            val result = ArrayList<ThreadRowInfo>()
            val seen = HashSet<Int>()
            for (reply in values) {
                if (reply !is NgaJsonObject) continue
                val row = NgaJsonObject(reply)
                val pid = row.getIntValue("pid")
                if (pid <= 0 || pid in seen || (!row.containsKey("content") && !row.containsKey("subject"))) continue
                if (!row.containsKey("tid")) row["tid"] = value["tid"]
                if (!row.containsKey("fid")) row["fid"] = value["fid"]
                if (!row.containsKey("lou")) {
                    row["lou"] = -1
                    for (normal in rows.values) {
                        if (normal is NgaJsonObject && normal.getIntValue("pid") == pid) row["lou"] = normal["lou"]
                    }
                }
                val normalized = NgaJsonObject()
                normalized["0"] = row
                try {
                    result.addAll(convertJsObjToList(normalized, 1, users))
                    seen.add(pid)
                } catch (ignored: RuntimeException) {
                    // 可选热点条目异常时保留主题正文与其他有效热点。
                }
            }
            return result
        }
        return ArrayList()
    }

    private fun convertJsObjToList(rowMap: NgaJsonObject, count: Int, userInfoMap: NgaJsonObject?): List<ThreadRowInfo> {
        val rowList = ArrayList<ThreadRowInfo>()
        for (i in 0 until count) {
            val rowObj = rowMap[i.toString()] as? NgaJsonObject ?: continue
            val row = NgaJson.toJavaObject(rowObj, ThreadRowInfo::class.java)!!
            buildRowHotReply(row, rowObj)
            buildRowComment(row, rowObj, userInfoMap)
            buildRowClientInfo(row, rowObj)
            buildRowUserInfo(row, userInfoMap)
            buildRowVote(row, rowObj)
            buildRowContent(row)
            rowList.add(row)
        }
        return rowList
    }

    private fun buildRowContent(row: ThreadRowInfo) {
        if (row.content == null) {
            row.content = row.subject
            row.subject = null
        }
        if (row.fromClient?.startsWith("103 ") == true && !row.content.isNullOrEmpty()) {
            row.content = StringUtils.unescape(row.content)
        }
        val imageUrls = ArrayList<String>()
        val ngaHtml = HtmlConvertFactory.convert(buildHtmlData(row), imageUrls)
        val arg = DiceData()
        arg.seed = 2110032.0
        arg.authorId = row.authorid
        arg.tId = row.tid
        arg.pId = row.pid
        arg.id = "postcontent0"
        arg.txt = ngaHtml
        arg.argsId = arg.id
        imageUrls.forEach(row::addImageUrl)
        row.formattedHtmlData = getRealDice(arg)
    }

    private fun buildHtmlData(row: ThreadRowInfo): HtmlData {
        val config = PhoneConfiguration.getInstance()
        val htmlData = HtmlData(row.content)
        htmlData.alertInfo = row.alterinfo
        htmlData.isDarkMode = ThemeManager.getInstance().isNightMode
        htmlData.isInBackList = row.isInBlackList
        htmlData.textSize = config.topicContentSize
        htmlData.emotionSize = config.emoticonSize
        htmlData.signature = if (config.isShowSignature) row.signature else null
        htmlData.vote = row.vote
        htmlData.subject = row.subject
        htmlData.isShowImage = config.isImageLoadEnabled
        htmlData.ngaHost = Utils.getNGAHost()
        row.attachs?.let { attachs ->
            htmlData.attachmentList = attachs.values.map { attachment ->
                AttachmentData().apply {
                    attachUrl = attachment.attachurl
                    thumb = attachment.thumb
                    attachmentHost = HttpUtil.NGA_ATTACHMENT_HOST
                }
            }
        }
        row.comments?.let { comments ->
            htmlData.commentList = comments.map { value ->
                CommentData().apply {
                    author = value.author
                    content = value.content
                    postTime = value.postdate
                    avatarUrl = FunctionUtils.parseAvatarUrl(value.js_escap_avatar)
                }
            }
        }
        return htmlData
    }

    private fun buildRowVote(row: ThreadRowInfo, rowObj: NgaJsonObject) {
        val vote = rowObj.getString("vote")
        if (!vote.isNullOrEmpty()) {
            row.vote = vote
        }
    }

    // 热门回复
    private fun buildRowHotReply(row: ThreadRowInfo, rowObj: NgaJsonObject) {
        val hotObj = rowObj.getString("17") ?: return
        row.hotReplies = hotObj.split(",").filter { it.isNotEmpty() }.toMutableList()
    }

    // 解析贴条
    private fun buildRowComment(row: ThreadRowInfo, rowObj: NgaJsonObject, userInfoMap: NgaJsonObject?) {
        val commObj = rowObj["comment"] as NgaJsonObject? ?: return
        row.comments = convertJsObjToList(commObj, commObj.size, userInfoMap)
    }

    private fun buildRowClientInfo(row: ThreadRowInfo, rowObj: NgaJsonObject) {
        val client = rowObj.getString("from_client")
        if (client.isNullOrEmpty()) return
        row.fromClient = client
        if (client.trim { it <= ' ' }.isEmpty()) return
        val clientAppCode = if (client.contains(" ")) client.substring(0, client.indexOf(' ')) else client
        row.fromClientModel = when (clientAppCode) {
            "1", "7", "101" -> "ios"
            "103", "9" -> "wp"
            "8", "100" -> "android"
            else -> "unknown"
        }
    }

    private fun buildRowUserInfo(row: ThreadRowInfo, userInfoMap: NgaJsonObject?) {
        if (row.authorid == 0 || userInfoMap == null) {
            return
        }
        val uid = row.authorid
        val userInfo = userInfoMap[uid.toString()] as NgaJsonObject? ?: return
        val groupObj = userInfoMap.getJSONObject("__GROUPS")
        row.isInBlackList = UserManagerImpl.getInstance().checkBlackList(uid.toString())
        val userName = userInfo.getString("username") ?: "UID:$uid"
        if (userName.length == 39 && userName.startsWith("#anony_")) {
            val builder = StringBuilder()
            var i = 6
            for (j in 0 until 6) {
                if (j == 0 || j == 3) {
                    builder.append(AnonymousName.STEMS[userName.substring(i + 1, i + 2).toInt(16)])
                } else {
                    builder.append(ANONYMOUS_SURNAMES[userName.substring(i, i + 2).toInt(16)])
                }
                i += 2
            }
            row.author = builder.toString()
            row.isAnonymous = true
        } else {
            row.author = userName
        }
        row.js_escap_avatar = userInfo.getString("avatar")
        row.yz = userInfo.getString("yz")
        row.muteTime = userInfo.getString("mute_time")
        row.aurvrc = userInfo.getString("rvrc")?.toIntOrNull() ?: 0
        row.signature = userInfo.getString("signature")
        try {
            row.postCount = userInfo.getString("postnum")
            row.reputation = userInfo.getString("rvrc")!!.toFloat() / 10.0f
            row.memberGroup = groupObj!!.getJSONObject(userInfo.getString("memberid")!!)!!.getString("0")
        } catch (e: Exception) {
            // 资料不完整时不显示发帖数、威望与用户组
        }
        val buffs = userInfo.getJSONObject("buffs")
        if (buffs != null && ForumConstants.BUFF_MUTE_IDS.any { buffs.containsKey(it) }) {
            row.isMuted = true
        }
    }

    /** 与网页相同的伪随机数：以作者、主题与楼层为种子。 */
    @JvmStatic
    fun rnd(arg: DiceData): Double {
        if (arg.argsId != null) {
            if (arg.rndSeed == 0.0) {
                val offset = if (arg.tId > 10246184 || arg.pId > 200188932) arg.seedOffset else 0
                arg.rndSeed = (arg.authorId + arg.tId + arg.pId + offset).toDouble()
                if (arg.rndSeed == 0.0) arg.rndSeed = floor(Math.random() * 10000)
            }
            arg.rndSeed = (arg.rndSeed * 9301 + 49297) % 233280
            return arg.rndSeed / 233280.0
        }
        arg.seed = (arg.seed * 9301 + 49297) % 233280
        return arg.seed / 233280.0
    }

    /** 与 Java 的 String.split 一致，去掉末尾的空串。 */
    private fun String.javaSplit(regex: String): List<String> = split(Regex(regex)).dropLastWhile { it.isEmpty() }

    /** 计算掷骰子结果，如 `[dice]2d6+1[/dice]`。 */
    @JvmStatic
    fun getRealDice(arg: DiceData): String {
        var txt = arg.txt.orEmpty()
        val pattern = Pattern.compile(DICE)
        var m = pattern.matcher(txt)
        if (!m.find()) return txt
        do {
            var sum = 0
            val diceStr = StringBuilder()
            val expression = m.group(0)!!.replace("[dice]", "").replace("[/dice]", "")
            val rx = StringBuilder()
            for (str in "+$expression".javaSplit("\\+")) {
                if (str.isEmpty()) continue
                val parts = str.javaSplit("d")
                if (parts.size > 1) {
                    val num = if (parts[0].isNotEmpty()) parts[0].toIntOrNull() ?: 1 else 1
                    val covers = parts[1].toIntOrNull() ?: 0
                    if (num > 10 || covers > 100000) {
                        sum = -1
                        diceStr.append("+OUT OF LIMIT")
                    }
                    for (j in 0 until num) {
                        arg.argsId = "postcomment__510458140"
                        val rand = floor(rnd(arg) * covers) + 1
                        rx.append("+d").append(covers).append("(").append(rand.roundToLong()).append(")")
                        if (sum != -1) sum = (sum + rand).toInt()
                    }
                } else {
                    val covers = parts[0].trim { it <= ' ' }.toIntOrNull() ?: 0
                    sum += covers
                    rx.append("+").append(covers)
                }
            }
            diceStr.append("<p><b>ROLL:").append(expression).append("</b>=").append(rx.substring(1))
                .append("=<b>").append(sum).append("</b></p>")
            txt = pattern.matcher(txt).replaceFirst(diceStr.toString())
            m = pattern.matcher(txt)
        } while (m.find())
        return txt
    }
}
