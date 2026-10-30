package gov.anzong.androidnga.arouter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import gov.anzong.androidnga.activity.LoginActivity
import gov.anzong.androidnga.base.util.ContextUtils
import gov.anzong.androidnga.account.UserManagerImpl

class NavigationRequest internal constructor(private val path: String) {

    private val extras = Bundle()

    fun withInt(key: String, value: Int): NavigationRequest = apply { extras.putInt(key, value) }

    fun withString(key: String, value: String?): NavigationRequest = apply { extras.putString(key, value) }

    fun withParcelable(key: String, value: Parcelable?): NavigationRequest = apply { extras.putParcelable(key, value) }

    @JvmOverloads
    fun navigation(context: Context? = null) {
        val target = context ?: ContextUtils.getContext()
        val intent = intent(target)
        if (target !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        target.startActivity(intent)
    }

    /** 目标页面的 Intent，供 `registerForActivityResult` 的启动器使用；需要登录时指向登录页。 */
    fun intent(context: Context): Intent {
        val needsLogin = AppNavigator.requiresLogin(path) && UserManagerImpl.getInstance().activeUser == null
        val intent = Intent(context, if (needsLogin) LoginActivity::class.java else AppNavigator.destination(path))
        if (!needsLogin) intent.putExtras(extras)
        return intent
    }
}
