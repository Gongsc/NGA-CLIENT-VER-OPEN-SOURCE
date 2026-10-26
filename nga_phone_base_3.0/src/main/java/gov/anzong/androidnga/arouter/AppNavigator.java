package gov.anzong.androidnga.arouter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import gov.anzong.androidnga.activity.*;
import gov.anzong.androidnga.base.util.ContextUtils;
import sp.phone.common.UserManagerImpl;

/** Explicit Android navigation; route names preserve existing business call contracts. */
public final class AppNavigator {
    private static final AppNavigator INSTANCE = new AppNavigator();
    private AppNavigator() {}
    public static AppNavigator getInstance() { return INSTANCE; }
    public NavigationRequest build(String path) { return new NavigationRequest(path); }

    static Class<? extends Activity> destination(String path) {
        switch (path) {
            case ARouterConstants.ACTIVITY_PROFILE: return ProfileActivity.class;
            case ARouterConstants.ACTIVITY_LOGIN: return LoginActivity.class;
            case ARouterConstants.ACTIVITY_POST: return PostActivity.class;
            case ARouterConstants.ACTIVITY_MESSAGE_POST: return MessagePostActivity.class;
            case ARouterConstants.ACTIVITY_TOPIC_CONTENT: return ArticleListActivity.class;
            case ARouterConstants.ACTIVITY_TOPIC_LIST: return TopicListActivity.class;
            case ARouterConstants.ACTIVITY_SEARCH: return SearchActivity.class;
            case ARouterConstants.ACTIVITY_NOTIFICATION: return RecentNotificationActivity.class;
            case ARouterConstants.ACTIVITY_MESSAGE_LIST: return MessageListActivity.class;
            default: throw new IllegalArgumentException("Unknown destination: " + path);
        }
    }

    static boolean requiresLogin(String path) {
        for (String protectedPath : ARouterConstants.ACTIVITY_NEED_LOGIN) {
            if (protectedPath.equals(path)) return true;
        }
        return false;
    }
}
