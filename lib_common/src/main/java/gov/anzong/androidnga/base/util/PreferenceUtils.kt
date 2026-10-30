package gov.anzong.androidnga.base.util

import android.content.SharedPreferences
import gov.anzong.androidnga.base.json.NgaJson

/**
 * @author Justwen
 */
object PreferenceUtils {

    private val preferences: SharedPreferences = ContextUtils.getDefaultSharedPreferences()

    @JvmStatic
    fun putData(key: String, value: String?) = preferences.edit().putString(key, value).apply()

    @JvmStatic
    fun putData(key: String, value: Int) = preferences.edit().putInt(key, value).apply()

    @JvmStatic
    fun putData(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()

    @JvmStatic
    fun putData(key: String, value: Float) = preferences.edit().putFloat(key, value).apply()

    @JvmStatic
    fun putData(key: String, value: Long) = preferences.edit().putLong(key, value).apply()

    @JvmStatic
    fun putData(key: String, value: Set<String>?) = preferences.edit().putStringSet(key, value).apply()

    @JvmStatic
    fun putData(key: String, list: List<*>?) =
        preferences.edit().putString(key, NgaJson.toJSONString(list)).apply()

    @JvmStatic
    fun getData(key: String, defValue: String?): String? = preferences.getString(key, defValue)

    @JvmStatic
    fun getData(key: String, defValue: Boolean): Boolean = preferences.getBoolean(key, defValue)

    @JvmStatic
    fun getData(key: String, defValue: Float): Float = preferences.getFloat(key, defValue)

    @JvmStatic
    fun getData(key: String, defValue: Long): Long = preferences.getLong(key, defValue)

    @JvmStatic
    fun getData(key: String, defValue: Int): Int = preferences.getInt(key, defValue)

    @JvmStatic
    fun getData(key: String, defValue: Set<String>?): Set<String>? = preferences.getStringSet(key, defValue)

    /** 读取以 JSON 数组保存的列表；没有记录时返回空列表。 */
    @JvmStatic
    fun <T> getData(key: String, clz: Class<T>): MutableList<T> {
        val value = preferences.getString(key, null) ?: return ArrayList()
        return NgaJson.parseArray(value, clz) ?: ArrayList()
    }

    @JvmStatic
    fun edit(): SharedPreferences.Editor = preferences.edit()
}
