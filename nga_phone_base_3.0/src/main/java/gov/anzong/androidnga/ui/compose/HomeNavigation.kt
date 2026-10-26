package gov.anzong.androidnga.ui.compose

import gov.anzong.androidnga.R

/** Drawer tiers: account entries sit in a tinted card, tools in a plain list, app entries in a bottom grid. */
enum class HomeNavigationTier { ACCOUNT, TOOLS, APP }

data class HomeNavigationGroup(val tier: HomeNavigationTier, val title: String, val actions: List<HomeAction>)

data class HomeAccount(val uid: String, val name: String, val avatarUrl: String? = null)

private val messageIds = listOf(R.id.menu_gun, R.id.menu_msg)
private val contentIds = listOf(R.id.menu_post, R.id.menu_reply, R.id.menu_bookmark, R.id.menu_download)
private val toolIds = listOf(R.id.menu_add, R.id.menu_add_id, R.id.menu_forward, R.id.menu_search)
private val appIds = listOf(R.id.menu_appearance, R.id.menu_setting, R.id.menu_about)

/** Entries that work without an account; the rest of the account tier is folded into a login hint. */
private val offlineAccountIds = listOf(R.id.menu_download)

fun homeNavigationGroups(actions: List<HomeAction>, loggedIn: Boolean = true): List<HomeNavigationGroup> {
    val unique = actions.distinctBy { it.id }.filter { it.id != R.id.menu_login }
    fun ordered(ids: List<Int>) = ids.mapNotNull { id -> unique.firstOrNull { it.id == id } }.map {
        if (it.id == R.id.menu_gun) it.copy(label = "回复通知") else it
    }
    val known = messageIds + contentIds + toolIds + appIds
    val account = if (loggedIn) listOf(
        HomeNavigationGroup(HomeNavigationTier.ACCOUNT, "消息", ordered(messageIds)),
        HomeNavigationGroup(HomeNavigationTier.ACCOUNT, "我的内容", ordered(contentIds))
    ) else listOf(HomeNavigationGroup(HomeNavigationTier.ACCOUNT, "", ordered(offlineAccountIds)))
    return (account + listOf(
        HomeNavigationGroup(HomeNavigationTier.TOOLS, "论坛工具", ordered(toolIds) + unique.filter { it.id !in known }),
        HomeNavigationGroup(HomeNavigationTier.APP, "应用", ordered(appIds))
    )).filter { it.actions.isNotEmpty() }
}

fun accountInitial(name: String): String = name.trim().firstOrNull()?.toString()?.uppercase() ?: "?"
