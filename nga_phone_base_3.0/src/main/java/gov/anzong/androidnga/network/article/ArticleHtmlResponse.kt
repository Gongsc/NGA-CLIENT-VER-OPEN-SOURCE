package gov.anzong.androidnga.network.article

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

/** 将 NGA 网页正文与启动数据归一化为主题数据；不加载网页脚本、广告或导航。 */
internal object ArticleHtmlResponse {

    private val MESSAGE = Regex("(?s)<!--msginfostart-->(.*?)<!--msginfoend-->")
    private val DOCUMENT_END = Regex("(?is).*</body>\\s*</html>\\s*")
    private val PAGINATION = Regex("var\\s+__PAGE\\s*=\\s*(\\{[^}]+\\})")
    private val DIGITS = Regex("[0-9]+")

    /** 与 Java 的 String.split 一致，去掉末尾的空串。 */
    private fun scores(text: String): List<String> = text.split(",").dropLastWhile { it.isEmpty() }

    @JvmStatic
    fun decode(source: String): NgaJsonObject {
        val document = Jsoup.parse(source)
        document.outputSettings().prettyPrint(false)
        MESSAGE.find(source)?.let { error ->
            return NgaJsonObject().apply { put("error", Jsoup.parse(error.groupValues[1]).text()) }
        }
        if (!source.contains("<!--postlistend-->") || !source.matches(DOCUMENT_END)) {
            throw IllegalArgumentException("网页楼层文档未结束")
        }
        val bootstrap = buildString {
            for (script in document.select("script")) append(script.data()).append('\n')
        }
        val defaults = ArticleScriptData.calls(bootstrap, "commonui.postArg.setDefault")
        if (defaults.isEmpty() || defaults[0].size < 14) throw IllegalArgumentException("缺少主题元数据")
        val meta = defaults[0]
        val fid = ArticleScriptData.number(meta, 0)
        val tid = ArticleScriptData.number(meta, 2)
        val replies = ArticleScriptData.number(meta, 11)
        val topic = NgaJsonObject()
        topic["fid"] = fid
        topic["tid"] = tid
        topic["authorid"] = ArticleScriptData.number(meta, 3)
        topic["type"] = ArticleScriptData.number(meta, 10)
        topic["replies"] = replies
        topic["subject"] = text(document, "currentTopicName")
        var users = NgaJsonObject()
        val userCalls = ArticleScriptData.calls(bootstrap, "commonui.userInfo.setAll")
        if (userCalls.isNotEmpty() && userCalls[0].isNotEmpty()) {
            users = NgaJson.parseObject(userCalls[0][0])!!
        }
        val author = users.getJSONObject(topic.getIntValue("authorid").toString())
        topic["author"] = if (author == null) "" else author.getString("username")
        val rows = NgaJsonObject()
        val procs = ArticleScriptData.calls(bootstrap, "commonui.postArg.proc")
        for (args in procs) {
            if (args.size < 23) throw IllegalArgumentException("楼层元数据不完整")
            val key = ArticleScriptData.literal(args, 0)
            if (!key.matches(DIGITS)) continue
            val floor = ArticleScriptData.number(args, 0)
            val container = document.getElementById("postcontainer$floor")
            val content = document.getElementById("postcontent$floor")
            if (container == null || content == null || !container.select("#postcontent$floor").contains(content)) {
                throw IllegalArgumentException("楼层正文不完整")
            }
            val row = NgaJsonObject()
            row["tid"] = tid
            row["fid"] = fid
            row["lou"] = floor
            row["pid"] = ArticleScriptData.number(args, 10)
            row["type"] = ArticleScriptData.number(args, 11)
            row["authorid"] = ArticleScriptData.number(args, 13)
            row["postdatetimestamp"] = ArticleScriptData.number(args, 14)
            row["postdate"] = text(document, "postdate$floor")
            row["subject"] = text(document, "postsubject$floor")
            row["content"] = clean(content.html())
            row["from_client"] = ArticleScriptData.literal(args, 19)
            val attachments = NgaJsonObject()
            for (script in container.select("script")) {
                for (call in ArticleScriptData.calls(script.data(), "ubbcode.attach.load")) {
                    for (argument in call) {
                        if (!argument.startsWith("[")) continue
                        val values = NgaJson.parseArray(argument)!!
                        for (i in values.indices) {
                            val attachment = values.getJSONObject(i)!!
                            attachment["attachurl"] = attachment.getString("url")
                            attachment["subid"] = attachments.size
                            attachments[attachments.size.toString()] = attachment
                        }
                    }
                }
            }
            if (attachments.isNotEmpty()) row["attachs"] = attachments
            val scores = scores(ArticleScriptData.literal(args, 15))
            if (scores.size >= 3) {
                row["score"] = scores[0]
                row["score_2"] = scores[1]
                row["recommend"] = scores[2]
            }
            rows[rows.size.toString()] = row
        }
        // 不假造被删除的楼层，但也不能在元数据缺失时悄悄显示部分正文。
        if (rows.isEmpty() || rows.size != document.select(".postrow").size) {
            throw IllegalArgumentException("楼层与元数据不匹配")
        }
        val highlights = document.getElementById("hightlight_for_0")
        if (highlights != null) {
            val hot = NgaJsonObject()
            for (args in procs) {
                val suffix = ArticleScriptData.literal(args, 0)
                if (args.size < 23 || suffix.matches(DIGITS)) continue
                val content = highlights.getElementById("postcomment$suffix") ?: continue
                val pid = ArticleScriptData.number(args, 10)
                var floor = -1
                for (value in rows.values) {
                    val normal = value as NgaJsonObject
                    if (normal.getIntValue("pid") == pid) floor = normal.getIntValue("lou")
                }
                val row = NgaJsonObject()
                row["tid"] = tid
                row["fid"] = fid
                row["pid"] = pid
                row["lou"] = floor
                row["type"] = ArticleScriptData.number(args, 11)
                row["authorid"] = ArticleScriptData.number(args, 13)
                row["postdatetimestamp"] = ArticleScriptData.number(args, 14)
                row["postdate"] = text(document, "commentInfo$suffix")
                row["subject"] = text(document, "postcommentsubject$suffix")
                row["content"] = clean(content.html())
                row["from_client"] = ArticleScriptData.literal(args, 19)
                val scores = scores(ArticleScriptData.literal(args, 15))
                if (scores.size >= 3) {
                    row["score"] = scores[0]
                    row["score_2"] = scores[1]
                }
                hot[hot.size.toString()] = row
            }
            for (value in rows.values) {
                val root = value as NgaJsonObject
                if (root.getIntValue("lou") == 0 && root.getIntValue("pid") == 0) root["hotreply"] = hot
            }
        }
        for (alert in ArticleScriptData.calls(bootstrap, "commonui.loadAlertInfo")) {
            if (alert.size < 2) continue
            val target = ArticleScriptData.literal(alert, 1).replace("alertc", "")
            for (value in rows.values) {
                val row = value as NgaJsonObject
                if (row.getIntValue("lou").toString() == target) row["alterinfo"] = clean(ArticleScriptData.literal(alert, 0))
            }
        }
        val data = NgaJsonObject()
        data["__T"] = topic
        data["__R"] = rows
        data["__U"] = users
        var totalRows = replies + 1
        PAGINATION.find(bootstrap)?.let { pagination ->
            val page = NgaJson.parseObject(pagination.groupValues[1])!!
            val pages = page.getIntValue("1")
            val current = page.getIntValue("2")
            if (pages > 0 && current > 0 && current <= pages) {
                // 作者筛选和单条回复页面以网页实际分页为准，而非整个主题的回复数。
                totalRows = minOf(replies + 1, pages * 20)
                if (pages < (replies + 20) / 20 && current == pages) totalRows = (current - 1) * 20 + rows.size
            }
        }
        data["__ROWS"] = totalRows
        data["__R__ROWS"] = rows.size
        return data
    }

    private fun text(document: Document, id: String): String = document.getElementById(id)?.text() ?: ""

    private fun clean(html: String): String = Jsoup.clean(
        html,
        "https://bbs.nga.cn/",
        Safelist.relaxed().addTags("span").addAttributes(":all", "class").preserveRelativeLinks(true),
        Document.OutputSettings().prettyPrint(false),
    )
}
