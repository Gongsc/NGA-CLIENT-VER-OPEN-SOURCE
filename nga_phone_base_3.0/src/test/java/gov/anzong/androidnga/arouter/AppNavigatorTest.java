package gov.anzong.androidnga.arouter;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppNavigatorTest {
    @Test public void loginGuardPreservesProtectedDestinations() {
        for (String path : ARouterConstants.ACTIVITY_NEED_LOGIN) assertTrue(AppNavigator.requiresLogin(path));
        assertFalse(AppNavigator.requiresLogin(ARouterConstants.ACTIVITY_LOGIN));
    }

    @Test public void everyPublicRouteHasAnExplicitDestination() {
        String[] routes = {ARouterConstants.ACTIVITY_PROFILE, ARouterConstants.ACTIVITY_LOGIN,
            ARouterConstants.ACTIVITY_POST, ARouterConstants.ACTIVITY_MESSAGE_POST,
            ARouterConstants.ACTIVITY_TOPIC_CONTENT, ARouterConstants.ACTIVITY_TOPIC_LIST,
            ARouterConstants.ACTIVITY_SEARCH, ARouterConstants.ACTIVITY_NOTIFICATION,
            ARouterConstants.ACTIVITY_MESSAGE_LIST};
        for (String path : routes) assertNotNull(AppNavigator.destination(path));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownRouteFailsExplicitly() { AppNavigator.destination("/missing"); }
}
