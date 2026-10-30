package gov.anzong.androidnga.json

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.json.NgaJsonArray
import gov.anzong.androidnga.base.json.NgaJsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 约束 [NgaJsonObject] 取值方法对各类原始值的类型转换，转换表由 fastjson 1.1 实测得到。
 *
 * "!" 表示抛出异常（不限定异常类型）。结果前缀：s 字符串、I Integer、L Long、B Boolean、D Double、O 对象、A 数组。
 */
class NgaJsonCoercionTest {

    @Test
    fun gettersCoerceRawValuesAsRecorded() {
        val mismatches = StringBuilder()
        for (row in TABLE) {
            val obj = NgaJson.parseObject(if (row[0] == null) "{}" else "{\"v\":" + row[0] + "}")!!
            GETTERS.forEachIndexed { i, getter ->
                val actual = describe { get(obj, getter, "v") }
                if (row[i + 1] != actual) {
                    mismatches.append(row[0]).append(' ').append(getter).append(": 期望 ")
                        .append(row[i + 1]).append("，实际 ").append(actual).append('\n')
                }
            }
        }
        assertEquals("", mismatches.toString())
    }

    @Test
    fun arrayGettersDoNotParseStringElements() {
        val array = NgaJson.parseArray("[{\"a\":1},2,\"x\",null,\"3\",\"{\\\"b\\\":2}\"]")!!
        assertEquals("{\"a\":1}", get(array, "getString", 0))
        assertEquals("2", get(array, "getString", 1))
        assertEquals("x", get(array, "getString", 2))
        assertEquals(null, get(array, "getString", 3))
        assertEquals(3, get(array, "getIntValue", 4))
        assertEquals(0, get(array, "getIntValue", 3))
        assertEquals("!", describe { get(array, "getJSONObject", 5) })
    }

    @Test
    fun additionalCoercions() {
        val o = NgaJson.parseObject(
            "{\"d\":1e3,\"t\":\"TRUE\",\"n\":\"null\",\"l\":4294967297,\"f\":1.9,\"z\":{\"a\":[1,{\"b\":null}]}}",
        )!!
        assertEquals("1000.0", get(o, "getString", "d"))
        assertEquals(true, get(o, "getBoolean", "t"))
        assertEquals(null, get(o, "getInteger", "n"))
        assertEquals(true, get(o, "getBoolean", "l"))
        assertEquals(1L, get(o, "getLongValue", "f"))
        assertEquals("{\"a\":[1,{}]}", get(o, "getJSONObject", "z").toString())
    }

    private fun describe(call: () -> Any?): String {
        val value = try {
            call()
        } catch (e: RuntimeException) {
            return "!"
        }
        return when (value) {
            null -> "null"
            is String -> "s:$value"
            is Int -> "I:$value"
            is Long -> "L:$value"
            is Boolean -> "B:$value"
            is Double -> "D:$value"
            is Map<*, *> -> "O:" + NgaJson.toJSONString(value)
            is List<*> -> "A:" + NgaJson.toJSONString(value)
            else -> value.javaClass.simpleName + ":" + value
        }
    }

    /** 调用对象的取值方法，如 getIntValue、getJSONObject。 */
    private fun get(o: NgaJsonObject, getter: String, key: String): Any? = when (getter) {
        "getString" -> o.getString(key)
        "getIntValue" -> o.getIntValue(key)
        "getInteger" -> o.getInteger(key)
        "getLongValue" -> o.getLongValue(key)
        "getLong" -> o.getLong(key)
        "getBoolean" -> o.getBoolean(key)
        "getBooleanValue" -> o.getBooleanValue(key)
        "getDoubleValue" -> o.getDoubleValue(key)
        "getJSONObject" -> o.getJSONObject(key)
        "getJSONArray" -> o.getJSONArray(key)
        "containsKey" -> o.containsKey(key)
        else -> throw IllegalArgumentException(getter)
    }

    private fun get(a: NgaJsonArray, getter: String, index: Int): Any? = when (getter) {
        "getString" -> a.getString(index)
        "getIntValue" -> a.getIntValue(index)
        "getJSONObject" -> a.getJSONObject(index)
        else -> throw IllegalArgumentException(getter)
    }

