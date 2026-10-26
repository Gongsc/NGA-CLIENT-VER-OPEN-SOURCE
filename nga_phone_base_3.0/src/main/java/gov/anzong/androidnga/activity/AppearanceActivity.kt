package gov.anzong.androidnga.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import gov.anzong.androidnga.ui.compose.AppearanceScreen
import gov.anzong.androidnga.ui.compose.NgaTheme

class AppearanceActivity : BaseActivity() {
    override fun usesComposeContent() = true
    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        setContent { NgaTheme { AppearanceScreen { finish() } } }
    }
}
