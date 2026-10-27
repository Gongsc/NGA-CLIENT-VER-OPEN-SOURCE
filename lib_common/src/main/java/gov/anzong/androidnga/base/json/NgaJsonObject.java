package gov.anzong.androidnga.base.json;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON 对象，保持键的插入顺序。取值方法的名称与转换规则沿用 fastjson 的 {@code JSONObject}，便于迁移。
 *
 * <p>{@code getXxxValue} 在键缺失、值为 null 或空字符串时返回默认值；{@code getInteger} 等包装类型方法返回 null。
 */
public class NgaJsonObject extends LinkedHashMap<String, Object> {

    public NgaJsonObject() {}

    public NgaJsonObject(Map<String, ?> values) {
        super(values);
    }

    public String getString(String key) {
        return JsonCasts.toStringValue(get(key));
    }

    public Integer getInteger(String key) {
        return JsonCasts.toInteger(get(key));
    }

    public int getIntValue(String key) {
        Integer value = getInteger(key);
        return value == null ? 0 : value;
    }

    public Long getLong(String key) {
        return JsonCasts.toLong(get(key));
    }

    public long getLongValue(String key) {
        Long value = getLong(key);
        return value == null ? 0L : value;
    }

    public Boolean getBoolean(String key) {
        return JsonCasts.toBoolean(get(key));
    }

    public boolean getBooleanValue(String key) {
        Boolean value = getBoolean(key);
        return value != null && value;
    }

    public double getDoubleValue(String key) {
        Double value = JsonCasts.toDouble(get(key));
        return value == null ? 0D : value;
    }

    public float getFloatValue(String key) {
        Float value = JsonCasts.toFloat(get(key));
        return value == null ? 0F : value;
    }

    /** 值为 JSON 文本的字符串会被解析；其他非对象值抛出 {@link NgaJsonException}。 */
    public NgaJsonObject getJSONObject(String key) {
        return JsonCasts.toJsonObject(get(key));
    }

    public NgaJsonArray getJSONArray(String key) {
        return JsonCasts.toJsonArray(get(key));
    }

    /** 精确匹配属性名绑定实体，见 {@link NgaJson#toJavaObject}。 */
    public <T> T toJavaObject(Class<T> type) {
        return NgaJson.toJavaObject(this, type);
    }

    public String toJSONString() {
        return JsonWriter.write(this);
    }

    @Override
    public String toString() {
        return toJSONString();
    }
}
