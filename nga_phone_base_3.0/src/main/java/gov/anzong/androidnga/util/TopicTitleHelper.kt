package gov.anzong.androidnga.util

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Base64
import androidx.annotation.ColorRes
import gov.anzong.androidnga.R
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.network.ApiConstants
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.util.StringUtils
import java.math.BigInteger
import java.util.Locale

/** 主题标题的颜色、字体与附件、锁定、合集标记。 */
object TopicTitleHelper {

    private fun SpannableStringBuilder.span(what: Any, start: Int, end: Int) {
        setSpan(what, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun color(@ColorRes id: Int) = ForegroundColorSpan(ContextUtils.getColor(id))

    private fun handleOldFormat(builder: SpannableStringBuilder, misc: String, titleLength: Int) {
        if (misc == "~1~~" || misc == "~~~1") {
            builder.span(StyleSpan(Typeface.BOLD_ITALIC), 0, titleLength)
            return
        }
        for (item in misc.lowercase(Locale.US).split("~")) {
            when (item) {
                "green" -> builder.span(color(R.color.title_green), 0, titleLength)
                "blue" -> builder.span(color(R.color.title_blue), 0, titleLength)
                "red" -> builder.span(color(R.color.title_red), 0, titleLength)
                "orange" -> builder.span(color(R.color.title_orange), 0, titleLength)
                "sliver" -> builder.span(color(R.color.silver), 0, titleLength)
                "b" -> builder.span(StyleSpan(Typeface.BOLD), 0, titleLength)
                "i" -> builder.span(StyleSpan(Typeface.ITALIC), 0, titleLength)
                "u" -> builder.span(UnderlineSpan(), 0, titleLength)
            }
        }
    }

    private fun has(value: Int, mask: Int) = value and mask == mask

    private fun handleNewFormat(builder: SpannableStringBuilder, misc: String, titleLength: Int) {
        val bytes = Base64.decode(misc, Base64.DEFAULT) ?: return
        var pos = 0
        while (pos < bytes.size) {
            // 1 表示主题bit数据
            if (bytes[pos].toInt() == 1) {
                val miscValue = BigInteger(StringUtils.toBinaryArray(bytes).substring(8), 2).toInt()
                when {
                    has(miscValue, ApiConstants.MASK_FONT_GREEN) -> builder.span(color(R.color.title_green), 0, titleLength)
                    has(miscValue, ApiConstants.MASK_FONT_BLUE) -> builder.span(color(R.color.title_blue), 0, titleLength)
                    has(miscValue, ApiConstants.MASK_FONT_RED) -> builder.span(color(R.color.title_red), 0, titleLength)
                    has(miscValue, ApiConstants.MASK_FONT_ORANGE) -> builder.span(color(R.color.title_orange), 0, titleLength)
                    has(miscValue, ApiConstants.MASK_FONT_SILVER) -> builder.span(color(R.color.silver), 0, titleLength)
                }
                val bold = has(miscValue, ApiConstants.MASK_FONT_BOLD)
                val italic = has(miscValue, ApiConstants.MASK_FONT_ITALIC)
                when {
                    bold && italic -> builder.span(StyleSpan(Typeface.BOLD_ITALIC), 0, titleLength)
                    italic -> builder.span(StyleSpan(Typeface.ITALIC), 0, titleLength)
                    bold -> builder.span(StyleSpan(Typeface.BOLD), 0, titleLength)
                }
                if (has(miscValue, ApiConstants.MASK_FONT_UNDERLINE)) {
                    builder.span(UnderlineSpan(), 0, titleLength)
                }
            }
            pos += 4
        }
    }

    private fun SpannableStringBuilder.appendMark(text: String, @ColorRes colorId: Int) {
        append(text)
        span(color(colorId), length - text.length, length)
    }

    @JvmStatic
    fun handleTitleFormat(entry: ThreadPageInfo): CharSequence {
        val title = StringUtils.removeBrTag(StringUtils.unEscapeHtml(entry.subject))
        val builder = SpannableStringBuilder(title)
        val type = entry.type
        val titleLength = title.length
        if (has(type, ApiConstants.MASK_TYPE_ATTACHMENT)) builder.appendMark(" +", R.color.title_orange)
        if (has(type, ApiConstants.MASK_TYPE_LOCK)) builder.appendMark(" [锁定]", R.color.title_red)
        if (has(type, ApiConstants.MASK_TYPE_ASSEMBLE)) builder.appendMark(" [合集]", R.color.title_blue)
        val misc = entry.topicMisc
        if (!misc.isNullOrEmpty()) {
            // ~ 开头的为旧格式
            if (misc.startsWith("~")) handleOldFormat(builder, misc, titleLength) else handleNewFormat(builder, misc, titleLength)
        }
        val board = entry.board
        if (!board.isNullOrEmpty()) {
            builder.appendMark("  [$board]", R.color.text_color_disabled)
        }
        return builder
    }
}
