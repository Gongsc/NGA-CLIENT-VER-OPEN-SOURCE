package gov.anzong.androidnga.settings

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.common.PreferenceKey

/** 关键词屏蔽列表，以 JSON 保存在默认设置中。 */
class FilterKeywordsManagerImpl private constructor() : FilterKeywordsManager {

    private lateinit var prefs: SharedPreferences

    private var filterKeywords: MutableList<FilterKeyword> = ArrayList()

    override fun initialize(context: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val saved = prefs.getString(PreferenceKey.FILTER_KEYWORDS_LIST, "")
        filterKeywords = if (saved.isNullOrEmpty()) ArrayList() else NgaJson.parseArray(saved, FilterKeyword::class.java) ?: ArrayList()
    }

    override fun toggleKeyword(position: Int) {
        val keyword = filterKeywords[position]
        keyword.isEnabled = !keyword.isEnabled
        commit()
    }

    override fun addKeyword(keyword: FilterKeyword) {
        filterKeywords.add(keyword)
        commit()
    }

    override val keywords: List<FilterKeyword>
        get() = filterKeywords

    override fun removeKeyword(index: Int) {
        filterKeywords.removeAt(index)
        commit()
    }

    private fun commit() {
        prefs.edit { putString(PreferenceKey.FILTER_KEYWORDS_LIST, NgaJson.toJSONString(filterKeywords)) }
    }

    companion object {

        private val INSTANCE = FilterKeywordsManagerImpl()

        @JvmStatic
        fun getInstance(): FilterKeywordsManagerImpl = INSTANCE
    }
}
