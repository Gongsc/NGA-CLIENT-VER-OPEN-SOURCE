package gov.anzong.androidnga.ui.compose
import gov.anzong.androidnga.R
import org.junit.Assert.*
import org.junit.Test
class HomeNavigationTest {
    private val all=listOf(R.id.menu_about,R.id.menu_msg,R.id.menu_appearance,R.id.menu_post,R.id.menu_setting,R.id.menu_forward,R.id.menu_login,
        R.id.menu_appearance,R.id.menu_gun,R.id.menu_download).map{HomeAction(it,"入口")}

    @Test fun accountToolAndAppTiersAreGroupedOnceInStableOrder() {
        val groups=homeNavigationGroups(all)
        assertEquals(listOf("消息","我的内容","论坛工具","应用"),groups.map{it.title})
        assertEquals(listOf(HomeNavigationTier.ACCOUNT,HomeNavigationTier.ACCOUNT,HomeNavigationTier.TOOLS,HomeNavigationTier.APP),groups.map{it.tier})
        assertEquals(listOf(R.id.menu_gun,R.id.menu_msg),groups[0].actions.map{it.id})
        assertEquals("回复通知",groups[0].actions[0].label)
        assertEquals(listOf(R.id.menu_post,R.id.menu_download),groups[1].actions.map{it.id})
        assertEquals(listOf(R.id.menu_appearance,R.id.menu_setting,R.id.menu_about),groups[3].actions.map{it.id})
        assertEquals(8,groups.sumOf{it.actions.size})
    }

    @Test fun loggedOutAccountTierKeepsOnlyOfflineEntries() {
        val groups=homeNavigationGroups(all,loggedIn=false)
        val account=groups.filter{it.tier==HomeNavigationTier.ACCOUNT}
        assertEquals(listOf(listOf(R.id.menu_download)),account.map{g->g.actions.map{it.id}})
        assertEquals(listOf(R.id.menu_forward),groups.single{it.tier==HomeNavigationTier.TOOLS}.actions.map{it.id})
        assertFalse(groups.flatMap{it.actions}.any{it.id==R.id.menu_login})
    }

    @Test fun accountInitialFallsBackForBlankNames() {
        assertEquals("喵",accountInitial(" 喵喵 "))
        assertEquals("A",accountInitial("alice"))
        assertEquals("?",accountInitial(""))
    }
}
