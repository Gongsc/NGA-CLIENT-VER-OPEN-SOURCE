package gov.anzong.androidnga.ui.compose
import gov.anzong.androidnga.R
import org.junit.Assert.*
import org.junit.Test
class HomeNavigationTest {
    @Test fun personalAndAppActionsAreGroupedOnceInStableOrder() {
        val actions=listOf(R.id.menu_about,R.id.menu_msg,R.id.menu_appearance,R.id.menu_post,R.id.menu_setting,R.id.menu_forward,R.id.menu_login,R.id.menu_appearance).map{HomeAction(it,"入口")}
        val groups=homeNavigationGroups(actions)
        assertEquals(listOf("账号与消息","论坛工具","应用"),groups.map{it.title})
        assertEquals(listOf(R.id.menu_post,R.id.menu_msg),groups[0].actions.map{it.id})
        assertEquals(listOf(R.id.menu_appearance,R.id.menu_setting,R.id.menu_about),groups[2].actions.map{it.id})
        assertEquals(6,groups.sumOf{it.actions.size})
    }
}
