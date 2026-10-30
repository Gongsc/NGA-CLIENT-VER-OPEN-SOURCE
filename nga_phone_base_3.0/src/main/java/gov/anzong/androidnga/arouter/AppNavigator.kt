package gov.anzong.androidnga.arouter

import android.app.Activity
import gov.anzong.androidnga.activity.ArticleListActivity
import gov.anzong.androidnga.activity.LoginActivity
import gov.anzong.androidnga.activity.MessageListActivity
import gov.anzong.androidnga.activity.MessagePostActivity
import gov.anzong.androidnga.activity.PostActivity
import gov.anzong.androidnga.activity.ProfileActivity
import gov.anzong.androidnga.activity.RecentNotificationActivity
import gov.anzong.androidnga.activity.SearchActivity
import gov.anzong.androidnga.activity.TopicListActivity

/** Explicit Android navigation; route names preserve existing business call contracts. */
class AppNavigator private constructor() {

    fun build(path: String): NavigationRequest = NavigationRequest(path)

    companion object {

        private val INSTANCE = AppNavigator()

        @JvmStatic
        fun getInstance(): AppNavigator = INSTANCE

        @JvmStatic
        fun destination(path: String): Class<out Activity> = when (path) {
            ARouterConstants.ACTIVITY_PROFILE -> ProfileActivity::class.java
            ARouterConstants.ACTIVITY_LOGIN -> LoginActivity::class.java
            ARouterConstants.ACTIVITY_POST -> PostActivity::class.java
            ARouterConstants.ACTIVITY_MESSAGE_POST -> MessagePostActivity::class.java
            ARouterConstants.ACTIVITY_TOPIC_CONTENT -> ArticleListActivity::class.java
            ARouterConstants.ACTIVITY_TOPIC_LIST -> TopicListActivity::class.java
            ARouterConstants.ACTIVITY_SEARCH -> SearchActivity::class.java
            ARouterConstants.ACTIVITY_NOTIFICATION -> RecentNotificationActivity::class.java
            ARouterConstants.ACTIVITY_MESSAGE_LIST -> MessageListActivity::class.java
            else -> throw IllegalArgumentException("Unknown destination: $path")
        }

        @JvmStatic
        fun requiresLogin(path: String): Boolean = path in ARouterConstants.ACTIVITY_NEED_LOGIN
    }
}
