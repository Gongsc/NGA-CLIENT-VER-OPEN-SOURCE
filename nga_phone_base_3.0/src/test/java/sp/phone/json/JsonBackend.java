package sp.phone.json;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

import gov.anzong.androidnga.base.json.NgaJson;
import gov.anzong.androidnga.base.json.NgaJsonArray;
import gov.anzong.androidnga.base.json.NgaJsonObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 特征测试的被测实现：fastjson 为对照基准，{@link NgaJson} 为替换实现。两者的对象均实现 {@link Map}，数组均实现 {@link List}。
 *
 * <p>fastjson 默认对象无序，需要比较键顺序时使用 {@link #parseObjectOrdered}。删除 fastjson 时移除 {@link #FASTJSON}。
 */
abstract class JsonBackend {

    abstract Object parse(String text);

    abstract Map<String, Object> parseObject(String text);

    abstract Map<String, Object> parseObjectOrdered(String text);

    abstract List<Object> parseArray(String text);

    abstract <T> T parseObject(String text, Class<T> type);

    abstract <T> List<T> parseArray(String text, Class<T> type);

    abstract <T> T toJavaObject(Map<String, Object> object, Class<T> type);

    abstract String toJSONString(Object value);

    abstract Map<String, Object> newObject();

    /** 调用对象的取值方法，如 getIntValue、getJSONObject。 */
    abstract Object get(Map<String, Object> object, String getter, String key);

    abstract Object get(List<Object> array, String getter, int index);

    static final JsonBackend FASTJSON =
            new JsonBackend() {
                @Override
                Object parse(String text) {
                    return JSON.parse(text);
                }

                @Override
                Map<String, Object> parseObject(String text) {
                    return JSON.parseObject(text);
                }

                @Override
                Map<String, Object> parseObjectOrdered(String text) {
                    return JSON.parseObject(text, Feature.OrderedField);
                }

                @Override
                List<Object> parseArray(String text) {
                    return JSON.parseArray(text);
                }

                @Override
                <T> T parseObject(String text, Class<T> type) {
                    return JSON.parseObject(text, type);
                }

                @Override
                <T> List<T> parseArray(String text, Class<T> type) {
                    return JSON.parseArray(text, type);
                }

                @Override
                <T> T toJavaObject(Map<String, Object> object, Class<T> type) {
                    return JSONObject.toJavaObject((JSONObject) object, type);
                }

                @Override
                String toJSONString(Object value) {
                    return JSON.toJSONString(value);
                }

                @Override
                Map<String, Object> newObject() {
                    return new JSONObject(true);
                }

                @Override
                Object get(Map<String, Object> map, String getter, String key) {
                    JSONObject o = (JSONObject) map;
                    switch (getter) {
                        case "getString": return o.getString(key);
                        case "getIntValue": return o.getIntValue(key);
                        case "getInteger": return o.getInteger(key);
                        case "getLongValue": return o.getLongValue(key);
                        case "getLong": return o.getLong(key);
                        case "getBoolean": return o.getBoolean(key);
                        case "getBooleanValue": return o.getBooleanValue(key);
                        case "getDoubleValue": return o.getDoubleValue(key);
                        case "getJSONObject": return o.getJSONObject(key);
                        case "getJSONArray": return o.getJSONArray(key);
                        case "containsKey": return o.containsKey(key);
                        default: throw new IllegalArgumentException(getter);
                    }
                }

                @Override
                Object get(List<Object> list, String getter, int index) {
                    JSONArray a = (JSONArray) list;
                    switch (getter) {
                        case "getString": return a.getString(index);
                        case "getIntValue": return a.getIntValue(index);
                        case "getJSONObject": return a.getJSONObject(index);
                        default: throw new IllegalArgumentException(getter);
                    }
                }

                @Override
                public String toString() {
                    return "fastjson";
                }
            };

    static final JsonBackend NGA_JSON =
            new JsonBackend() {
                @Override
                Object parse(String text) {
                    return NgaJson.parse(text);
                }

                @Override
                Map<String, Object> parseObject(String text) {
                    return NgaJson.parseObject(text);
                }

                @Override
                Map<String, Object> parseObjectOrdered(String text) {
                    return NgaJson.parseObject(text);
                }

                @Override
                List<Object> parseArray(String text) {
                    return NgaJson.parseArray(text);
                }

                @Override
                <T> T parseObject(String text, Class<T> type) {
                    return NgaJson.parseObject(text, type);
                }

                @Override
                <T> List<T> parseArray(String text, Class<T> type) {
                    return NgaJson.parseArray(text, type);
                }

                @Override
                <T> T toJavaObject(Map<String, Object> object, Class<T> type) {
                    return NgaJson.toJavaObject((NgaJsonObject) object, type);
                }

                @Override
                String toJSONString(Object value) {
                    return NgaJson.toJSONString(value);
                }

                @Override
                Map<String, Object> newObject() {
                    return new NgaJsonObject();
                }

                @Override
                Object get(Map<String, Object> map, String getter, String key) {
                    NgaJsonObject o = (NgaJsonObject) map;
                    switch (getter) {
                        case "getString": return o.getString(key);
                        case "getIntValue": return o.getIntValue(key);
                        case "getInteger": return o.getInteger(key);
                        case "getLongValue": return o.getLongValue(key);
                        case "getLong": return o.getLong(key);
                        case "getBoolean": return o.getBoolean(key);
                        case "getBooleanValue": return o.getBooleanValue(key);
                        case "getDoubleValue": return o.getDoubleValue(key);
                        case "getJSONObject": return o.getJSONObject(key);
                        case "getJSONArray": return o.getJSONArray(key);
                        case "containsKey": return o.containsKey(key);
                        default: throw new IllegalArgumentException(getter);
                    }
                }

                @Override
                Object get(List<Object> list, String getter, int index) {
                    NgaJsonArray a = (NgaJsonArray) list;
                    switch (getter) {
                        case "getString": return a.getString(index);
                        case "getIntValue": return a.getIntValue(index);
                        case "getJSONObject": return a.getJSONObject(index);
                        default: throw new IllegalArgumentException(getter);
                    }
                }

                @Override
                public String toString() {
                    return "NgaJson";
                }
            };

    static Collection<Object[]> all() {
        return Arrays.asList(new Object[] {FASTJSON}, new Object[] {NGA_JSON});
    }
}
