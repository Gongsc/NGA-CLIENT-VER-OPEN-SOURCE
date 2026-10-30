package gov.anzong.androidnga.activity

import android.os.Bundle
import gov.anzong.androidnga.ui.compose.installSearchContent

class SearchActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setToolbarEnabled(true)
        super.onCreate(savedInstanceState)
        installSearchContent(this)
    }

    override fun usesComposeContent(): Boolean = true
}
