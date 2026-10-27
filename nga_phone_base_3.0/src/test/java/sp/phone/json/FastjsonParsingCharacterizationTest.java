package sp.phone.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 特征测试：记录 fastjson 对宽松输入、数字、键顺序与序列化的处理结果，并要求 {@code NgaJson} 保持一致。
 *
 * <p>NGA 接口返回的 JSON 并不规范，业务代码隐式依赖这些行为。HashMap 造成的无序键不属于需要保留的行为，
 * 新实现统一保持插入顺序。
 */
@RunWith(Parameterized.class)
public class FastjsonParsingCharacterizationTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> backends() {
        return JsonBackend.all();
    }

    private final JsonBackend json;

    public FastjsonParsingCharacterizationTest(JsonBackend json) {
        this.json = json;
    }

    @Test
    public void lenientInputIsAccepted() {
        String[][] cases = {
            {"{\"a\":000123}", "{\"a\":123}"},
            {"{\"a\":-0012}", "{\"a\":-12}"},
            {"{a:1}", "{\"a\":1}"},
            {"{0:1,a_b:2,_c:3,中:4}", "{\"0\":1,\"a_b\":2,\"_c\":3,\"中\":4}"},
            {"{'a':'x'}", "{\"a\":\"x\"}"},
            {"{\"a\":1}/*x*/", "{\"a\":1}"},
            {"{/*c*/\"a\":/*d*/1}", "{\"a\":1}"},
            {"{\"a\":1,}", "{\"a\":1}"},
            {"{\"a\":1,,\"b\":2}", "{\"a\":1,\"b\":2}"},
            {"{,\"a\":1}", "{\"a\":1}"},
            {"{\"a\":[1,2,]}", "{\"a\":[1,2]}"},
            {"{\"a\":[1,,2]}", "{\"a\":[1,2]}"},
            {"{\"a\":\"x\ny\"}", "{\"a\":\"x\\ny\"}"},
            {"{\"a\":\"\t\"}", "{\"a\":\"\\t\"}"},
            {"{\"a\":\"\\u4e2d\\/\"}", "{\"a\":\"中/\"}"},
            {"{\"a\":\"\\x41\"}", "{\"a\":\"A\"}"},
            {"{\"a\":\"\\'\"}", "{\"a\":\"'\"}"},
            {"{\"a\":1,\"a\":2}", "{\"a\":2}"},
            {"{\"a\":undefined}", "{}"},
            {"{\"a\":[undefined,1]}", "{\"a\":[null,1]}"},
            {"{\"a\":1E2,\"b\":-1.5e-1}", "{\"a\":100,\"b\":-0.15}"},
        };
        for (String[] c : cases) assertEquals(c[0], c[1], json.toJSONString(json.parseObjectOrdered(c[0])));
    }

    @Test
    public void malformedInputIsRejected() {
        for (String source :
                new String[] {
                    "{\"a\":1} xyz", "{\"a\":+1}", "{\"a\":.5}", "{\"a\":NaN}", "{\"a\":'it''s'}",
                    "{\"a\":TRUE}", "[1]", "{\"a\":1}// c", "{a$:2}", "{$a:2}", "{a-b:2}", "{1a:2}", "{a.b:2}", "{\"a\":\"\\q\"}", "{\"a\":12abc}"
                }) {
            try {
                json.parseObject(source);
                fail("应拒绝：" + source);
            } catch (RuntimeException expected) {
                // 记录为抛出异常，不限定异常类型。
            }
        }
        try {
            json.parseArray("{\"a\":1}");
            fail("parseArray 应拒绝对象");
        } catch (RuntimeException expected) {
        }
        try {
            json.parse("abc");
            fail("parse 应拒绝裸字符");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void blankAndNullDocumentsReturnNull() {
        for (String source : new String[] {"", "   ", "null"}) {
            assertNull(json.parseObject(source));
            assertNull(json.parseArray(source));
            assertNull(json.parseArray(source, String.class));
        }
        assertNull(json.parse(""));
        assertNull(json.parse("null"));
    }

    @Test
    public void parseReturnsMatchingContainerOrScalar() {
        assertEquals(1, json.parse("1"));
        assertEquals("x", json.parse("\"x\""));
        assertTrue(json.parse("{\"a\":1}") instanceof Map);
        assertTrue(json.parse("[1]") instanceof List);
        assertTrue(json.parseObject("{\"a\":[]}").get("a") instanceof List);
        assertTrue(json.parseObject("{\"a\":{}}").get("a") instanceof Map);
    }

    @Test
    public void arraysKeepElementsAndNulls() {
        assertEquals("[1,\"2\",null]", json.toJSONString(json.parseArray("[1,\"2\",null]")));
        assertEquals("[1]", json.toJSONString(json.parseArray("[1,]")));
        assertEquals(Arrays.asList("a", "b"), json.parseArray("[\"a\",\"b\"]", String.class));
        assertEquals(Arrays.asList("1", "2"), json.parseArray("[1,2]", String.class));
        assertEquals(Arrays.asList((String) null), json.parseArray("[null]", String.class));
    }

    @Test
    public void numbersKeepTheirMagnitudeAndText() {
        Map<String, Object> o =
                json.parseObject(
                        "{\"i\":1,\"l\":12345678901,\"h\":123456789012345678901234,\"d\":1.50,\"e\":1e3}");
        assertEquals(Integer.class, o.get("i").getClass());
        assertEquals(Long.class, o.get("l").getClass());
        assertEquals(BigInteger.class, o.get("h").getClass());
        assertEquals(BigDecimal.class, o.get("d").getClass());
        assertEquals("1.50", o.get("d").toString());
        assertEquals(1000.0, ((Number) o.get("e")).doubleValue(), 0);
        assertEquals("{\"e\":1000}", json.toJSONString(json.parseObject("{\"e\":1e3}")));
        assertEquals("{\"a\":0,\"b\":0.0}", json.toJSONString(json.parseObject("{\"a\":-0,\"b\":-0.0}")));
        // 9 位以内为 Integer，10 位起为 Long，超出 long 为 BigInteger。
        Map<String, Object> bounds =
                json.parseObject(
                        "{\"a\":999999999,\"b\":2147483647,\"c\":-2147483648,\"d\":9223372036854775807,"
                                + "\"e\":9223372036854775808}");
        assertEquals(Integer.class, bounds.get("a").getClass());
        assertEquals(Long.class, bounds.get("b").getClass());
        assertEquals(Long.class, bounds.get("c").getClass());
        assertEquals(Long.class, bounds.get("d").getClass());
        assertEquals(BigInteger.class, bounds.get("e").getClass());
    }

    @Test
    public void orderedObjectsKeepSourceOrderIncludingNestedObjects() {
        String source = "{\"b\":1,\"a\":2,\"10\":3,\"2\":4,\"x\":{\"b\":1,\"a\":2,\"10\":3,\"2\":4}}";
        Map<String, Object> ordered = json.parseObjectOrdered(source);
        assertEquals(Arrays.asList("b", "a", "10", "2", "x"), new ArrayList<>(ordered.keySet()));
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) ordered.get("x");
        assertEquals(Arrays.asList("b", "a", "10", "2"), new ArrayList<>(nested.keySet()));
        Map<String, Object> built = json.newObject();
        built.put("b", 1);
        built.put("a", 2);
        built.put("10", 3);
        assertEquals("{\"b\":1,\"a\":2,\"10\":3}", json.toJSONString(built));
    }

    @Test
    public void serializationEscapesOnlyWhatJsonRequiresAndDropsNulls() {
        Map<String, Object> o = json.newObject();
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
                json.toJSONString(o));
        assertEquals("\"\\u001F\\u007F\u2029\\b\\f\\r\"", json.toJSONString("\u001f\u007f\u2029\b\f\r"));
        assertEquals("[\"a\",\"中\",\"\\\"q\\\"\"]", json.toJSONString(Arrays.asList("a", "中", "\"q\"")));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fid", 1);
        map.put("topped_topic", null);
        assertEquals("[{\"fid\":1}]", json.toJSONString(Arrays.asList(map)));
        Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("a\"b", 1);
        keys.put("中", "x");
        assertEquals("{\"a\\\"b\":1,\"中\":\"x\"}", json.toJSONString(keys));
        assertEquals("\"x\"", json.toJSONString("x"));
        assertEquals("null", json.toJSONString(null));
        assertEquals(
                "[1.0E20,1,-0.5,100,1.0E-7,null,1.25,1.2345678E7]",
                json.toJSONString(Arrays.asList(1e20, 1.0, -0.5, 100.0, 1.0E-7, Double.NaN, 1.25f, 12345678.0)));
    }

    @Test
    public void reparsingSerializedTreeIsStable() {
        String source =
                "{\"__T\":{\"tid\":12,\"subject\":\"主题\"},\"__R\":{\"0\":{\"content\":\"正文\\n<br>\",\"lou\":0}},"
                        + "\"__U\":{\"9\":{\"uid\":9,\"username\":\"用户\"}},\"__ROWS\":1,\"__R__ROWS\":1}";
        Map<String, Object> first = json.parseObjectOrdered(source);
        Map<String, Object> second = json.parseObjectOrdered(json.toJSONString(first));
        assertEquals(json.toJSONString(first), json.toJSONString(second));
        assertEquals(
                Arrays.asList("__T", "__R", "__U", "__ROWS", "__R__ROWS"), new ArrayList<>(second.keySet()));
    }
}
