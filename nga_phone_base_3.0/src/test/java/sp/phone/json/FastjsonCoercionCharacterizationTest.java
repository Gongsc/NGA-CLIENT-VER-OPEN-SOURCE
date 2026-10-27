package sp.phone.json;

import static org.junit.Assert.assertEquals;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.junit.Test;

/**
 * 特征测试：记录 fastjson 取值方法对各类原始值的类型转换结果。
 *
 * <p>替换 fastjson 时，新实现须逐格保持一致；"!" 表示抛出异常（不限定异常类型）。结果前缀：s 字符串、I Integer、
 * L Long、B Boolean、D Double、O 对象、A 数组。
 */
public class FastjsonCoercionCharacterizationTest {

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
            JSONObject object = JSON.parseObject(row[0] == null ? "{}" : "{\"v\":" + row[0] + "}");
            for (int i = 0; i < GETTERS.length; i++) {
                String actual = describe(object, GETTERS[i]);
                if (!row[i + 1].equals(actual)) {
                    mismatches.append(row[0]).append(' ').append(GETTERS[i]).append(": 期望 ")
                            .append(row[i + 1]).append("，实际 ").append(actual).append('\n');
                }
            }
        }
        assertEquals("", mismatches.toString());
    }

    @Test
    public void arrayGetStringSerializesNestedObjects() {
        JSONArray array = JSON.parseArray("[{\"a\":1},2,\"x\",null]");
        assertEquals("{\"a\":1}", array.getString(0));
        assertEquals("2", array.getString(1));
        assertEquals("x", array.getString(2));
        assertEquals(null, array.getString(3));
    }

    private static String describe(JSONObject object, String getter) {
        Object value;
        try {
            value = call(object, getter);
        } catch (RuntimeException e) {
            return "!";
        }
        if (value == null) return "null";
        if (value instanceof String) return "s:" + value;
        if (value instanceof Integer) return "I:" + value;
        if (value instanceof Long) return "L:" + value;
        if (value instanceof Boolean) return "B:" + value;
        if (value instanceof Double) return "D:" + value;
        if (value instanceof JSONObject) return "O:" + ((JSONObject) value).toJSONString();
        if (value instanceof JSONArray) return "A:" + ((JSONArray) value).toJSONString();
        return value.getClass().getSimpleName() + ":" + value;
    }

    private static Object call(JSONObject object, String getter) {
        switch (getter) {
            case "getString": return object.getString("v");
            case "getIntValue": return object.getIntValue("v");
            case "getInteger": return object.getInteger("v");
            case "getLongValue": return object.getLongValue("v");
            case "getLong": return object.getLong("v");
            case "getBoolean": return object.getBoolean("v");
            case "getBooleanValue": return object.getBooleanValue("v");
            case "getDoubleValue": return object.getDoubleValue("v");
            case "getJSONObject": return object.getJSONObject("v");
            case "getJSONArray": return object.getJSONArray("v");
            case "containsKey": return object.containsKey("v");
            default: throw new IllegalArgumentException(getter);
        }
    }
}
