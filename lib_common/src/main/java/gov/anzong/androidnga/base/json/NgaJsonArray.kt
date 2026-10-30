package gov.anzong.androidnga.base.json

/** JSON 数组。按下标取值的方法沿用 fastjson 的 `JSONArray`；与对象不同，元素不会从字符串解析为对象或数组。 */
class NgaJsonArray : ArrayList<Any?> {

    constructor()

    constructor(values: Collection<*>) : super(values)

    fun getString(index: Int): String? = JsonCasts.toStringValue(get(index))

    fun getInteger(index: Int): Int? = JsonCasts.toInteger(get(index))

    fun getIntValue(index: Int): Int = getInteger(index) ?: 0

    fun getLongValue(index: Int): Long = JsonCasts.toLong(get(index)) ?: 0L

    fun getBooleanValue(index: Int): Boolean = JsonCasts.toBoolean(get(index)) ?: false

    fun getJSONObject(index: Int): NgaJsonObject? {
        val value = get(index)
        if (value == null || value is NgaJsonObject) {
            return value as NgaJsonObject?
        }
        throw JsonCasts.cannotCast(value, "JSON 对象")
    }

    fun getJSONArray(index: Int): NgaJsonArray? {
        val value = get(index)
        if (value == null || value is NgaJsonArray) {
            return value as NgaJsonArray?
        }
        throw JsonCasts.cannotCast(value, "JSON 数组")
    }

    fun toJSONString(): String = JsonWriter.write(this)

    override fun toString(): String = toJSONString()
}
