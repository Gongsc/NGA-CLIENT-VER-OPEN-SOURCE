package gov.anzong.androidnga.settings

import android.content.Context

interface FilterKeywordsManager {

    fun initialize(context: Context)

    fun toggleKeyword(position: Int)

    fun addKeyword(keyword: FilterKeyword)

    val keywords: List<FilterKeyword>

    fun removeKeyword(index: Int)
}
