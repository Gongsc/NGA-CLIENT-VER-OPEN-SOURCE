package gov.anzong.androidnga.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import gov.anzong.androidnga.R
import gov.anzong.androidnga.account.UserManagerImpl
import gov.anzong.androidnga.activity.MessageListActivity
import gov.anzong.androidnga.activity.RecentNotificationActivity
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.common.PreferenceKey
import gov.anzong.androidnga.http.OnHttpCallBack
import gov.anzong.androidnga.mvvm.model.entity.NotificationInfo
import gov.anzong.androidnga.mvvm.model.entity.RecentReplyInfo
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.task.ForumNotificationTask

object NotificationController {

    private const val NOTIFICATION_ID = "NGA"

    private const val NOTIFICATION_NAME = "NGA_CLIENT"

    private const val DELAY_TIME = 30 * 1000

    private val notificationTask = ForumNotificationTask(null)

    private val configuration = PhoneConfiguration.getInstance()

    private var lastQueryTime = 0L

    init {
        createNotificationChannel(ContextUtils.getContext())
    }

    fun checkNotificationDelay() {
        if (configuration.isNotificationEnabled
            && UserManagerImpl.getInstance().hasValidUser()
            && System.currentTimeMillis() - lastQueryTime > DELAY_TIME
        ) {
            lastQueryTime = System.currentTimeMillis()
            notificationTask.queryNotification(object : OnHttpCallBack<List<NotificationInfo>> {
                override fun onError(text: String?) {}

                override fun onSuccess(data: List<NotificationInfo>) {
                    showNotification(data)
                }
            })
        }
    }

    private fun showNotification(infoList: List<NotificationInfo>) {
        val context = ContextUtils.getContext()
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return
        }
        if (infoList.isEmpty() || !infoList[0].isUnread) {
            return
        }
        val recentReplyList = ArrayList(infoList.filterIsInstance<RecentReplyInfo>())
        if (recentReplyList.size < infoList.size) {
            showMessageNotification(context)
        }
        if (recentReplyList.isNotEmpty()) {
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putInt(PreferenceKey.KEY_REPLY_COUNT, recentReplyList.size)
            }
            showReplyNotification(context, recentReplyList)
        }
    }

    private fun showReplyNotification(context: Context, infoList: ArrayList<RecentReplyInfo>) {
        val intent = Intent(context, RecentNotificationActivity::class.java)
        intent.putParcelableArrayListExtra("unread", infoList)
        notify(context, infoList[0].pidStr.toInt(), intent, "有新的被喷提醒，请查看")
    }

    private fun showMessageNotification(context: Context) {
        notify(context, 1000, Intent(context, MessageListActivity::class.java), "有新的短消息，请查看")
    }

    private fun notify(context: Context, id: Int, intent: Intent, text: String) {
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, NOTIFICATION_ID)
            .setLights("#fff0cd".toColorInt(), 2333, 0)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setWhen(System.currentTimeMillis())
            // 设置默认的提示音，振动方式，灯光
            .setDefaults(
                if (configuration.isNotificationSoundEnabled) Notification.DEFAULT_ALL else Notification.DEFAULT_LIGHTS
            )
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setTicker(text)
            .setContentTitle(text)
            .setContentText(text)
        context.getSystemService(NotificationManager::class.java)?.notify(id, builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(NOTIFICATION_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_HIGH)
        channel.enableLights(true)
        channel.lightColor = Color.GREEN
        channel.setShowBadge(true)
        notificationManager.createNotificationChannel(channel)
    }
}
