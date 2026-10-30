package gov.anzong.androidnga.coroutines

/**
 * 页面间事件，经 [UiEventBus] 发送。
 * Created by Justwen on 2017/11/25.
 */
class UiEvent @JvmOverloads constructor(
    @JvmField val what: Int,
    @JvmField val arg: Int = 0,
    @JvmField val obj: Any? = null,
) {

    constructor(what: Int, obj: Any?) : this(what, 0, obj)

    companion object {

        const val EVENT_ARTICLE_GO_FLOOR = 3

        const val EVENT_SHOW_TOPIC_LIST = 6
    }
}
