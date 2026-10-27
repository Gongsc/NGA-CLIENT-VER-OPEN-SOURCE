package gov.anzong.androidnga.base.json;

import java.util.ArrayList;
import java.util.Collection;

/** JSON 数组。按下标取值的方法沿用 fastjson 的 {@code JSONArray}；与对象不同，元素不会从字符串解析为对象或数组。 */
public class NgaJsonArray extends ArrayList<Object> {

    public NgaJsonArray() {}

    public NgaJsonArray(Collection<?> values) {
        super(values);
    }

    public String getString(int index) {
        return JsonCasts.toStringValue(get(index));
    }

    public Integer getInteger(int index) {
        return JsonCasts.toInteger(get(index));
    }

    public int getIntValue(int index) {
        Integer value = getInteger(index);
        return value == null ? 0 : value;
    }

    public long getLongValue(int index) {
        Long value = JsonCasts.toLong(get(index));
        return value == null ? 0L : value;
    }

    public boolean getBooleanValue(int index) {
        Boolean value = JsonCasts.toBoolean(get(index));
        return value != null && value;
    }

    public NgaJsonObject getJSONObject(int index) {
        Object value = get(index);
        if (value == null || value instanceof NgaJsonObject) {
            return (NgaJsonObject) value;
        }
        throw JsonCasts.cannotCast(value, "JSON 对象");
    }

    public NgaJsonArray getJSONArray(int index) {
        Object value = get(index);
        if (value == null || value instanceof NgaJsonArray) {
            return (NgaJsonArray) value;
        }
        throw JsonCasts.cannotCast(value, "JSON 数组");
    }

    public String toJSONString() {
        return JsonWriter.write(this);
    }

    @Override
    public String toString() {
        return toJSONString();
    }
}
