package sp.phone.http.article;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;

/** 将主题接口的 JSON 与 XML 响应转换为统一的数据结构。 */
public final class ArticleResponse {
    private ArticleResponse() {}

    /** 仅检测明确的未结束文档，不把登录 HTML 或完整但格式不兼容的响应当作截断。 */
    public static boolean isIncomplete(String text) {
        String source = text.replace("\uFEFF", "").trim().replace("/*$js$*/", "");
        if (source.isEmpty()) return true;
        if (source.startsWith("<")) {
            return source.matches("(?s)(?:<\\?xml[^>]*>\\s*)?<root(?:\\s[^>]*)?>.*")
                    && !source.matches("(?s).*</root>\\s*(?:<!--.*?-->\\s*)?");
        }
        if (!source.startsWith("{") && !source.startsWith("[")) return false;
        Deque<Character> stack = new ArrayDeque<>();
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < source.length(); i++) {
            char value = source.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (value == '\\') escaped = true;
                else if (value == '"') quoted = false;
            } else if (value == '"') quoted = true;
            else if (value == '{' || value == '[') stack.push(value);
            else if (value == '}' || value == ']') {
                if (stack.isEmpty() || stack.pop() != (value == '}' ? '{' : '[')) return false;
                // NGA 有时在完整 JSON 后追加非数据内容，不将其参与结构检查。
                if (stack.isEmpty()) return false;
            }
        }
        return quoted || !stack.isEmpty();
    }

    public static JSONObject decode(String text) throws Exception {
        String source = text.replace("\uFEFF", "").trim().replace("/*$js$*/", "");
        if (!source.startsWith("<")) {
            int errorFill = source.indexOf("/*error fill content");
            if (errorFill >= 0) source = source.substring(0, errorFill);
            source =
                    source.replaceAll("\"(content|subject)\":([+]\\d+|0\\d+),", "\"$1\":\"$2\",")
                            .replaceAll("\"author\":(0\\d+),", "\"author\":\"$1\",");
            JSONObject root = JSON.parseObject(source);
            if (root == null) throw new IllegalArgumentException("空主题响应");
            JSONObject data = root.getJSONObject("data");
            return data != null ? data : root;
        }
        if (source.toLowerCase(Locale.ROOT).startsWith("<!doctype html")
                || source.toLowerCase(Locale.ROOT).startsWith("<html"))
            return ArticleHtmlResponse.decode(source);
        String upper = source.toUpperCase(Locale.ROOT);
        if (upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY")) {
            throw new IllegalArgumentException("不支持的 XML 声明");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        builder.setEntityResolver(
                (publicId, systemId) -> {
                    throw new IllegalArgumentException("禁止访问外部 XML 资源");
                });
        Element root =
                builder.parse(new InputSource(new StringReader(source))).getDocumentElement();
        if (!"root".equals(root.getTagName())) throw new IllegalArgumentException("非主题响应");
        JSONObject data = (JSONObject) elementValue(root);
        for (String key : new String[] {"__R", "__U"}) {
            if ("".equals(data.get(key))) data.put(key, new JSONObject(true));
        }
        if (!data.containsKey("__R__ROWS") && data.get("__R") instanceof JSONObject) {
            data.put("__R__ROWS", data.getJSONObject("__R").size());
        }
        JSONObject users = data.getJSONObject("__U");
        if (users != null) {
            JSONObject byId = new JSONObject(true);
            for (String key : users.keySet()) {
                Object user = users.get(key);
                String uid =
                        user instanceof JSONObject ? ((JSONObject) user).getString("uid") : null;
                byId.put(uid != null ? uid : key, user);
            }
            data.put("__U", byId);
        }
        return data;
    }

    private static Object elementValue(Element element) {
        JSONObject values = new JSONObject(true);
        int index = 0;
        boolean hasElements = false;
        for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element)) continue;
            hasElements = true;
            Element child = (Element) node;
            String key = child.getTagName();
            if ("item".equals(key)) key = String.valueOf(index++);
            else if (key.matches("_-?\\d+")) key = key.substring(1);
            Object value = elementValue(child);
            if ("".equals(value)
                    && ("comment".equals(key)
                            || "attachs".equals(key)
                            || "buffs".equals(key)
                            || "__GROUPS".equals(key))) {
                value = new JSONObject(true);
            }
            values.put(key, value);
        }
        return hasElements ? values : element.getTextContent();
    }

    public static String businessError(JSONObject data) {
        Object error = data.get("error");
        if (error == null) error = data.get("__MESSAGE");
        if (error instanceof JSONObject) {
            JSONObject object = (JSONObject) error;
            String message = object.getString("1");
            if (message == null) message = object.getString("0");
            if (message == null) message = object.getString("message");
            return message;
        }
        if (error instanceof JSONArray) {
            JSONArray array = (JSONArray) error;
            return array.isEmpty() ? null : array.getString(array.size() - 1);
        }
        return error instanceof String ? (String) error : null;
    }
}