    private companion object {

        val GETTERS = listOf(
            "getString", "getIntValue", "getInteger", "getLongValue", "getLong", "getBoolean",
            "getBooleanValue", "getDoubleValue", "getJSONObject", "getJSONArray", "containsKey",
        )

        /** 第一列为 {"v":原始值} 中的原始值，null 表示键不存在。 */
        val TABLE: List<Array<String?>> = listOf(
            arrayOf(null, "null", "I:0", "null", "L:0", "null", "null", "B:false", "D:0.0", "null", "null", "B:false"),
            arrayOf("null", "null", "I:0", "null", "L:0", "null", "null", "B:false", "D:0.0", "null", "null", "B:true"),
            arrayOf("\"\"", "s:", "I:0", "null", "L:0", "null", "null", "B:false", "D:0.0", "null", "null", "B:true"),
            arrayOf("\" \"", "s: ", "!", "!", "!", "!", "!", "!", "!", "null", "null", "B:true"),
            arrayOf("\"12\"", "s:12", "I:12", "I:12", "L:12", "L:12", "!", "!", "D:12.0", "!", "!", "B:true"),
            arrayOf("\"012\"", "s:012", "I:12", "I:12", "L:12", "L:12", "!", "!", "D:12.0", "!", "!", "B:true"),
            arrayOf("\"-3\"", "s:-3", "I:-3", "I:-3", "L:-3", "L:-3", "!", "!", "D:-3.0", "!", "!", "B:true"),
            arrayOf("\"1.5\"", "s:1.5", "!", "!", "!", "!", "!", "!", "D:1.5", "!", "!", "B:true"),
            arrayOf("\"abc\"", "s:abc", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"),
            arrayOf("12", "s:12", "I:12", "I:12", "L:12", "L:12", "B:false", "B:false", "D:12.0", "!", "!", "B:true"),
            arrayOf("1.9", "s:1.9", "I:1", "I:1", "L:1", "L:1", "!", "!", "D:1.9", "!", "!", "B:true"),
            arrayOf("-1.9", "s:-1.9", "I:-1", "I:-1", "L:-1", "L:-1", "!", "!", "D:-1.9", "!", "!", "B:true"),
            arrayOf("3000000000", "s:3000000000", "I:-1294967296", "I:-1294967296", "L:3000000000", "L:3000000000", "B:false", "B:false", "D:3.0E9", "!", "!", "B:true"),
            arrayOf("true", "s:true", "!", "!", "!", "!", "B:true", "B:true", "!", "!", "!", "B:true"),
            arrayOf("false", "s:false", "!", "!", "!", "!", "B:false", "B:false", "!", "!", "!", "B:true"),
            arrayOf("\"true\"", "s:true", "!", "!", "!", "!", "B:true", "B:true", "!", "!", "!", "B:true"),
            arrayOf("\"false\"", "s:false", "!", "!", "!", "!", "B:false", "B:false", "!", "!", "!", "B:true"),
            arrayOf("\"1\"", "s:1", "I:1", "I:1", "L:1", "L:1", "B:true", "B:true", "D:1.0", "!", "!", "B:true"),
            arrayOf("\"0\"", "s:0", "I:0", "I:0", "L:0", "L:0", "B:false", "B:false", "D:0.0", "!", "!", "B:true"),
            arrayOf("0", "s:0", "I:0", "I:0", "L:0", "L:0", "B:false", "B:false", "D:0.0", "!", "!", "B:true"),
            arrayOf("2", "s:2", "I:2", "I:2", "L:2", "L:2", "B:false", "B:false", "D:2.0", "!", "!", "B:true"),
            arrayOf("\"Y\"", "s:Y", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"),
            arrayOf("\"N\"", "s:N", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"),
            arrayOf("\"T\"", "s:T", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"),
            arrayOf("\"F\"", "s:F", "!", "!", "!", "!", "!", "!", "!", "!", "!", "B:true"),
            arrayOf("{\"a\":[1,\"x\"],\"b\":2}", "s:{\"a\":[1,\"x\"],\"b\":2}", "!", "!", "!", "!", "!", "!", "!", "O:{\"a\":[1,\"x\"],\"b\":2}", "!", "B:true"),
            arrayOf("[1,\"x\"]", "s:[1,\"x\"]", "!", "!", "!", "!", "!", "!", "!", "!", "A:[1,\"x\"]", "B:true"),
            arrayOf("\"{\\\"b\\\":1}\"", "s:{\"b\":1}", "!", "!", "!", "!", "!", "!", "!", "O:{\"b\":1}", "!", "B:true"),
            arrayOf("\"[1]\"", "s:[1]", "!", "!", "!", "!", "!", "!", "!", "!", "A:[1]", "B:true"),
        )
    }
}
