package gov.anzong.androidnga.ui.compose

import gov.anzong.androidnga.network.bean.ThreadRowInfo

data class ArticleEntry(val row: ThreadRowInfo?, val hot: Boolean = false) {
    val key: String get() = row?.let { "${if (hot) "hot" else "floor"}:${it.tid}:${it.pid}:${it.lou}" } ?: "hot_header"
}

/** 热点区只插入实际显示的主楼之后，正常楼层顺序和定位保持独立。 */
fun articleEntries(rows: List<ThreadRowInfo>, hotReplies: List<ThreadRowInfo>): List<ArticleEntry> = buildList {
    val hot = hotReplies.filter { it.pid > 0 }.distinctBy { it.pid }
    rows.forEach { row ->
        add(ArticleEntry(row))
        if (row.lou == 0 && row.pid == 0 && hot.isNotEmpty()) {
            add(ArticleEntry(null, hot = true))
            hot.forEach { add(ArticleEntry(it, hot = true)) }
        }
    }
}
