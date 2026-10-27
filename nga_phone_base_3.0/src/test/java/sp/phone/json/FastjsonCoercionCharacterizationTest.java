package sp.phone.json;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 特征测试：记录 fastjson 取值方法对各类原始值的类型转换结果。
 *
 * <p>替换 fastjson 时，新实现须逐格保持一致；"!" 表示抛出异常（不限定异常类型）。结果前缀：s 字符串、I Integer、
 * L Long、B Boolean、D Double、O 对象、A 数组。每个用例同时针对 fastjson 与 {@code NgaJson} 运行。
 */
@RunWith(Parameterized.class)
public class FastjsonCoercionCharacterizationTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> backends() {
        return JsonBackend.all();
    }

    private final JsonBackend json;

    public FastjsonCoercionCharacterizationTest(JsonBackend json) {
        this.json = json;
    }

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
            Map<String, Object> object = json.parseObject(row[0] == null ? "{}" : "{\"v\":" + row[0] + "}");
            for (int i = 0; i < GETTERS.length; i++) {
                String getter = GETTERS[i];
                String actual = describe(() -> json.get(object, getter, "v"));
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
        List<Object> array = json.parseArray("[{\"a\":1},2,\"x\",null,\"3\",\"{\\\"b\\\":2}\"]");
        assertEquals("{\"a\":1}", json.get(array, "getString", 0));
        assertEquals("2", json.get(array, "getString", 1));
        assertEquals("x", json.get(array, "getString", 2));
        assertEquals(null, json.get(array, "getString", 3));
        assertEquals(3, json.get(array, "getIntValue", 4));
        assertEquals(0, json.get(array, "getIntValue", 3));
        assertEquals("!", describe(() -> json.get(array, "getJSONObject", 5)));
    }

    @Test
    public void additionalCoercions() {
        Map<String, Object> o =
                json.parseObject(
                        "{\"d\":1e3,\"t\":\"TRUE\",\"n\":\"null\",\"l\":4294967297,\"f\":1.9,\"z\":{\"a\":[1,{\"b\":null}]}}");
        assertEquals("1000.0", json.get(o, "getString", "d"));
        assertEquals(true, json.get(o, "getBoolean", "t"));
        assertEquals(null, json.get(o, "getInteger", "n"));
        assertEquals(true, json.get(o, "getBoolean", "l"));
        assertEquals(1L, json.get(o, "getLongValue", "f"));
        assertEquals("{\"a\":[1,{}]}", json.get(o, "getJSONObject", "z").toString());
    }

    private interface Call {
        Object run();
    }

    private String describe(Call call) {
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
        if (value instanceof Map) return "O:" + json.toJSONString(value);
        if (value instanceof List) return "A:" + json.toJSONString(value);
        return value.getClass().getSimpleName() + ":" + value;
    }
}
