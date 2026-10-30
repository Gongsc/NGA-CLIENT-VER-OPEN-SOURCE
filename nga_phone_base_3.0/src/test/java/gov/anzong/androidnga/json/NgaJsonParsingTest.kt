package gov.anzong.androidnga.json

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * 约束 [NgaJson] 对宽松输入、数字、键顺序与序列化的处理，行为沿用 fastjson 1.1（期望值由 fastjson 实测得到）。
 *
 * NGA 接口返回的 JSON 并不规范，业务代码隐式依赖这些行为。对象统一保持插入顺序。见 docs/fastjson-migration.md。
 */
class NgaJsonParsingTest {

    @Test
    fun lenientInputIsAccepted() {
        val cases = listOf(
            "{\"a\":000123}" to "{\"a\":123}",
            "{\"a\":-0012}" to "{\"a\":-12}",
            "{a:1}" to "{\"a\":1}",
            "{0:1,a_b:2,_c:3,中:4}" to "{\"0\":1,\"a_b\":2,\"_c\":3,\"中\":4}",
            "{'a':'x'}" to "{\"a\":\"x\"}",
            "{\"a\":1}/*x*/" to "{\"a\":1}",
            "{/*c*/\"a\":/*d*/1}" to "{\"a\":1}",
            "{\"a\":1,}" to "{\"a\":1}",
            "{\"a\":1,,\"b\":2}" to "{\"a\":1,\"b\":2}",
            "{,\"a\":1}" to "{\"a\":1}",
            "{\"a\":[1,2,]}" to "{\"a\":[1,2]}",
            "{\"a\":[1,,2]}" to "{\"a\":[1,2]}",
            "{\"a\":\"x\ny\"}" to "{\"a\":\"x\\ny\"}",
            "{\"a\":\"\t\"}" to "{\"a\":\"\\t\"}",
            "{\"a\":\"\\u4e2d\\/\"}" to "{\"a\":\"中/\"}",
            "{\"a\":\"\\x41\"}" to "{\"a\":\"A\"}",
            "{\"a\":\"\\'\"}" to "{\"a\":\"'\"}",
            "{\"a\":1,\"a\":2}" to "{\"a\":2}",
            "{\"a\":undefined}" to "{}",
            "{\"a\":[undefined,1]}" to "{\"a\":[null,1]}",
            "{\"a\":1E2,\"b\":-1.5e-1}" to "{\"a\":100,\"b\":-0.15}",
        )
        for ((source, expected) in cases) assertEquals(source, expected, NgaJson.toJSONString(NgaJson.parseObject(source)))
    }

    @Test
    fun malformedInputIsRejected() {
        val sources = listOf(
            "{\"a\":1} xyz", "{\"a\":+1}", "{\"a\":.5}", "{\"a\":NaN}", "{\"a\":'it''s'}",
            "{\"a\":TRUE}", "[1]", "{\"a\":1}// c", "{a\$:2}", "{\$a:2}", "{a-b:2}", "{1a:2}", "{a.b:2}",
            "{\"a\":\"\\q\"}", "{\"a\":12abc}",
        )
        for (source in sources) {
            try {
                NgaJson.parseObject(source)
                fail("应拒绝：$source")
            } catch (expected: RuntimeException) {
                // 记录为抛出异常，不限定异常类型。
            }
        }
        try {
            NgaJson.parseArray("{\"a\":1}")
            fail("parseArray 应拒绝对象")
        } catch (expected: RuntimeException) {
        }
        try {
            NgaJson.parse("abc")
            fail("parse 应拒绝裸字符")
        } catch (expected: RuntimeException) {
        }
    }

    @Test
    fun blankAndNullDocumentsReturnNull() {
        for (source in listOf("", "   ", "null")) {
            assertNull(NgaJson.parseObject(source))
            assertNull(NgaJson.parseArray(source))
            assertNull(NgaJson.parseArray(source, String::class.java))
        }
        assertNull(NgaJson.parse(""))
        assertNull(NgaJson.parse("null"))
    }

    @Test
    fun parseReturnsMatchingContainerOrScalar() {
        assertEquals(1, NgaJson.parse("1"))
        assertEquals("x", NgaJson.parse("\"x\""))
        assertTrue(NgaJson.parse("{\"a\":1}") is Map<*, *>)
        assertTrue(NgaJson.parse("[1]") is List<*>)
        assertTrue(NgaJson.parseObject("{\"a\":[]}")!!["a"] is List<*>)
        assertTrue(NgaJson.parseObject("{\"a\":{}}")!!["a"] is Map<*, *>)
    }

    @Test
    fun arraysKeepElementsAndNulls() {
        assertEquals("[1,\"2\",null]", NgaJson.toJSONString(NgaJson.parseArray("[1,\"2\",null]")))
        assertEquals("[1]", NgaJson.toJSONString(NgaJson.parseArray("[1,]")))
        assertEquals(listOf("a", "b"), NgaJson.parseArray("[\"a\",\"b\"]", String::class.java))
        assertEquals(listOf("1", "2"), NgaJson.parseArray("[1,2]", String::class.java))
        assertEquals(listOf<String?>(null), NgaJson.parseArray("[null]", String::class.java))
    }

