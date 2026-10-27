package gov.anzong.androidnga.base.json;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

/**
 * JSON 序列化，输出格式与 fastjson 1.1 一致。
 *
 * <p>省略值为 null 的键；不转义中文与 {@code /}；控制字符、DEL 与 U+2028 写作大写的 {@code \}uXXXX；
 * 整数值的浮点数去掉末尾 ".0"；实体按属性名字母顺序输出。
 */
final class JsonWriter {

    private JsonWriter() {}

    static String write(Object value) {
        StringBuilder builder = new StringBuilder();
        append(builder, value);
        return builder.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof CharSequence || value instanceof Character) {
            appendString(out, value.toString());
        } else if (value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Double || value instanceof Float) {
            appendFloating(out, ((Number) value).doubleValue(), value.toString());
        } else if (value instanceof Number) {
            out.append(value);
        } else if (value instanceof Map) {
            appendMap(out, (Map<?, ?>) value);
        } else if (value instanceof Collection) {
            appendArray(out, ((Collection<?>) value).toArray());
        } else if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            Object[] items = new Object[length];
            for (int i = 0; i < length; i++) {
                items[i] = Array.get(value, i);
            }
            appendArray(out, items);
        } else if (value instanceof Enum) {
            appendString(out, ((Enum<?>) value).name());
        } else {
            appendMap(out, JsonBeans.toMap(value));
        }
    }

    private static void appendFloating(StringBuilder out, double number, String text) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            out.append("null");
        } else if (text.endsWith(".0")) {
            out.append(text, 0, text.length() - 2);
        } else {
            out.append(text);
        }
    }

    private static void appendMap(StringBuilder out, Map<?, ?> map) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                out.append(',');
            }
            first = false;
            appendString(out, String.valueOf(entry.getKey()));
            out.append(':');
            append(out, entry.getValue());
        }
        out.append('}');
    }

    private static void appendArray(StringBuilder out, Object[] items) {
        out.append('[');
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                out.append(',');
            }
            append(out, items[i]);
        }
        out.append(']');
    }

    private static void appendString(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20 || c == 0x7F || c == ' ') {
                        out.append(String.format(Locale.ROOT, "\\u%04X", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        out.append('"');
    }
}
