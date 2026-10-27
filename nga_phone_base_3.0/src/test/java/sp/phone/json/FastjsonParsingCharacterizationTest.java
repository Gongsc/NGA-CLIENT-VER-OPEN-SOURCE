package sp.phone.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 特征测试：记录 fastjson 对宽松输入、数字、键顺序与序列化的处理结果。
 *
 * <p>NGA 接口返回的 JSON 并不规范，业务代码隐式依赖这些行为。替换实现须保持本类断言成立；HashMap 造成的无序键
 * 不属于需要保留的行为，新实现可以保持插入顺序。
 */
public class FastjsonParsingCharacterizationTest {

    @Test
    public void lenientInputIsAccepted() {
        String[][] cases = {
            {"{\"a\":000123}", "{\"a\":123}"},
            {"{\"a\":-0012}", "{\"a\":-12}"},
            {"{a:1}", "{\"a\":1}"},
            {"{'a':'x'}", "{\"a\":\"x\"}"},
            {"{\"a\":1}/*x*/", "{\"a\":1}"},
            {"{\"a\":1,}", "{\"a\":1}"},
            {"{\"a\":1,,\"b\":2}", "{\"a\":1,\"b\":2}"},
            {"{\"a\":[1,2,]}", "{\"a\":[1,2]}"},
            {"{\"a\":\"x\ny\"}", "{\"a\":\"x\\ny\"}"},
            {"{\"a\":\"\t\"}", "{\"a\":\"\\t\"}"},
            {"{\"a\":\"\\u4e2d\\/\"}", "{\"a\":\"中/\"}"},
            {"{\"a\":\"\\x41\"}", "{\"a\":\"A\"}"},
            {"{\"a\":1,\"a\":2}", "{\"a\":2}"},
            {"{\"a\":undefined}", "{}"},
        };
        for (String[] c : cases) assertEquals(c[0], c[1], JSON.parseObject(c[0]).toJSONString());
    }

