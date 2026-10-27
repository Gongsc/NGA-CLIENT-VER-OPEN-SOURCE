package gov.anzong.androidnga.base.json;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 取值方法与 {@link NgaJson#toJavaObject} 共用的类型转换，规则与 fastjson 1.1 的 TypeUtils 对齐。
 *
 * <p>空字符串与字符串 "null" 视为 null；数字转整数时截断并允许溢出；布尔值只认 1/0 与 true/false。
 */
final class JsonCasts {

    private JsonCasts() {}

    private static boolean isNullText(Object value) {
        return value instanceof String
                && (((String) value).isEmpty() || "null".equalsIgnoreCase((String) value));
    }

    static String toStringValue(Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        }
        if (value instanceof Map || value instanceof Collection) {
            return JsonWriter.write(value);
        }
        return value.toString();
    }

    static Integer toInteger(Object value) {
        if (value == null || isNullText(value)) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw cannotCast(value, "int");
    }

    static Long toLong(Object value) {
        if (value == null || isNullText(value)) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new NgaJsonException("无法转换为 long：" + value, e);
            }
        }
        throw cannotCast(value, "long");
    }

    static Double toDouble(Object value) {
        if (value == null || isNullText(value)) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw cannotCast(value, "double");
    }

    static Float toFloat(Object value) {
        Double result = toDouble(value);
        return result == null ? null : result.floatValue();
    }

    static Boolean toBoolean(Object value) {
        if (value == null || isNullText(value)) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).intValueExact() == 1;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        if (value instanceof String) {
            String text = (String) value;
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return false;
            }
        }
        throw cannotCast(value, "boolean");
    }

    /** 字符串值按 JSON 解析；数字等其他类型无法转换。 */
    @SuppressWarnings("unchecked")
    static NgaJsonObject toJsonObject(Object value) {
        if (value == null || value instanceof NgaJsonObject) {
            return (NgaJsonObject) value;
        }
        if (value instanceof Map) {
            return new NgaJsonObject((Map<String, Object>) value);
        }
        if (value instanceof String) {
            Object parsed = JsonParser.parse((String) value);
            if (parsed == null || parsed instanceof NgaJsonObject) {
                return (NgaJsonObject) parsed;
            }
        }
        throw cannotCast(value, "JSON 对象");
    }

    @SuppressWarnings("unchecked")
    static NgaJsonArray toJsonArray(Object value) {
        if (value == null || value instanceof NgaJsonArray) {
            return (NgaJsonArray) value;
        }
        if (value instanceof List) {
            return new NgaJsonArray((List<Object>) value);
        }
        if (value instanceof String) {
            Object parsed = JsonParser.parse((String) value);
            if (parsed == null || parsed instanceof NgaJsonArray) {
                return (NgaJsonArray) parsed;
            }
        }
        throw cannotCast(value, "JSON 数组");
    }

    static NgaJsonException cannotCast(Object value, String target) {
        return new NgaJsonException(
                "无法将 " + value.getClass().getSimpleName() + " 转换为 " + target + "：" + value);
    }
}
