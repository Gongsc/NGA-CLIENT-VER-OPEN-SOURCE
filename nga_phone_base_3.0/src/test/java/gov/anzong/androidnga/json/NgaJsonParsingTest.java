package gov.anzong.androidnga.json;

import gov.anzong.androidnga.base.json.NgaJson;
import gov.anzong.androidnga.base.json.NgaJsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 约束 {@link NgaJson} 对宽松输入、数字、键顺序与序列化的处理，行为沿用 fastjson 1.1（期望值由 fastjson 实测得到）。
 *
 * <p>NGA 接口返回的 JSON 并不规范，业务代码隐式依赖这些行为。对象统一保持插入顺序。见 docs/fastjson-migration.md。
 */
public class NgaJsonParsingTest {

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
        for (String[] c : cases) assertEquals(c[0], c[1], NgaJson.toJSONString(NgaJson.parseObject(c[0])));
    }

    @Test
    public void malformedInputIsRejected() {
        for (String source :
                new String[] {
                    "{\"a\":1} xyz", "{\"a\":+1}", "{\"a\":.5}", "{\"a\":NaN}", "{\"a\":'it''s'}",
                    "{\"a\":TRUE}", "[1]", "{\"a\":1}// c", "{a$:2}", "{$a:2}", "{a-b:2}", "{1a:2}", "{a.b:2}", "{\"a\":\"\\q\"}", "{\"a\":12abc}"
                }) {
            try {
                NgaJson.parseObject(source);
                fail("应拒绝：" + source);
            } catch (RuntimeException expected) {
                // 记录为抛出异常，不限定异常类型。
            }
        }
        try {
            NgaJson.parseArray("{\"a\":1}");
            fail("parseArray 应拒绝对象");
        } catch (RuntimeException expected) {
        }
        try {
            NgaJson.parse("abc");
            fail("parse 应拒绝裸字符");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void blankAndNullDocumentsReturnNull() {
        for (String source : new String[] {"", "   ", "null"}) {
            assertNull(NgaJson.parseObject(source));
            assertNull(NgaJson.parseArray(source));
            assertNull(NgaJson.parseArray(source, String.class));
        }
        assertNull(NgaJson.parse(""));
        assertNull(NgaJson.parse("null"));
    }

    @Test
    public void parseReturnsMatchingContainerOrScalar() {
        assertEquals(1, NgaJson.parse("1"));
        assertEquals("x", NgaJson.parse("\"x\""));
        assertTrue(NgaJson.parse("{\"a\":1}") instanceof Map);
        assertTrue(NgaJson.parse("[1]") instanceof List);
        assertTrue(NgaJson.parseObject("{\"a\":[]}").get("a") instanceof List);
        assertTrue(NgaJson.parseObject("{\"a\":{}}").get("a") instanceof Map);
    }

    @Test
    public void arraysKeepElementsAndNulls() {
        assertEquals("[1,\"2\",null]", NgaJson.toJSONString(NgaJson.parseArray("[1,\"2\",null]")));
        assertEquals("[1]", NgaJson.toJSONString(NgaJson.parseArray("[1,]")));
        assertEquals(Arrays.asList("a", "b"), NgaJson.parseArray("[\"a\",\"b\"]", String.class));
        assertEquals(Arrays.asList("1", "2"), NgaJson.parseArray("[1,2]", String.class));
        assertEquals(Arrays.asList((String) null), NgaJson.parseArray("[null]", String.class));
    }

    @Test
    public void numbersKeepTheirMagnitudeAndText() {
        Map<String, Object> o =
                NgaJson.parseObject(
                        "{\"i\":1,\"l\":12345678901,\"h\":123456789012345678901234,\"d\":1.50,\"e\":1e3}");
        assertEquals(Integer.class, o.get("i").getClass());
        assertEquals(Long.class, o.get("l").getClass());
        assertEquals(BigInteger.class, o.get("h").getClass());
        assertEquals(BigDecimal.class, o.get("d").getClass());
        assertEquals("1.50", o.get("d").toString());
        assertEquals(1000.0, ((Number) o.get("e")).doubleValue(), 0);
        assertEquals("{\"e\":1000}", NgaJson.toJSONString(NgaJson.parseObject("{\"e\":1e3}")));
        assertEquals("{\"a\":0,\"b\":0.0}", NgaJson.toJSONString(NgaJson.parseObject("{\"a\":-0,\"b\":-0.0}")));
        // 9 位以内为 Integer，10 位起为 Long，超出 long 为 BigInteger。
        Map<String, Object> bounds =
                NgaJson.parseObject(
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
        Map<String, Object> ordered = NgaJson.parseObject(source);
        assertEquals(Arrays.asList("b", "a", "10", "2", "x"), new ArrayList<>(ordered.keySet()));
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) ordered.get("x");
        assertEquals(Arrays.asList("b", "a", "10", "2"), new ArrayList<>(nested.keySet()));
        Map<String, Object> built = new NgaJsonObject();
        built.put("b", 1);
        built.put("a", 2);
        built.put("10", 3);
        assertEquals("{\"b\":1,\"a\":2,\"10\":3}", NgaJson.toJSONString(built));
    }

    @Test
    public void serializationEscapesOnlyWhatJsonRequiresAndDropsNulls() {
        Map<String, Object> o = new NgaJsonObject();
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
                NgaJson.toJSONString(o));
        assertEquals("\"\\u001F\\u007F\u2029\\b\\f\\r\"", NgaJson.toJSONString("\u001f\u007f\u2029\b\f\r"));
        assertEquals("[\"a\",\"中\",\"\\\"q\\\"\"]", NgaJson.toJSONString(Arrays.asList("a", "中", "\"q\"")));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fid", 1);
        map.put("topped_topic", null);
        assertEquals("[{\"fid\":1}]", NgaJson.toJSONString(Arrays.asList(map)));
        Map<String, Object> keys = new LinkedHashMap<>();
        keys.put("a\"b", 1);
        keys.put("中", "x");
        assertEquals("{\"a\\\"b\":1,\"中\":\"x\"}", NgaJson.toJSONString(keys));
        assertEquals("\"x\"", NgaJson.toJSONString("x"));
        assertEquals("null", NgaJson.toJSONString(null));
        assertEquals(
                "[1.0E20,1,-0.5,100,1.0E-7,null,1.25,1.2345678E7]",
                NgaJson.toJSONString(Arrays.asList(1e20, 1.0, -0.5, 100.0, 1.0E-7, Double.NaN, 1.25f, 12345678.0)));
    }

    @Test
    public void reparsingSerializedTreeIsStable() {
        String source =
                "{\"__T\":{\"tid\":12,\"subject\":\"主题\"},\"__R\":{\"0\":{\"content\":\"正文\\n<br>\",\"lou\":0}},"
                        + "\"__U\":{\"9\":{\"uid\":9,\"username\":\"用户\"}},\"__ROWS\":1,\"__R__ROWS\":1}";
        Map<String, Object> first = NgaJson.parseObject(source);
        Map<String, Object> second = NgaJson.parseObject(NgaJson.toJSONString(first));
        assertEquals(NgaJson.toJSONString(first), NgaJson.toJSONString(second));
        assertEquals(
                Arrays.asList("__T", "__R", "__U", "__ROWS", "__R__ROWS"), new ArrayList<>(second.keySet()));
    }
}
