package gov.anzong.androidnga.arouter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {

    @Test
    fun loginGuardPreservesProtectedDestinations() {
        for (path in ARouterConstants.ACTIVITY_NEED_LOGIN) assertTrue(AppNavigator.requiresLogin(path))
        assertFalse(AppNavigator.requiresLogin(ARouterConstants.ACTIVITY_LOGIN))
    }

    @Test
    fun everyPublicRouteHasAnExplicitDestination() {
        val routes = listOf(
            ARouterConstants.ACTIVITY_PROFILE, ARouterConstants.ACTIVITY_LOGIN,
            ARouterConstants.ACTIVITY_POST, ARouterConstants.ACTIVITY_MESSAGE_POST,
            ARouterConstants.ACTIVITY_TOPIC_CONTENT, ARouterConstants.ACTIVITY_TOPIC_LIST,
            ARouterConstants.ACTIVITY_SEARCH, ARouterConstants.ACTIVITY_NOTIFICATION,
            ARouterConstants.ACTIVITY_MESSAGE_LIST,
        )
        for (path in routes) assertNotNull(AppNavigator.destination(path))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownRouteFailsExplicitly() {
        AppNavigator.destination("/missing")
    }
}
