package gov.anzong.androidnga.ui.compose

import android.text.Html
import gov.anzong.androidnga.network.bean.ThreadRowInfo
import gov.anzong.androidnga.settings.PhoneConfiguration

/** 仅将无交互、无附加内容的文字楼层交给原生文本排版。 */
fun canRenderPlainArticle(content: String): Boolean {
    if (content.contains('[') || content.contains(Regex("(?i)https?://"))) return false
    return !content.replace(Regex("(?i)</?p\\s*>|<br\\s*/?>"), "").contains('<')
}

fun plainArticleText(row: ThreadRowInfo): String? {
    val content = row.content ?: return null
    if (content.isBlank() || row.isInBlackList || !row.comments.isNullOrEmpty() || !row.attachs.isNullOrEmpty()
        || !row.vote.isNullOrEmpty() || !row.alterinfo.isNullOrEmpty()
        || (PhoneConfiguration.getInstance().isShowSignature && !row.signature.isNullOrEmpty())
        || !canRenderPlainArticle(content) || !canRenderPlainArticle(row.subject.orEmpty())) return null
    val text = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString().trimEnd('\n')
    val subject = row.subject.orEmpty()
    return if (subject.isEmpty()) text else Html.fromHtml(subject, Html.FROM_HTML_MODE_LEGACY).toString() + "\n\n" + text
}
