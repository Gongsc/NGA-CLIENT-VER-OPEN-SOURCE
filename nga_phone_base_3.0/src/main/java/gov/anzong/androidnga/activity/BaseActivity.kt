package gov.anzong.androidnga.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import gov.anzong.androidnga.ui.compose.applyLegacyWindowInsets
import gov.anzong.androidnga.ui.compose.configureModernWindow
import gov.anzong.androidnga.notification.NotificationController
import gov.anzong.androidnga.settings.PhoneConfiguration
import gov.anzong.androidnga.ui.theme.ThemeManager

/**
 * 页面基类：主题、边到边窗口、返回处理与回到前台时的提醒检查。
 * Created by liuboyu on 16/6/28.
 */
abstract class BaseActivity : AppCompatActivity() {

    protected val mConfig: PhoneConfiguration
        get() = PhoneConfiguration.getInstance()

    private var toolbarEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
        setTheme(ThemeManager.getInstance().getTheme(toolbarEnabled))
        super.onCreate(savedInstanceState)
        configureModernWindow(this)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!handleBackNavigation()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
        ThemeManager.getInstance().initializeWebTheme(this)
    }

    /** 返回 true 表示页面已处理返回操作。 */
    protected open fun handleBackNavigation(): Boolean = false

    /** Compose 页面自行处理 Insets。 */
    protected open fun usesComposeContent(): Boolean = false

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (!usesComposeContent()) {
            applyLegacyWindowInsets(this)
        }
    }

    protected fun setToolbarEnabled(enabled: Boolean) {
        toolbarEnabled = enabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean = try {
        super.dispatchTouchEvent(ev)
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean = try {
        super.dispatchKeyEvent(event)
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    override fun onResume() {
        NotificationController.checkNotificationDelay()
        super.onResume()
    }

    /** startActivity 最终经过这里；设备上没有能处理的应用时忽略。 */
    @Deprecated("Deprecated in Java")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        try {
            @Suppress("DEPRECATION")
            super.startActivityForResult(intent, requestCode, options)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
}
