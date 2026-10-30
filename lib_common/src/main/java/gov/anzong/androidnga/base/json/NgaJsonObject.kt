package gov.anzong.androidnga.base.json

/**
 * JSON 对象，保持键的插入顺序。取值方法的名称与转换规则沿用 fastjson 的 `JSONObject`，便于迁移。
 *
 * `getXxxValue` 在键缺失、值为 null 或空字符串时返回默认值；`getInteger` 等包装类型方法返回 null。
 */
class NgaJsonObject : LinkedHashMap<String, Any?> {

    constructor()

    constructor(values: Map<String, *>) : super(values)

    fun getString(key: String): String? = JsonCasts.toStringValue(get(key))

    fun getInteger(key: String): Int? = JsonCasts.toInteger(get(key))

    fun getIntValue(key: String): Int = getInteger(key) ?: 0

    fun getLong(key: String): Long? = JsonCasts.toLong(get(key))

    fun getLongValue(key: String): Long = getLong(key) ?: 0L

    fun getBoolean(key: String): Boolean? = JsonCasts.toBoolean(get(key))

    fun getBooleanValue(key: String): Boolean = getBoolean(key) ?: false

    fun getDoubleValue(key: String): Double = JsonCasts.toDouble(get(key)) ?: 0.0

    fun getFloatValue(key: String): Float = JsonCasts.toFloat(get(key)) ?: 0f

    /** 值为 JSON 文本的字符串会被解析；其他非对象值抛出 [NgaJsonException]。 */
    fun getJSONObject(key: String): NgaJsonObject? = JsonCasts.toJsonObject(get(key))

    fun getJSONArray(key: String): NgaJsonArray? = JsonCasts.toJsonArray(get(key))

    /** 精确匹配属性名绑定实体，见 [NgaJson.toJavaObject]。 */
    fun <T : Any> toJavaObject(type: Class<T>): T? = NgaJson.toJavaObject(this, type)

    fun toJSONString(): String = JsonWriter.write(this)

    override fun toString(): String = toJSONString()
}
