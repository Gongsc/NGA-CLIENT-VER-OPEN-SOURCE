package gov.anzong.androidnga.util

import android.os.SystemClock
import android.view.View

/** 防止连续点击：首次点击立即响应，之后 [windowMillis] 内的点击被忽略。替代原 RxView.clicks().throttleFirst(1s)。 */
class ThrottledClickListener @JvmOverloads constructor(
    private val listener: View.OnClickListener,
    private val windowMillis: Long = 1000,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : View.OnClickListener {

    private var lastClick = Long.MIN_VALUE

    override fun onClick(view: View?) {
        val now = clock()
        if (lastClick != Long.MIN_VALUE && now - lastClick < windowMillis) return
        lastClick = now
        listener.onClick(view)
    }

    companion object {
        @JvmStatic
        fun clicks(view: View, listener: View.OnClickListener) {
            view.setOnClickListener(ThrottledClickListener(listener))
        }
    }
}
