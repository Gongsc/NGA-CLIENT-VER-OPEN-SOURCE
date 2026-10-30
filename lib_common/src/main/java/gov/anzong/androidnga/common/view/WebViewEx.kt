package gov.anzong.androidnga.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.autofill.AutofillValue
import android.webkit.WebView

open class WebViewEx : WebView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
        super(context, attrs, defStyleAttr, defStyleRes)

    override fun autofill(value: AutofillValue) {
        try {
            super.autofill(value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
