package gov.anzong.androidnga.ui.theme

import android.content.Context
import androidx.core.content.ContextCompat
import gov.anzong.androidnga.R

/**
 * Created by Justwen on 2018/9/5.
 */
class WebViewTheme(context: Context) {

    val webTextColor: Int = ContextCompat.getColor(context, R.color.web_text_color)

    val quoteBackgroundColor: Int = ContextCompat.getColor(context, R.color.web_quote_background_color)
}
