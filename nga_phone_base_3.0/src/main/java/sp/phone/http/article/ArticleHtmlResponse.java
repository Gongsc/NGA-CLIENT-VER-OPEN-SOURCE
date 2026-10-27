package sp.phone.http.article;

import gov.anzong.androidnga.base.json.NgaJson;
import gov.anzong.androidnga.base.json.NgaJsonArray;
import gov.anzong.androidnga.base.json.NgaJsonObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 将 NGA 网页正文与启动数据归一化为主题数据；不加载网页脚本、广告或导航。 */
final class ArticleHtmlResponse {
    private ArticleHtmlResponse() {}

    static NgaJsonObject decode(String source) {
        Document document = Jsoup.parse(source);
        document.outputSettings().prettyPrint(false);
        Matcher error =
                Pattern.compile("(?s)<!--msginfostart-->(.*?)<!--msginfoend-->").matcher(source);
        if (error.find()) {
            NgaJsonObject result = new NgaJsonObject();
            result.put("error", Jsoup.parse(error.group(1)).text());
            return result;
        }
        if (!source.contains("<!--postlistend-->")
                || !source.matches("(?is).*</body>\\s*</html>\\s*"))
            throw new IllegalArgumentException("网页楼层文档未结束");
        StringBuilder scripts = new StringBuilder();
        for (Element script : document.select("script")) scripts.append(script.data()).append('\n');
        String bootstrap = scripts.toString();
        List<List<String>> defaults =
                ArticleScriptData.calls(bootstrap, "commonui.postArg.setDefault");
        if (defaults.isEmpty() || defaults.get(0).size() < 14)
            throw new IllegalArgumentException("缺少主题元数据");
        List<String> meta = defaults.get(0);
        int fid = ArticleScriptData.number(meta, 0);
        int tid = ArticleScriptData.number(meta, 2);
        int replies = ArticleScriptData.number(meta, 11);
        NgaJsonObject topic = new NgaJsonObject();
        topic.put("fid", fid);
        topic.put("tid", tid);
        topic.put("authorid", ArticleScriptData.number(meta, 3));
        topic.put("type", ArticleScriptData.number(meta, 10));
        topic.put("replies", replies);
        topic.put("subject", text(document, "currentTopicName"));
        NgaJsonObject users = new NgaJsonObject();
        List<List<String>> userCalls =
                ArticleScriptData.calls(bootstrap, "commonui.userInfo.setAll");
        if (!userCalls.isEmpty() && !userCalls.get(0).isEmpty())
            users = NgaJson.parseObject(userCalls.get(0).get(0));
        topic.put(
                "author",
                users.getJSONObject(String.valueOf(topic.getIntValue("authorid"))) == null
                        ? ""
                        : users.getJSONObject(String.valueOf(topic.getIntValue("authorid")))
                                .getString("username"));
        NgaJsonObject rows = new NgaJsonObject();
        for (List<String> args : ArticleScriptData.calls(bootstrap, "commonui.postArg.proc")) {
            if (args.size() < 23) throw new IllegalArgumentException("楼层元数据不完整");
            String key = ArticleScriptData.literal(args, 0);
            if (!key.matches("[0-9]+")) continue;
            int floor = ArticleScriptData.number(args, 0);
            Element container = document.getElementById("postcontainer" + floor);
            Element content = document.getElementById("postcontent" + floor);
            if (container == null
                    || content == null
                    || !container.select("#postcontent" + floor).contains(content))
                throw new IllegalArgumentException("楼层正文不完整");
            NgaJsonObject row = new NgaJsonObject();
            row.put("tid", tid);
            row.put("fid", fid);
            row.put("lou", floor);
            row.put("pid", ArticleScriptData.number(args, 10));
            row.put("type", ArticleScriptData.number(args, 11));
            row.put("authorid", ArticleScriptData.number(args, 13));
            row.put("postdatetimestamp", ArticleScriptData.number(args, 14));
            row.put("postdate", text(document, "postdate" + floor));
            row.put("subject", text(document, "postsubject" + floor));
            row.put("content", clean(content.html()));
            row.put("from_client", ArticleScriptData.literal(args, 19));
            NgaJsonObject attachments = new NgaJsonObject();
            for (Element script : container.select("script")) {
                for (List<String> call :
                        ArticleScriptData.calls(script.data(), "ubbcode.attach.load")) {
                    for (String argument : call) {
                        if (!argument.startsWith("[")) continue;
                        NgaJsonArray values = NgaJson.parseArray(argument);
                        for (int i = 0; i < values.size(); i++) {
                            NgaJsonObject attachment = values.getJSONObject(i);
                            attachment.put("attachurl", attachment.getString("url"));
                            attachment.put("subid", attachments.size());
                            attachments.put(String.valueOf(attachments.size()), attachment);
                        }
                    }
                }
            }
            if (!attachments.isEmpty()) row.put("attachs", attachments);
            String[] scores = ArticleScriptData.literal(args, 15).split(",");
            if (scores.length >= 3) {
                row.put("score", scores[0]);
                row.put("score_2", scores[1]);
                row.put("recommend", scores[2]);
            }
            rows.put(String.valueOf(rows.size()), row);
        }
        // 不假造被删除的楼层，但也不能在元数据缺失时悄悄显示部分正文。
        if (rows.isEmpty() || rows.size() != document.select(".postrow").size())
            throw new IllegalArgumentException("楼层与元数据不匹配");
        Element highlights = document.getElementById("hightlight_for_0");
        if (highlights != null) {
            NgaJsonObject hot = new NgaJsonObject();
            for (List<String> args : ArticleScriptData.calls(bootstrap, "commonui.postArg.proc")) {
                String suffix = ArticleScriptData.literal(args, 0);
                if (args.size() < 23 || suffix.matches("[0-9]+")) continue;
                Element content = highlights.getElementById("postcomment" + suffix);
                if (content == null) continue;
                NgaJsonObject row = new NgaJsonObject();
                int pid = ArticleScriptData.number(args, 10);
                int floor = -1;
                for (Object value : rows.values()) {
                    NgaJsonObject normal = (NgaJsonObject) value;
                    if (normal.getIntValue("pid") == pid) floor = normal.getIntValue("lou");
                }
                row.put("tid", tid);
                row.put("fid", fid);
                row.put("pid", pid);
                row.put("lou", floor);
                row.put("type", ArticleScriptData.number(args, 11));
                row.put("authorid", ArticleScriptData.number(args, 13));
                row.put("postdatetimestamp", ArticleScriptData.number(args, 14));
                row.put("postdate", text(document, "commentInfo" + suffix));
                row.put("subject", text(document, "postcommentsubject" + suffix));
                row.put("content", clean(content.html()));
                row.put("from_client", ArticleScriptData.literal(args, 19));
                String[] scores = ArticleScriptData.literal(args, 15).split(",");
                if (scores.length >= 3) {
                    row.put("score", scores[0]);
                    row.put("score_2", scores[1]);
                }
                hot.put(String.valueOf(hot.size()), row);
            }
            for (Object value : rows.values()) {
                NgaJsonObject root = (NgaJsonObject) value;
                if (root.getIntValue("lou") == 0 && root.getIntValue("pid") == 0)
                    root.put("hotreply", hot);
            }
        }
        for (List<String> alert : ArticleScriptData.calls(bootstrap, "commonui.loadAlertInfo")) {
            if (alert.size() < 2) continue;
            String target = ArticleScriptData.literal(alert, 1).replace("alertc", "");
            for (Object value : rows.values()) {
                NgaJsonObject row = (NgaJsonObject) value;
                if (String.valueOf(row.getIntValue("lou")).equals(target))
                    row.put("alterinfo", clean(ArticleScriptData.literal(alert, 0)));
            }
        }
        NgaJsonObject data = new NgaJsonObject();
        data.put("__T", topic);
        data.put("__R", rows);
        data.put("__U", users);
        int totalRows = replies + 1;
        Matcher pagination =
                Pattern.compile("var\\s+__PAGE\\s*=\\s*(\\{[^}]+\\})").matcher(bootstrap);
        if (pagination.find()) {
            NgaJsonObject page = NgaJson.parseObject(pagination.group(1));
            int pages = page.getIntValue("1");
            int current = page.getIntValue("2");
            if (pages > 0 && current > 0 && current <= pages) {
                // 作者筛选和单条回复页面以网页实际分页为准，而非整个主题的回复数。
                totalRows = Math.min(replies + 1, pages * 20);
                if (pages < (replies + 20) / 20 && current == pages)
                    totalRows = (current - 1) * 20 + rows.size();
            }
        }
        data.put("__ROWS", totalRows);
        data.put("__R__ROWS", rows.size());
        return data;
    }

    private static String text(Document document, String id) {
        Element element = document.getElementById(id);
        return element == null ? "" : element.text();
    }

    private static String clean(String html) {
        return Jsoup.clean(
                html,
                "https://bbs.nga.cn/",
                Safelist.relaxed()
                        .addTags("span")
                        .addAttributes(":all", "class")
                        .preserveRelativeLinks(true),
                new Document.OutputSettings().prettyPrint(false));
    }
}
