package gov.anzong.androidnga.base.json

import java.math.BigDecimal

/**
 * 取值方法与 [NgaJson.toJavaObject] 共用的类型转换，规则与 fastjson 1.1 的 TypeUtils 对齐。
 *
 * 空字符串与字符串 "null" 视为 null；数字转整数时截断并允许溢出；布尔值只认 1/0 与 true/false。
 */
internal object JsonCasts {

    private fun isNullText(value: Any?): Boolean =
        value is String && (value.isEmpty() || "null".equals(value, ignoreCase = true))

    fun toStringValue(value: Any?): String? = when (value) {
        null -> null
        is String -> value
        is Map<*, *>, is Collection<*> -> JsonWriter.write(value)
        else -> value.toString()
    }

    fun toInteger(value: Any?): Int? = when {
        value == null || isNullText(value) -> null
        value is Int -> value
        value is Number -> value.toInt()
        value is String -> Integer.parseInt(value)
        else -> throw cannotCast(value, "int")
    }

    fun toLong(value: Any?): Long? = when {
        value == null || isNullText(value) -> null
        value is Number -> value.toLong()
        value is String ->
            try {
                java.lang.Long.parseLong(value)
            } catch (e: NumberFormatException) {
                throw NgaJsonException("无法转换为 long：$value", e)
            }
        else -> throw cannotCast(value, "long")
    }

    fun toDouble(value: Any?): Double? = when {
        value == null || isNullText(value) -> null
        value is Number -> value.toDouble()
        value is String -> java.lang.Double.parseDouble(value)
        else -> throw cannotCast(value, "double")
    }

    fun toFloat(value: Any?): Float? = toDouble(value)?.toFloat()

    fun toBoolean(value: Any?): Boolean? {
        if (value == null || isNullText(value)) {
            return null
        }
        when (value) {
            is Boolean -> return value
            is BigDecimal -> return value.intValueExact() == 1
            is Number -> return value.toInt() == 1
            is String -> {
                if ("true".equals(value, ignoreCase = true) || "1" == value) {
                    return true
                }
                if ("false".equals(value, ignoreCase = true) || "0" == value) {
                    return false
                }
            }
        }
        throw cannotCast(value, "boolean")
    }

    /** 字符串值按 JSON 解析；数字等其他类型无法转换。 */
    @Suppress("UNCHECKED_CAST")
    fun toJsonObject(value: Any?): NgaJsonObject? {
        when (value) {
            null -> return null
            is NgaJsonObject -> return value
            is Map<*, *> -> return NgaJsonObject(value as Map<String, Any?>)
            is String -> {
                val parsed = JsonParser.parse(value)
                if (parsed == null || parsed is NgaJsonObject) {
                    return parsed as NgaJsonObject?
                }
            }
        }
        throw cannotCast(value, "JSON 对象")
    }

    fun toJsonArray(value: Any?): NgaJsonArray? {
        when (value) {
            null -> return null
            is NgaJsonArray -> return value
            is List<*> -> return NgaJsonArray(value)
            is String -> {
                val parsed = JsonParser.parse(value)
                if (parsed == null || parsed is NgaJsonArray) {
                    return parsed as NgaJsonArray?
                }
            }
        }
        throw cannotCast(value, "JSON 数组")
    }

    fun cannotCast(value: Any, target: String): NgaJsonException =
        NgaJsonException("无法将 ${value.javaClass.simpleName} 转换为 $target：$value")
}
