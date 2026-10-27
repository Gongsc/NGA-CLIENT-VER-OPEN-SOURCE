package sp.phone.json;

import gov.anzong.androidnga.base.json.NgaJson;
import gov.anzong.androidnga.base.json.NgaJsonArray;
import gov.anzong.androidnga.base.json.NgaJsonObject;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * 约束 {@link NgaJsonObject} 取值方法对各类原始值的类型转换，转换表由 fastjson 1.1 实测得到。
 *
 * <p>"!" 表示抛出异常（不限定异常类型）。结果前缀：s 字符串、I Integer、L Long、B Boolean、D Double、O 对象、A 数组。
 */
public class NgaJsonCoercionTest {

    private static final String[] GETTERS = {
        "getString", "getIntValue", "getInteger", "getLongValue", "getLong", "getBoolean",
        "getBooleanValue", "getDoubleValue", "getJSONObject", "getJSONArray", "containsKey"
    };

    /** 第一列为 {"v":原始值} 中的原始值，null 表示键不存在。 */
    private static final String[][] TABLE = {
            {null, "null", "I:0", "null", "L:0", "null", "null", "B:false", "D:0.0", "null", "null", "B:false"},
            {"null", "null", "I:0", "null", "L:0", "null", "null", "B:false", "D:0.0", "null", "null", "B:true"},
            {"\"\"", "s:", "I:0", "null", "L:0", "null", "null", "B:false", "D:0.0", "null", "null", "B:true"},
            {"\" \"", "s: ", "!", "!", "!", "!", "!", "!", "!", "null", "null", "B:true"},
            {"\"12\"", "s:12", "I:12", "I:12", "L:12", "L:12", "!", "!", "D:12.0", "!", "!", "B:true"},
            {"\"012\"", "s:012", "I:12", "I:12", "L:12", "L:12", "!", "!", "D:12.0", "!", "!", "B:true"},
            {"\"-3\"", "s:-3", "I:-3", "I:-3", "L:-3", "L:-3", "!", "!", "D:-3.0", "!", "!", "B:true"},
            {"\"1.5\"", "s:1.5", "!", "!", "!", "!", "!", "!", "D:1.5", "!", "!", "B:true"},
            {"\"abc\"", "s:abc", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"},
            {"12", "s:12", "I:12", "I:12", "L:12", "L:12", "B:false", "B:false", "D:12.0", "!", "!", "B:true"},
            {"1.9", "s:1.9", "I:1", "I:1", "L:1", "L:1", "!", "!", "D:1.9", "!", "!", "B:true"},
            {"-1.9", "s:-1.9", "I:-1", "I:-1", "L:-1", "L:-1", "!", "!", "D:-1.9", "!", "!", "B:true"},
            {"3000000000", "s:3000000000", "I:-1294967296", "I:-1294967296", "L:3000000000", "L:3000000000", "B:false", "B:false", "D:3.0E9", "!", "!", "B:true"},
            {"true", "s:true", "!", "!", "!", "!", "B:true", "B:true", "!", "!", "!", "B:true"},
            {"false", "s:false", "!", "!", "!", "!", "B:false", "B:false", "!", "!", "!", "B:true"},
            {"\"true\"", "s:true", "!", "!", "!", "!", "B:true", "B:true", "!", "!", "!", "B:true"},
            {"\"false\"", "s:false", "!", "!", "!", "!", "B:false", "B:false", "!", "!", "!", "B:true"},
            {"\"1\"", "s:1", "I:1", "I:1", "L:1", "L:1", "B:true", "B:true", "D:1.0", "!", "!", "B:true"},
            {"\"0\"", "s:0", "I:0", "I:0", "L:0", "L:0", "B:false", "B:false", "D:0.0", "!", "!", "B:true"},
            {"0", "s:0", "I:0", "I:0", "L:0", "L:0", "B:false", "B:false", "D:0.0", "!", "!", "B:true"},
            {"2", "s:2", "I:2", "I:2", "L:2", "L:2", "B:false", "B:false", "D:2.0", "!", "!", "B:true"},
            {"\"Y\"", "s:Y", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"},
            {"\"N\"", "s:N", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"},
            {"\"T\"", "s:T", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"},
            {"\"F\"", "s:F", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"},
            {"{\"a\":[1,\"x\"],\"b\":2}", "s:{\"a\":[1,\"x\"],\"b\":2}", "!", "!", "!", "!", "!", "!", "!", "O:{\"a\":[1,\"x\"],\"b\":2}", "!", "B:true"},
            {"[1,\"x\"]", "s:[1,\"x\"]", "!", "!", "!", "!", "!", "!", "!", "!", "A:[1,\"x\"]", "B:true"},
            {"\"{\\\"b\\\":1}\"", "s:{\"b\":1}", "!", "!", "!", "!", "!", "!", "!", "O:{\"b\":1}", "!", "B:true"},
            {"\"[1]\"", "s:[1]", "!", "!", "!", "!", "!", "!", "!", "!", "A:[1]", "B:true"},
    };

    @Test
    public void gettersCoerceRawValuesAsRecorded() {
        StringBuilder mismatches = new StringBuilder();
        for (String[] row : TABLE) {
            Map<String, Object> object = NgaJson.parseObject(row[0] == null ? "{}" : "{\"v\":" + row[0] + "}");
            for (int i = 0; i < GETTERS.length; i++) {
                String getter = GETTERS[i];
                String actual = describe(() -> get(object, getter, "v"));
                if (!row[i + 1].equals(actual)) {
                    mismatches.append(row[0]).append(' ').append(GETTERS[i]).append(": 期望 ")
                            .append(row[i + 1]).append("，实际 ").append(actual).append('\n');
                }
            }
        }
        assertEquals("", mismatches.toString());
    }

    @Test
    public void arrayGettersDoNotParseStringElements() {
        List<Object> array = NgaJson.parseArray("[{\"a\":1},2,\"x\",null,\"3\",\"{\\\"b\\\":2}\"]");
        assertEquals("{\"a\":1}", get(array, "getString", 0));
        assertEquals("2", get(array, "getString", 1));
        assertEquals("x", get(array, "getString", 2));
        assertEquals(null, get(array, "getString", 3));
        assertEquals(3, get(array, "getIntValue", 4));
        assertEquals(0, get(array, "getIntValue", 3));
        assertEquals("!", describe(() -> get(array, "getJSONObject", 5)));
    }

    @Test
    public void additionalCoercions() {
        Map<String, Object> o =
                NgaJson.parseObject(
                        "{\"d\":1e3,\"t\":\"TRUE\",\"n\":\"null\",\"l\":4294967297,\"f\":1.9,\"z\":{\"a\":[1,{\"b\":null}]}}");
        assertEquals("1000.0", get(o, "getString", "d"));
        assertEquals(true, get(o, "getBoolean", "t"));
        assertEquals(null, get(o, "getInteger", "n"));
        assertEquals(true, get(o, "getBoolean", "l"));
        assertEquals(1L, get(o, "getLongValue", "f"));
        assertEquals("{\"a\":[1,{}]}", get(o, "getJSONObject", "z").toString());
    }

    private interface Call {
        Object run();
    }

    private static String describe(Call call) {
        Object value;
        try {
            value = call.run();
        } catch (RuntimeException e) {
            return "!";
        }
        if (value == null) return "null";
        if (value instanceof String) return "s:" + value;
        if (value instanceof Integer) return "I:" + value;
        if (value instanceof Long) return "L:" + value;
        if (value instanceof Boolean) return "B:" + value;
        if (value instanceof Double) return "D:" + value;
        if (value instanceof Map) return "O:" + NgaJson.toJSONString(value);
        if (value instanceof List) return "A:" + NgaJson.toJSONString(value);
        return value.getClass().getSimpleName() + ":" + value;
    }

    /** 调用对象的取值方法，如 getIntValue、getJSONObject。 */
    private static Object get(Map<String, Object> map, String getter, String key) {
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

    private static Object get(List<Object> list, String getter, int index) {
        NgaJsonArray a = (NgaJsonArray) list;
        switch (getter) {
            case "getString": return a.getString(index);
            case "getIntValue": return a.getIntValue(index);
            case "getJSONObject": return a.getJSONObject(index);
            default: throw new IllegalArgumentException(getter);
        }
    }
}
