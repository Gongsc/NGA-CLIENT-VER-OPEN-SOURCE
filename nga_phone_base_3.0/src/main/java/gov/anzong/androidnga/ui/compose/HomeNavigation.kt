package gov.anzong.androidnga.ui.compose

import gov.anzong.androidnga.R

data class HomeNavigationGroup(val title: String, val actions: List<HomeAction>)

fun homeNavigationGroups(actions: List<HomeAction>): List<HomeNavigationGroup> {
    val unique = actions.distinctBy { it.id }.filter { it.id != R.id.menu_login }
    val account = listOf(R.id.menu_post,R.id.menu_reply,R.id.menu_msg,R.id.menu_gun,R.id.menu_bookmark,R.id.menu_download)
    val tools = listOf(R.id.menu_add,R.id.menu_add_id,R.id.menu_forward,R.id.menu_search)
    val app = listOf(R.id.menu_appearance,R.id.menu_setting,R.id.menu_about)
    fun ordered(ids: List<Int>) = ids.mapNotNull { id -> unique.firstOrNull { it.id == id } }.map {
        if(it.id == R.id.menu_gun) it.copy(label="回复通知") else it
    }
    return listOf(
        HomeNavigationGroup("账号与消息",ordered(account)),
        HomeNavigationGroup("论坛工具",ordered(tools)+unique.filter{it.id !in account+tools+app}),
        HomeNavigationGroup("应用",ordered(app))
    ).filter{it.actions.isNotEmpty()}
}
