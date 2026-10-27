package gov.anzong.androidnga.base.json;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 fastjson 1.1 的 JavaBean 规则绑定与导出实体：属性来自 setter/getter 与公有字段。
 *
 * <p>两种绑定模式与 fastjson 的两条路径对应：
 *
 * <ul>
 *   <li>文本模式（{@link NgaJson#parseObject(String, Class)}）：键名先精确匹配，再忽略大小写，最后再忽略下划线与连字符；
 *       数字须为整数字面量且不溢出，空字符串与 null 保留基本类型默认值。
 *   <li>精确模式（{@link NgaJson#toJavaObject}）：键名精确匹配；按 {@link JsonCasts} 转换，空字符串与 null 写入基本类型时失败。
 * </ul>
 */
final class JsonBeans {

    /** 文本模式下不写入属性，保留默认值。 */
    private static final Object SKIP = new Object();

    private static final Map<Class<?>, BeanInfo> CACHE = new ConcurrentHashMap<>();

    private JsonBeans() {}

    private static final class Property {
        final String name;
        final Type type;
        final Method method;
        final Field field;

        Property(String name, Type type, Method method, Field field) {
            this.name = name;
            this.type = type;
            this.method = method;
            this.field = field;
        }

        Object read(Object target) throws ReflectiveOperationException {
            return method != null ? method.invoke(target) : field.get(target);
        }

        void write(Object target, Object value) throws ReflectiveOperationException {
            if (method != null) {
                method.invoke(target, value);
            } else {
                field.set(target, value);
            }
        }
    }

    private static final class BeanInfo {
        final Map<String, Property> writable = new LinkedHashMap<>();
        final Map<String, Property> readable = new TreeMap<>();
    }

    private static BeanInfo info(Class<?> type) {
        BeanInfo cached = CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        BeanInfo info = new BeanInfo();
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            String name = method.getName();
            if (method.getParameterTypes().length == 1 && name.startsWith("set")) {
                String property = propertyName(name, 3);
                if (property != null) {
                    method.setAccessible(true);
                    info.writable.put(
                            property,
                            new Property(property, method.getGenericParameterTypes()[0], method, null));
                }
            } else if (method.getParameterTypes().length == 0 && method.getReturnType() != void.class) {
                String property = null;
                if (name.startsWith("get") && !name.equals("getClass")) {
                    property = propertyName(name, 3);
                } else if (name.startsWith("is")
                        && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {
                    property = propertyName(name, 2);
                }
                if (property != null) {
                    method.setAccessible(true);
                    info.readable.put(
                            property, new Property(property, method.getGenericReturnType(), method, null));
                }
            }
        }
        for (Field field : type.getFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                continue;
            }
            field.setAccessible(true);
            Property property = new Property(field.getName(), field.getGenericType(), null, field);
            if (!info.readable.containsKey(field.getName())) {
                info.readable.put(field.getName(), property);
            }
            if (!Modifier.isFinal(modifiers) && !info.writable.containsKey(field.getName())) {
                info.writable.put(field.getName(), property);
            }
        }
        CACHE.put(type, info);
        return info;
    }

    /** {@code getISANONYMOUS} → {@code iSANONYMOUS}，{@code get_isInBlackList} → {@code isInBlackList}。 */
    private static String propertyName(String methodName, int prefix) {
        if (methodName.length() <= prefix) {
            return null;
        }
        char c = methodName.charAt(prefix);
        if (c == '_') {
            return methodName.length() > prefix + 1 ? methodName.substring(prefix + 1) : null;
        }
        if (Character.isUpperCase(c)) {
            return Character.toLowerCase(c) + methodName.substring(prefix + 1);
        }
        return null;
    }

    static Map<String, Object> toMap(Object bean) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Property property : info(bean.getClass()).readable.values()) {
            try {
                values.put(property.name, property.read(bean));
            } catch (ReflectiveOperationException e) {
                throw new NgaJsonException("无法读取属性 " + property.name, e);
            }
        }
        return values;
    }

    static <T> T bind(NgaJsonObject source, Class<T> type, boolean lenient) {
        T target = newInstance(type);
        BeanInfo info = info(type);
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Property property = find(info.writable, entry.getKey(), lenient);
            if (property == null) {
                if (lenient) {
                    fillReadOnlyContainer(target, info, entry);
                }
                continue;
            }
            Object value = convert(entry.getValue(), property.type, lenient);
            if (value == SKIP) {
                continue;
            }
            try {
                property.write(target, value);
            } catch (ReflectiveOperationException e) {
                throw new NgaJsonException("无法写入属性 " + property.name, e);
            }
        }
        return target;
    }

    /** 文本模式下，只有读取方法的集合属性按读取到的实例追加内容。 */
    @SuppressWarnings("unchecked")
    private static void fillReadOnlyContainer(Object target, BeanInfo info, Map.Entry<String, Object> entry) {
        Property property = find(info.readable, entry.getKey(), true);
        if (property == null || property.method == null) {
            return;
        }
        try {
            Object current = property.read(target);
            Object value = convert(entry.getValue(), property.type, true);
            if (current instanceof Collection && value instanceof Collection) {
                ((Collection<Object>) current).addAll((Collection<Object>) value);
            } else if (current instanceof Map && value instanceof Map) {
                ((Map<Object, Object>) current).putAll((Map<Object, Object>) value);
            }
        } catch (ReflectiveOperationException e) {
            throw new NgaJsonException("无法写入属性 " + property.name, e);
        }
    }

    private static Property find(Map<String, Property> properties, String key, boolean lenient) {
        Property exact = properties.get(key);
        if (exact != null || !lenient) {
            return exact;
        }
        for (Property property : properties.values()) {
            if (property.name.equalsIgnoreCase(key)) {
                return property;
            }
        }
        String normalized = normalize(key);
        for (Property property : properties.values()) {
            if (normalize(property.name).equals(normalized)) {
                return property;
            }
        }
        return null;
    }

    private static String normalize(String name) {
        return name.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NgaJsonException("无法创建 " + type.getName(), e);
        }
    }

    /** 转换为目标类型；文本模式下可能返回 {@link #SKIP}。 */
    static Object convert(Object value, Type type, boolean lenient) {
        Class<?> raw = rawClass(type);
        if (raw == Object.class) {
            return value;
        }
        if (raw == String.class) {
            return JsonCasts.toStringValue(value);
        }
        if (raw == NgaJsonObject.class) {
            return JsonCasts.toJsonObject(value);
        }
        if (raw == NgaJsonArray.class) {
            return JsonCasts.toJsonArray(value);
        }
        if (isScalar(raw)) {
            return lenient ? convertLexical(value, raw) : convertCast(value, raw);
        }
        if (value == null || "".equals(value)) {
            return null;
        }
        if (Map.class.isAssignableFrom(raw)) {
            if (!(value instanceof NgaJsonObject)) {
                throw JsonCasts.cannotCast(value, raw.getSimpleName());
            }
            Type valueType = typeArgument(type, 1);
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : ((NgaJsonObject) value).entrySet()) {
                result.put(entry.getKey(), element(entry.getValue(), valueType, lenient));
            }
            return result;
        }
        if (Collection.class.isAssignableFrom(raw)) {
            if (!(value instanceof NgaJsonArray)) {
                throw JsonCasts.cannotCast(value, raw.getSimpleName());
            }
            Type elementType = typeArgument(type, 0);
            Collection<Object> result = Set.class.isAssignableFrom(raw) ? new HashSet<>() : new ArrayList<>();
            for (Object item : (NgaJsonArray) value) {
                result.add(element(item, elementType, lenient));
            }
            return result;
        }
        if (raw.isEnum()) {
            return enumValue(raw, value.toString());
        }
        if (value instanceof NgaJsonObject) {
            return bind((NgaJsonObject) value, raw, lenient);
        }
        throw JsonCasts.cannotCast(value, raw.getSimpleName());
    }

    private static Object element(Object value, Type type, boolean lenient) {
        Object result = convert(value, type, lenient);
        return result == SKIP ? null : result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(Class<?> type, String name) {
        return Enum.valueOf((Class) type, name);
    }

    private static boolean isScalar(Class<?> raw) {
        return raw.isPrimitive()
                || raw == Integer.class
                || raw == Long.class
                || raw == Short.class
                || raw == Byte.class
                || raw == Boolean.class
                || raw == Float.class
                || raw == Double.class
                || raw == BigDecimal.class
                || raw == BigInteger.class;
    }

    /** 精确模式：沿用取值方法的转换，null 写入基本类型时失败。 */
    private static Object convertCast(Object value, Class<?> raw) {
        Object result;
        if (raw == int.class || raw == Integer.class) {
            result = JsonCasts.toInteger(value);
        } else if (raw == long.class || raw == Long.class) {
            result = JsonCasts.toLong(value);
        } else if (raw == short.class || raw == Short.class) {
            Integer number = JsonCasts.toInteger(value);
            result = number == null ? null : number.shortValue();
        } else if (raw == byte.class || raw == Byte.class) {
            Integer number = JsonCasts.toInteger(value);
            result = number == null ? null : number.byteValue();
        } else if (raw == boolean.class || raw == Boolean.class) {
            result = JsonCasts.toBoolean(value);
        } else if (raw == float.class || raw == Float.class) {
            result = JsonCasts.toFloat(value);
        } else if (raw == double.class || raw == Double.class) {
            result = JsonCasts.toDouble(value);
        } else {
            result = bigNumber(value, raw);
        }
        if (result == null && raw.isPrimitive()) {
            throw new NgaJsonException("无法将空值写入 " + raw.getName());
        }
        return result;
    }

    /** 文本模式：与 fastjson 词法读取一致，整数属性只接受未溢出的整数字面量或数字字符串。 */
    private static Object convertLexical(Object value, Class<?> raw) {
        if (value == null || "".equals(value)) {
            return raw.isPrimitive() ? SKIP : null;
        }
        if (raw == int.class || raw == Integer.class
                || raw == short.class || raw == Short.class
                || raw == byte.class || raw == Byte.class) {
            int number;
            if (value instanceof Integer) {
                number = (Integer) value;
            } else if (value instanceof Long
                    && (Long) value >= Integer.MIN_VALUE
                    && (Long) value <= Integer.MAX_VALUE) {
                number = ((Long) value).intValue();
            } else if (value instanceof String) {
                number = Integer.parseInt((String) value);
            } else {
                throw JsonCasts.cannotCast(value, raw.getSimpleName());
            }
            if (raw == short.class || raw == Short.class) {
                return (short) number;
            }
            if (raw == byte.class || raw == Byte.class) {
                return (byte) number;
            }
            return number;
        }
        if (raw == long.class || raw == Long.class) {
            if (value instanceof Integer || value instanceof Long) {
                return ((Number) value).longValue();
            }
            if (value instanceof String) {
                return Long.parseLong((String) value);
            }
            throw JsonCasts.cannotCast(value, raw.getSimpleName());
        }
        if (raw == boolean.class || raw == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof Integer || value instanceof Long) {
                return ((Number) value).intValue() == 1;
            }
            if (value instanceof String) {
                return JsonCasts.toBoolean(value);
            }
            throw JsonCasts.cannotCast(value, raw.getSimpleName());
        }
        if (raw == float.class || raw == Float.class) {
            return JsonCasts.toFloat(value);
        }
        if (raw == double.class || raw == Double.class) {
            return JsonCasts.toDouble(value);
        }
        return bigNumber(value, raw);
    }

    private static Object bigNumber(Object value, Class<?> raw) {
        if (value == null || "".equals(value)) {
            return null;
        }
        if (raw == BigDecimal.class) {
            return value instanceof BigDecimal ? value : new BigDecimal(value.toString());
        }
        if (value instanceof BigInteger) {
            return value;
        }
        if (value instanceof Number || value instanceof String) {
            return new BigInteger(value.toString());
        }
        throw JsonCasts.cannotCast(value, raw.getSimpleName());
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return rawClass(((ParameterizedType) type).getRawType());
        }
        if (type instanceof WildcardType) {
            return rawClass(((WildcardType) type).getUpperBounds()[0]);
        }
        return Object.class;
    }

    private static Type typeArgument(Type type, int index) {
        if (type instanceof ParameterizedType) {
            Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
            if (index < arguments.length) {
                return arguments[index];
            }
        }
        return Object.class;
    }

    static <T> List<T> bindList(NgaJsonArray array, Class<T> type) {
        List<T> result = new ArrayList<>(array.size());
        for (Object item : array) {
            result.add(type.cast(element(item, type, true)));
        }
        return result;
    }
}
