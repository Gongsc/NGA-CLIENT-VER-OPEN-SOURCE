package sp.phone.http.article;

import gov.anzong.androidnga.base.json.NgaJson;

import java.util.ArrayList;
import java.util.List;

/** 只读取网页启动脚本中的字面量；不执行 JavaScript。 */
final class ArticleScriptData {
    private ArticleScriptData() {}

    static List<List<String>> calls(String script, String name) {
        List<List<String>> result = new ArrayList<>();
        int offset = 0;
        while ((offset = script.indexOf(name + "(", offset)) >= 0) {
            int start = offset + name.length() + 1;
            int depth = 0;
            char quote = 0;
            boolean escaped = false;
            List<String> args = new ArrayList<>();
            int part = start;
            offset = start;
            for (; offset < script.length(); offset++) {
                char c = script.charAt(offset);
                if (quote != 0) {
                    if (escaped) escaped = false;
                    else if (c == '\\') escaped = true;
                    else if (c == quote) quote = 0;
                    continue;
                }
                if (c == '\'' || c == '"' || c == '`') quote = c;
                else if (c == '(' || c == '[' || c == '{') depth++;
                else if (c == ')' && depth == 0) {
                    args.add(script.substring(part, offset).trim());
                    result.add(args);
                    offset++;
                    break;
                } else if (c == ')' || c == ']' || c == '}') depth--;
                else if (c == ',' && depth == 0) {
                    args.add(script.substring(part, offset).trim());
                    part = offset + 1;
                }
            }
        }
        return result;
    }

    static String literal(List<String> args, int index) {
        if (index >= args.size()) return "";
        String value = args.get(index);
        if ("null".equals(value) || "undefined".equals(value)) return "";
        if (value.startsWith("\"") || value.startsWith("'")) {
            Object decoded = NgaJson.parse(value);
            return decoded == null ? "" : decoded.toString();
        }
        return value;
    }

    static int number(List<String> args, int index) {
        return Integer.parseInt(literal(args, index));
    }
}
