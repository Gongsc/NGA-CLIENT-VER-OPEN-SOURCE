package gov.anzong.androidnga.base.json

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale
import java.util.TreeMap
import java.util.concurrent.ConcurrentHashMap

/**
 * 按 fastjson 1.1 的 JavaBean 规则绑定与导出实体：属性来自 setter/getter 与公有字段。
 *
 * 两种绑定模式与 fastjson 的两条路径对应：
 *
 * - 文本模式（[NgaJson.parseObject]）：键名先精确匹配，再忽略大小写，最后再忽略下划线与连字符；
 *   数字须为整数字面量且不溢出，空字符串与 null 保留基本类型默认值。
 * - 精确模式（[NgaJson.toJavaObject]）：键名精确匹配；按 [JsonCasts] 转换，空字符串与 null 写入基本类型时失败。
 */
internal object JsonBeans {

    /** 文本模式下不写入属性，保留默认值。 */
    private val SKIP = Any()

    private val CACHE = ConcurrentHashMap<Class<*>, BeanInfo>()

    private class Property(val name: String, val type: Type, val method: Method?, val field: Field?) {

        fun read(target: Any): Any? = if (method != null) method.invoke(target) else field!!.get(target)

        fun write(target: Any, value: Any?) {
            if (method != null) {
                method.invoke(target, value)
            } else {
                field!!.set(target, value)
            }
        }
    }

    private class BeanInfo {
        val writable = LinkedHashMap<String, Property>()
        val readable = TreeMap<String, Property>()
    }

