package gov.anzong.androidnga.base.json;

import java.util.List;
import java.util.Map;

/**
 * JSON 解析与序列化入口，用于替换 fastjson 1.1，行为以 docs/fastjson-migration.md 与特征测试为准。
 *
 * <p>空白文档与 {@code null} 解析为 null；解析或转换失败抛出 {@link NgaJsonException} 或其他运行时异常。
 */
public final class NgaJson {

    private NgaJson() {}

    /** 返回 {@link NgaJsonObject}、{@link NgaJsonArray}、字符串、数字、布尔值或 null。 */
    public static Object parse(String text) {
        return JsonParser.parse(text);
    }

    public static NgaJsonObject parseObject(String text) {
        Object value = JsonParser.parse(text);
        if (value == null || value instanceof NgaJsonObject) {
            return (NgaJsonObject) value;
        }
        throw JsonCasts.cannotCast(value, "JSON 对象");
    }

    public static NgaJsonArray parseArray(String text) {
        Object value = JsonParser.parse(text);
        if (value == null || value instanceof NgaJsonArray) {
            return (NgaJsonArray) value;
        }
        throw JsonCasts.cannotCast(value, "JSON 数组");
    }

    /** 宽松匹配键名（忽略大小写、下划线与连字符）绑定实体。 */
    @SuppressWarnings("unchecked")
    public static <T> T parseObject(String text, Class<T> type) {
        NgaJsonObject object = parseObject(text);
        if (object == null || type.isAssignableFrom(NgaJsonObject.class)) {
            return (T) object;
        }
        if (Map.class.isAssignableFrom(type)) {
            throw JsonCasts.cannotCast(object, type.getSimpleName());
        }
        return JsonBeans.bind(object, type, true);
    }

    /** 宽松匹配键名逐项绑定，null 元素保留为 null。 */
    public static <T> List<T> parseArray(String text, Class<T> type) {
        NgaJsonArray array = parseArray(text);
        return array == null ? null : JsonBeans.bindList(array, type);
    }

    /** 仅按属性名精确匹配绑定实体，供接口树形数据转换使用。 */
    public static <T> T toJavaObject(NgaJsonObject object, Class<T> type) {
        return object == null ? null : JsonBeans.bind(object, type, false);
    }

    /** 省略值为 null 的键；实体按属性名字母顺序输出。 */
    public static String toJSONString(Object value) {
        return JsonWriter.write(value);
    }
}
