package gov.anzong.androidnga.mvvm.model

import android.preference.PreferenceManager
import androidx.core.content.edit
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo

/**
 * 浏览历史，最近浏览的在前，最多保留 [MAX_HISTORY_TOPIC_COUNT] 条。
 * Created by Justwen on 2018/1/17.
 */
class TopicHistoryManager private constructor() {

    private val context = ContextUtils.getContext()

    private val topicList: MutableList<ThreadPageInfo> =
        PreferenceManager.getDefaultSharedPreferences(context).getString(PreferenceKey.KEY_TOPIC_HISTORY, null)
            ?.let { NgaJson.parseArray(it, ThreadPageInfo::class.java) }
            ?: ArrayList()

    val topicHistoryList: List<ThreadPageInfo>
        get() = topicList

    fun addTopicHistory(topic: ThreadPageInfo) {
        if (topicList.contains(topic)) {
            topicList.remove(topic)
        } else if (topicList.size >= MAX_HISTORY_TOPIC_COUNT) {
            topicList.removeAt(topicList.size - 1)
        }
        topic.historyVisitedAt = System.currentTimeMillis()
        topicList.add(0, topic)
        commit()
    }

    fun removeTopicHistory(topic: ThreadPageInfo) {
        if (topicList.remove(topic)) {
            commit()
        }
    }

    fun removeTopicHistory(index: Int) {
        topicList.removeAt(index)
        commit()
    }

    fun removeAllTopicHistory() {
        topicList.clear()
        commit()
    }

    private fun commit() {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(PreferenceKey.KEY_TOPIC_HISTORY, NgaJson.toJSONString(topicList))
        }
    }

    companion object {

        private const val MAX_HISTORY_TOPIC_COUNT = 40

        private val INSTANCE by lazy { TopicHistoryManager() }

        @JvmStatic
        fun getInstance(): TopicHistoryManager = INSTANCE
    }
}
