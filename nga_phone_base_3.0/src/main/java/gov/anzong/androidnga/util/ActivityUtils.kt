package gov.anzong.androidnga.util

import android.content.Context
import android.content.Intent
import gov.anzong.androidnga.activity.TopicHistoryActivity
import gov.anzong.androidnga.base.util.ToastUtils
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.account.UserManagerImpl

object ActivityUtils {

    @JvmStatic
    fun showToast(text: String?) = ToastUtils.info(text)

    @JvmStatic
    fun startLoginActivity(context: Context) {
        context.startActivity(Intent(context, PhoneConfiguration.getInstance().loginActivityClass))
    }

    @JvmStatic
    fun startFavoriteTopicActivity(context: Context) {
        startTopicListWhenLoggedIn(context, Intent().putExtra("favor", 1))
    }

    @JvmStatic
    fun startRecommendTopicActivity(context: Context, intent: Intent) = startTopicListWhenLoggedIn(context, intent)

    @JvmStatic
    fun startTwentyFourActivity(context: Context, intent: Intent) = startTopicListWhenLoggedIn(context, intent)

    /** 收藏、精华与热帖需要登录，未登录时打开登录页。 */
    private fun startTopicListWhenLoggedIn(context: Context, intent: Intent) {
        if (UserManagerImpl.getInstance().activeUser == null) {
            startLoginActivity(context)
        } else {
            intent.setClass(context, PhoneConfiguration.getInstance().topicActivityClass)
            context.startActivity(intent)
        }
    }

    @JvmStatic
    fun startHistoryTopicActivity(context: Context) {
        context.startActivity(Intent(context, TopicHistoryActivity::class.java))
    }
}