    @Test
    public void malformedInputIsRejected() {
        for (String source :
                new String[] {
                    "{\"a\":1} xyz", "{\"a\":+1}", "{\"a\":.5}", "{\"a\":NaN}", "{\"a\":'it''s'}",
                    "{\"a\":TRUE}", "[1]"
                }) {
            try {
                JSON.parseObject(source);
                fail("应拒绝：" + source);
            } catch (RuntimeException expected) {
                // 记录为抛出异常，不限定异常类型。
            }
        }
        try {
            JSON.parseArray("{\"a\":1}");
            fail("parseArray 应拒绝对象");
        } catch (RuntimeException expected) {
        }
        try {
            JSON.parse("abc");
            fail("parse 应拒绝裸字符");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void blankAndNullDocumentsReturnNull() {
        for (String source : new String[] {"", "   ", "null"}) {
            assertNull(JSON.parseObject(source));
            assertNull(JSON.parseArray(source));
            assertNull(JSON.parseArray(source, String.class));
        }
        assertNull(JSON.parse(""));
        assertNull(JSON.parse("null"));
    }

    @Test
    public void parseReturnsMatchingContainerOrScalar() {
        assertEquals(1, JSON.parse("1"));
        assertEquals("x", JSON.parse("\"x\""));
        assertTrue(JSON.parse("{\"a\":1}") instanceof JSONObject);
        assertTrue(JSON.parse("[1]") instanceof JSONArray);
        assertTrue(JSON.parseObject("{\"a\":[]}").get("a") instanceof JSONArray);
        assertTrue(JSON.parseObject("{\"a\":{}}").get("a") instanceof JSONObject);
    }

    @Test
    public void arraysKeepElementsAndNulls() {
        assertEquals("[1,\"2\",null]", JSON.parseArray("[1,\"2\",null]").toJSONString());
        assertEquals("[1]", JSON.parseArray("[1,]").toJSONString());
        assertEquals(Arrays.asList("a", "b"), JSON.parseArray("[\"a\",\"b\"]", String.class));
        assertEquals(Arrays.asList("1", "2"), JSON.parseArray("[1,2]", String.class));
        assertEquals(Arrays.asList((String) null), JSON.parseArray("[null]", String.class));
    }

    @Test
    public void numbersKeepTheirMagnitudeAndText() {
        JSONObject o =
                JSON.parseObject(
                        "{\"i\":1,\"l\":12345678901,\"h\":123456789012345678901234,\"d\":1.50,\"e\":1e3}");
        assertEquals(Integer.class, o.get("i").getClass());
        assertEquals(Long.class, o.get("l").getClass());
        assertEquals(BigInteger.class, o.get("h").getClass());
        assertEquals(BigDecimal.class, o.get("d").getClass());
        assertEquals("1.50", o.getString("d"));
        assertEquals(1000.0, ((Number) o.get("e")).doubleValue(), 0);
        assertEquals("{\"e\":1000}", JSON.parseObject("{\"e\":1e3}").toJSONString());
    }

    @Test
    public void orderedFieldKeepsSourceOrderIncludingNestedObjects() {
        String source = "{\"b\":1,\"a\":2,\"10\":3,\"2\":4,\"x\":{\"b\":1,\"a\":2,\"10\":3,\"2\":4}}";
        JSONObject ordered = JSON.parseObject(source, Feature.OrderedField);
        assertEquals(Arrays.asList("b", "a", "10", "2", "x"), new ArrayList<>(ordered.keySet()));
        assertEquals(
                Arrays.asList("b", "a", "10", "2"),
                new ArrayList<>(ordered.getJSONObject("x").keySet()));
        JSONObject built = new JSONObject(true);
        built.put("b", 1);
        built.put("a", 2);
        built.put("10", 3);
        assertEquals("{\"b\":1,\"a\":2,\"10\":3}", built.toJSONString());
    }

    @Test
    public void serializationEscapesOnlyWhatJsonRequiresAndDropsNulls() {
        JSONObject o = new JSONObject(true);
        o.put("s", "中文/<>&\"\\\n\t\u0001\u2028");
        o.put("n", null);
        o.put("b", true);
        o.put("d", 1.0);
        o.put("f", 1.5f);
        o.put("l", 12345678901L);
        o.put("arr", Arrays.asList(1, null, "x"));
        assertEquals(
                "{\"s\":\"中文/<>&\\\"\\\\\\n\\t\\u0001\\u2028\",\"b\":true,\"d\":1,\"f\":1.5,"
                        + "\"l\":12345678901,\"arr\":[1,null,\"x\"]}",
                o.toJSONString());
        assertEquals("[\"a\",\"中\",\"\\\"q\\\"\"]", JSON.toJSONString(Arrays.asList("a", "中", "\"q\"")));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fid", 1);
        map.put("topped_topic", null);
        assertEquals("[{\"fid\":1}]", JSON.toJSONString(Arrays.asList(map)));
        assertEquals("\"x\"", JSON.toJSONString("x"));
        assertEquals("null", JSON.toJSONString(null));
    }

    @Test
    public void reparsingSerializedTreeIsStable() {
        String source =
                "{\"__T\":{\"tid\":12,\"subject\":\"主题\"},\"__R\":{\"0\":{\"content\":\"正文\\n<br>\",\"lou\":0}},"
                        + "\"__U\":{\"9\":{\"uid\":9,\"username\":\"用户\"}},\"__ROWS\":1,\"__R__ROWS\":1}";
        JSONObject first = JSON.parseObject(source, Feature.OrderedField);
        JSONObject second = JSON.parseObject(first.toJSONString(), Feature.OrderedField);
        assertEquals(first.toJSONString(), second.toJSONString());
        List<String> keys = new ArrayList<>(second.keySet());
        assertEquals(Arrays.asList("__T", "__R", "__U", "__ROWS", "__R__ROWS"), keys);
    }
}