    @Test
    fun numbersKeepTheirMagnitudeAndText() {
        val o = NgaJson.parseObject("{\"i\":1,\"l\":12345678901,\"h\":123456789012345678901234,\"d\":1.50,\"e\":1e3}")!!
        assertEquals(Integer::class.java, o["i"]!!.javaClass)
        assertEquals(java.lang.Long::class.java, o["l"]!!.javaClass)
        assertEquals(BigInteger::class.java, o["h"]!!.javaClass)
        assertEquals(BigDecimal::class.java, o["d"]!!.javaClass)
        assertEquals("1.50", o["d"].toString())
        assertEquals(1000.0, (o["e"] as Number).toDouble(), 0.0)
        assertEquals("{\"e\":1000}", NgaJson.toJSONString(NgaJson.parseObject("{\"e\":1e3}")))
        assertEquals("{\"a\":0,\"b\":0.0}", NgaJson.toJSONString(NgaJson.parseObject("{\"a\":-0,\"b\":-0.0}")))
        // 9 位以内为 Integer，10 位起为 Long，超出 long 为 BigInteger。
        val bounds = NgaJson.parseObject(
            "{\"a\":999999999,\"b\":2147483647,\"c\":-2147483648,\"d\":9223372036854775807," +
                "\"e\":9223372036854775808}",
        )!!
        assertEquals(Integer::class.java, bounds["a"]!!.javaClass)
        assertEquals(java.lang.Long::class.java, bounds["b"]!!.javaClass)
        assertEquals(java.lang.Long::class.java, bounds["c"]!!.javaClass)
        assertEquals(java.lang.Long::class.java, bounds["d"]!!.javaClass)
        assertEquals(BigInteger::class.java, bounds["e"]!!.javaClass)
    }

    @Test
    fun orderedObjectsKeepSourceOrderIncludingNestedObjects() {
        val source = "{\"b\":1,\"a\":2,\"10\":3,\"2\":4,\"x\":{\"b\":1,\"a\":2,\"10\":3,\"2\":4}}"
        val ordered = NgaJson.parseObject(source)!!
        assertEquals(listOf("b", "a", "10", "2", "x"), ordered.keys.toList())
        val nested = ordered["x"] as Map<*, *>
        assertEquals(listOf("b", "a", "10", "2"), nested.keys.toList())
        val built = NgaJsonObject()
        built["b"] = 1
        built["a"] = 2
        built["10"] = 3
        assertEquals("{\"b\":1,\"a\":2,\"10\":3}", NgaJson.toJSONString(built))
    }

    @Test
    fun serializationEscapesOnlyWhatJsonRequiresAndDropsNulls() {
        val o = NgaJsonObject()
        o["s"] = "中文/<>&\"\\\n\t\u0001\u2028"
        o["n"] = null
        o["b"] = true
        o["d"] = 1.0
        o["f"] = 1.5f
        o["l"] = 12345678901L
        o["arr"] = listOf(1, null, "x")
        assertEquals(
            "{\"s\":\"中文/<>&\\\"\\\\\\n\\t\\u0001\\u2028\",\"b\":true,\"d\":1,\"f\":1.5," +
                "\"l\":12345678901,\"arr\":[1,null,\"x\"]}",
            NgaJson.toJSONString(o),
        )
        assertEquals("\"\\u001F\\u007F\u2029\\b\\f\\r\"", NgaJson.toJSONString("\u001f\u007f\u2029\b\u000C\r"))
        assertEquals("[\"a\",\"中\",\"\\\"q\\\"\"]", NgaJson.toJSONString(listOf("a", "中", "\"q\"")))
        val map = linkedMapOf<String, Any?>("fid" to 1, "topped_topic" to null)
        assertEquals("[{\"fid\":1}]", NgaJson.toJSONString(listOf(map)))
        val keys = linkedMapOf<String, Any?>("a\"b" to 1, "中" to "x")
        assertEquals("{\"a\\\"b\":1,\"中\":\"x\"}", NgaJson.toJSONString(keys))
        assertEquals("\"x\"", NgaJson.toJSONString("x"))
        assertEquals("null", NgaJson.toJSONString(null))
        assertEquals(
            "[1.0E20,1,-0.5,100,1.0E-7,null,1.25,1.2345678E7]",
            NgaJson.toJSONString(listOf<Any>(1e20, 1.0, -0.5, 100.0, 1.0E-7, Double.NaN, 1.25f, 12345678.0)),
        )
    }

    @Test
    fun reparsingSerializedTreeIsStable() {
        val source =
            "{\"__T\":{\"tid\":12,\"subject\":\"主题\"},\"__R\":{\"0\":{\"content\":\"正文\\n<br>\",\"lou\":0}}," +
                "\"__U\":{\"9\":{\"uid\":9,\"username\":\"用户\"}},\"__ROWS\":1,\"__R__ROWS\":1}"
        val first = NgaJson.parseObject(source)
        val second = NgaJson.parseObject(NgaJson.toJSONString(first))!!
        assertEquals(NgaJson.toJSONString(first), NgaJson.toJSONString(second))
        assertEquals(listOf("__T", "__R", "__U", "__ROWS", "__R__ROWS"), second.keys.toList())
    }
}
