package gov.anzong.androidnga.base.json

/**
 * JSON 解析与序列化入口，用于替换 fastjson 1.1，行为以 docs/fastjson-migration.md 与特征测试为准。
 *
 * 空白文档与 `null` 解析为 null；解析或转换失败抛出 [NgaJsonException] 或其他运行时异常。
 */
object NgaJson {

    /** 返回 [NgaJsonObject]、[NgaJsonArray]、字符串、数字、布尔值或 null。 */
    @JvmStatic
    fun parse(text: String?): Any? = JsonParser.parse(text)

    @JvmStatic
    fun parseObject(text: String?): NgaJsonObject? {
        val value = JsonParser.parse(text)
        if (value == null || value is NgaJsonObject) {
            return value as NgaJsonObject?
        }
        throw JsonCasts.cannotCast(value, "JSON 对象")
    }

    @JvmStatic
    fun parseArray(text: String?): NgaJsonArray? {
        val value = JsonParser.parse(text)
        if (value == null || value is NgaJsonArray) {
            return value as NgaJsonArray?
        }
        throw JsonCasts.cannotCast(value, "JSON 数组")
    }

    /** 宽松匹配键名（忽略大小写、下划线与连字符）绑定实体。 */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> parseObject(text: String?, type: Class<T>): T? {
        val obj = parseObject(text)
        if (obj == null || type.isAssignableFrom(NgaJsonObject::class.java)) {
            return obj as T?
        }
        if (Map::class.java.isAssignableFrom(type)) {
            throw JsonCasts.cannotCast(obj, type.simpleName)
        }
        return JsonBeans.bind(obj, type, true)
    }

    /** 宽松匹配键名逐项绑定，null 元素保留为 null。 */
    @JvmStatic
    fun <T> parseArray(text: String?, type: Class<T>): MutableList<T>? =
        parseArray(text)?.let { JsonBeans.bindList(it, type) }

    /** 仅按属性名精确匹配绑定实体，供接口树形数据转换使用。 */
    @JvmStatic
    fun <T : Any> toJavaObject(obj: NgaJsonObject?, type: Class<T>): T? =
        obj?.let { JsonBeans.bind(it, type, false) }

    /** 省略值为 null 的键；实体按属性名字母顺序输出。 */
    @JvmStatic
    fun toJSONString(value: Any?): String = JsonWriter.write(value)
}