    private fun info(type: Class<*>): BeanInfo {
        CACHE[type]?.let { return it }
        val info = BeanInfo()
        for (method in type.methods) {
            if (Modifier.isStatic(method.modifiers)) {
                continue
            }
            val name = method.name
            if (method.parameterTypes.size == 1 && name.startsWith("set")) {
                val property = propertyName(name, 3)
                if (property != null) {
                    method.isAccessible = true
                    info.writable[property] = Property(property, method.genericParameterTypes[0], method, null)
                }
            } else if (method.parameterTypes.isEmpty() && method.returnType != Void.TYPE) {
                var property: String? = null
                if (name.startsWith("get") && name != "getClass") {
                    property = propertyName(name, 3)
                } else if (name.startsWith("is") &&
                    (method.returnType == java.lang.Boolean.TYPE || method.returnType == java.lang.Boolean::class.java)
                ) {
                    property = propertyName(name, 2)
                }
                if (property != null) {
                    method.isAccessible = true
                    info.readable[property] = Property(property, method.genericReturnType, method, null)
                }
            }
        }
        for (field in type.fields) {
            val modifiers = field.modifiers
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                continue
            }
            field.isAccessible = true
            val property = Property(field.name, field.genericType, null, field)
            info.readable.putIfAbsent(field.name, property)
            if (!Modifier.isFinal(modifiers)) {
                info.writable.putIfAbsent(field.name, property)
            }
        }
        CACHE[type] = info
        return info
    }

    /** `getISANONYMOUS` → `iSANONYMOUS`，`get_isInBlackList` → `isInBlackList`。 */
    private fun propertyName(methodName: String, prefix: Int): String? {
        if (methodName.length <= prefix) {
            return null
        }
        val c = methodName[prefix]
        if (c == '_') {
            return if (methodName.length > prefix + 1) methodName.substring(prefix + 1) else null
        }
        if (Character.isUpperCase(c)) {
            return Character.toLowerCase(c) + methodName.substring(prefix + 1)
        }
        return null
    }

    fun toMap(bean: Any): Map<String, Any?> {
        val values = LinkedHashMap<String, Any?>()
        for (property in info(bean.javaClass).readable.values) {
            try {
                values[property.name] = property.read(bean)
            } catch (e: ReflectiveOperationException) {
                throw NgaJsonException("无法读取属性 ${property.name}", e)
            }
        }
        return values
    }

    fun <T : Any> bind(source: NgaJsonObject, type: Class<T>, lenient: Boolean): T {
        val target = newInstance(type)
        val info = info(type)
        for (entry in source.entries) {
            val property = find(info.writable, entry.key, lenient)
            if (property == null) {
                if (lenient) {
                    fillReadOnlyContainer(target, info, entry)
                }
                continue
            }
            val value = convert(entry.value, property.type, lenient)
            if (value === SKIP) {
                continue
            }
            try {
                property.write(target, value)
            } catch (e: ReflectiveOperationException) {
                throw NgaJsonException("无法写入属性 ${property.name}", e)
            }
        }
        return target
    }

    /** 文本模式下，只有读取方法的集合属性按读取到的实例追加内容。 */
    @Suppress("UNCHECKED_CAST")
    private fun fillReadOnlyContainer(target: Any, info: BeanInfo, entry: Map.Entry<String, Any?>) {
        val property = find(info.readable, entry.key, true)
        if (property?.method == null) {
            return
        }
        try {
            val current = property.read(target)
            val value = convert(entry.value, property.type, true)
            if (current is MutableCollection<*> && value is Collection<*>) {
                (current as MutableCollection<Any?>).addAll(value)
            } else if (current is MutableMap<*, *> && value is Map<*, *>) {
                (current as MutableMap<Any?, Any?>).putAll(value)
            }
        } catch (e: ReflectiveOperationException) {
            throw NgaJsonException("无法写入属性 ${property.name}", e)
        }
    }

    private fun find(properties: Map<String, Property>, key: String, lenient: Boolean): Property? {
        val exact = properties[key]
        if (exact != null || !lenient) {
            return exact
        }
        properties.values.firstOrNull { it.name.equals(key, ignoreCase = true) }?.let { return it }
        val normalized = normalize(key)
        return properties.values.firstOrNull { normalize(it.name) == normalized }
    }

    private fun normalize(name: String): String = name.replace("_", "").replace("-", "").lowercase(Locale.ROOT)

    private fun <T : Any> newInstance(type: Class<T>): T {
        try {
            val constructor = type.getDeclaredConstructor()
            constructor.isAccessible = true
            return constructor.newInstance()
        } catch (e: ReflectiveOperationException) {
            throw NgaJsonException("无法创建 ${type.name}", e)
        }
    }

    /** 转换为目标类型；文本模式下可能返回 [SKIP]。 */
    fun convert(value: Any?, type: Type, lenient: Boolean): Any? {
        val raw = rawClass(type)
        when {
            raw == Any::class.java -> return value
            raw == String::class.java -> return JsonCasts.toStringValue(value)
            raw == NgaJsonObject::class.java -> return JsonCasts.toJsonObject(value)
            raw == NgaJsonArray::class.java -> return JsonCasts.toJsonArray(value)
            isScalar(raw) -> return if (lenient) convertLexical(value, raw) else convertCast(value, raw)
        }
        if (value == null || "" == value) {
            return null
        }
        if (Map::class.java.isAssignableFrom(raw)) {
            if (value !is NgaJsonObject) {
                throw JsonCasts.cannotCast(value, raw.simpleName)
            }
            val valueType = typeArgument(type, 1)
            val result = LinkedHashMap<String, Any?>()
            for ((key, item) in value) {
                result[key] = element(item, valueType, lenient)
            }
            return result
        }
        if (Collection::class.java.isAssignableFrom(raw)) {
            if (value !is NgaJsonArray) {
                throw JsonCasts.cannotCast(value, raw.simpleName)
            }
            val elementType = typeArgument(type, 0)
            val result: MutableCollection<Any?> =
                if (Set::class.java.isAssignableFrom(raw)) HashSet() else ArrayList()
            for (item in value) {
                result.add(element(item, elementType, lenient))
            }
            return result
        }
        if (raw.isEnum) {
            return enumValue(raw, value.toString())
        }
        if (value is NgaJsonObject) {
            return bind(value, raw, lenient)
        }
        throw JsonCasts.cannotCast(value, raw.simpleName)
    }

    private fun element(value: Any?, type: Type, lenient: Boolean): Any? {
        val result = convert(value, type, lenient)
        return if (result === SKIP) null else result
    }

    @Suppress("UNCHECKED_CAST")
    private fun enumValue(type: Class<*>, name: String): Any =
        java.lang.Enum.valueOf(type as Class<out Enum<*>>, name)

    private fun isScalar(raw: Class<*>): Boolean =
        raw.isPrimitive ||
            raw == java.lang.Integer::class.java ||
            raw == java.lang.Long::class.java ||
            raw == java.lang.Short::class.java ||
            raw == java.lang.Byte::class.java ||
            raw == java.lang.Boolean::class.java ||
            raw == java.lang.Float::class.java ||
            raw == java.lang.Double::class.java ||
            raw == BigDecimal::class.java ||
            raw == BigInteger::class.java

    private fun isInt(raw: Class<*>) = raw == Integer.TYPE || raw == java.lang.Integer::class.java
    private fun isLong(raw: Class<*>) = raw == java.lang.Long.TYPE || raw == java.lang.Long::class.java
    private fun isShort(raw: Class<*>) = raw == java.lang.Short.TYPE || raw == java.lang.Short::class.java
    private fun isByte(raw: Class<*>) = raw == java.lang.Byte.TYPE || raw == java.lang.Byte::class.java
    private fun isBoolean(raw: Class<*>) = raw == java.lang.Boolean.TYPE || raw == java.lang.Boolean::class.java
    private fun isFloat(raw: Class<*>) = raw == java.lang.Float.TYPE || raw == java.lang.Float::class.java
    private fun isDouble(raw: Class<*>) = raw == java.lang.Double.TYPE || raw == java.lang.Double::class.java

    /** 精确模式：沿用取值方法的转换，null 写入基本类型时失败。 */
    private fun convertCast(value: Any?, raw: Class<*>): Any? {
        val result: Any? = when {
            isInt(raw) -> JsonCasts.toInteger(value)
            isLong(raw) -> JsonCasts.toLong(value)
            isShort(raw) -> JsonCasts.toInteger(value)?.toShort()
            isByte(raw) -> JsonCasts.toInteger(value)?.toByte()
            isBoolean(raw) -> JsonCasts.toBoolean(value)
            isFloat(raw) -> JsonCasts.toFloat(value)
            isDouble(raw) -> JsonCasts.toDouble(value)
            else -> bigNumber(value, raw)
        }
        if (result == null && raw.isPrimitive) {
            throw NgaJsonException("无法将空值写入 ${raw.name}")
        }
        return result
    }

    /** 文本模式：与 fastjson 词法读取一致，整数属性只接受未溢出的整数字面量或数字字符串。 */
    private fun convertLexical(value: Any?, raw: Class<*>): Any? {
        if (value == null || "" == value) {
            return if (raw.isPrimitive) SKIP else null
        }
        if (isInt(raw) || isShort(raw) || isByte(raw)) {
            val number = when {
                value is Int -> value
                value is Long && value >= Int.MIN_VALUE && value <= Int.MAX_VALUE -> value.toInt()
                value is String -> Integer.parseInt(value)
                else -> throw JsonCasts.cannotCast(value, raw.simpleName)
            }
            return when {
                isShort(raw) -> number.toShort()
                isByte(raw) -> number.toByte()
                else -> number
            }
        }
        if (isLong(raw)) {
            return when (value) {
                is Int, is Long -> (value as Number).toLong()
                is String -> java.lang.Long.parseLong(value)
                else -> throw JsonCasts.cannotCast(value, raw.simpleName)
            }
        }
        if (isBoolean(raw)) {
            return when (value) {
                is Boolean -> value
                is Int, is Long -> (value as Number).toInt() == 1
                is String -> JsonCasts.toBoolean(value)
                else -> throw JsonCasts.cannotCast(value, raw.simpleName)
            }
        }
        if (isFloat(raw)) {
            return JsonCasts.toFloat(value)
        }
        if (isDouble(raw)) {
            return JsonCasts.toDouble(value)
        }
        return bigNumber(value, raw)
    }

    private fun bigNumber(value: Any?, raw: Class<*>): Any? {
        if (value == null || "" == value) {
            return null
        }
        if (raw == BigDecimal::class.java) {
            return value as? BigDecimal ?: BigDecimal(value.toString())
        }
        if (value is BigInteger) {
            return value
        }
        if (value is Number || value is String) {
            return BigInteger(value.toString())
        }
        throw JsonCasts.cannotCast(value, raw.simpleName)
    }

    private fun rawClass(type: Type): Class<*> = when (type) {
        is Class<*> -> type
        is ParameterizedType -> rawClass(type.rawType)
        is WildcardType -> rawClass(type.upperBounds[0])
        else -> Any::class.java
    }

    private fun typeArgument(type: Type, index: Int): Type {
        if (type is ParameterizedType) {
            val arguments = type.actualTypeArguments
            if (index < arguments.size) {
                return arguments[index]
            }
        }
        return Any::class.java
    }

    fun <T> bindList(array: NgaJsonArray, type: Class<T>): MutableList<T> {
        val result = ArrayList<T>(array.size)
        for (item in array) {
            @Suppress("UNCHECKED_CAST")
            result.add(type.cast(element(item, type, true)) as T)
        }
        return result
    }
}
