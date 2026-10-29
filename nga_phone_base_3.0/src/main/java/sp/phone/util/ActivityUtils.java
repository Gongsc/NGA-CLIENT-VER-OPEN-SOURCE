package sp.phone.util;

import android.content.Context;
import android.content.Intent;

import gov.anzong.androidnga.activity.TopicHistoryActivity;
import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.util.ToastUtils;
import sp.phone.common.PhoneConfiguration;
import sp.phone.common.UserManagerImpl;

public class ActivityUtils {

    private ActivityUtils() {
    }

    public static void showToast(Context context, int resId) {
        if (context != null) {
            ToastUtils.showToast(resId);
        }
    }

    public static void showToast(Context context, String res) {
        if (context != null) {
            ToastUtils.showToast(res);
        }
    }

    public static void showToast(String res) {
        showToast(ContextUtils.getContext(), res);
    }

    public static void showToast(int resId) {
        showToast(ContextUtils.getContext(), resId);
    }

    public static void startLoginActivity(Context context) {
        Intent intent = new Intent(context, PhoneConfiguration.getInstance().loginActivityClass);
        context.startActivity(intent);
    }

    public static void startFavoriteTopicActivity(Context context) {
        if (UserManagerImpl.getInstance().getActiveUser() == null) {
            startLoginActivity(context);
        } else {
            Intent intent = new Intent(context, PhoneConfiguration.getInstance().topicActivityClass);
            intent.putExtra("favor", 1);
            context.startActivity(intent);
        }
    }

    public static void startRecommendTopicActivity(Context context, Intent intent) {
        if (UserManagerImpl.getInstance().getActiveUser() == null) {
            startLoginActivity(context);
        } else {
            intent.setClass(context, PhoneConfiguration.getInstance().topicActivityClass);
            context.startActivity(intent);
        }
    }

    public static void startTwentyFourActivity(Context context, Intent intent) {
        if (UserManagerImpl.getInstance().getActiveUser() == null) {
            startLoginActivity(context);
        } else {
            intent.setClass(context, PhoneConfiguration.getInstance().topicActivityClass);
            context.startActivity(intent);
        }
    }

    public static void startHistoryTopicActivity(Context context) {
        context.startActivity(new Intent(context, TopicHistoryActivity.class));
    }
}
