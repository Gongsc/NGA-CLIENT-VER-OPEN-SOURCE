package gov.anzong.androidnga.arouter

/**
 * 页面路由名称，由 [AppNavigator.destination] 映射到 Activity。
 * Created by Justwen on 2018/4/20.
 */
object ARouterConstants {

    const val ACTIVITY_PROFILE = "/activity/profile"

    const val ACTIVITY_LOGIN = "/activity/login"

    const val ACTIVITY_POST = "/activity/post"

    const val ACTIVITY_MESSAGE_POST = "/activity/message_post"

    const val ACTIVITY_TOPIC_CONTENT = "/activity/topic_content"

    const val ACTIVITY_TOPIC_LIST = "/activity/topic_list"

    const val ACTIVITY_SEARCH = "/activity/search"

    const val ACTIVITY_NOTIFICATION = "/activity/notification"

    const val ACTIVITY_MESSAGE_LIST = "/activity/message_list"

    /** 未登录时打开登录页的路由。 */
    @JvmField
    val ACTIVITY_NEED_LOGIN = arrayOf(
        ACTIVITY_MESSAGE_LIST,
        ACTIVITY_PROFILE,
        ACTIVITY_NOTIFICATION,
        ACTIVITY_SEARCH,
        ACTIVITY_TOPIC_LIST,
    )
}
