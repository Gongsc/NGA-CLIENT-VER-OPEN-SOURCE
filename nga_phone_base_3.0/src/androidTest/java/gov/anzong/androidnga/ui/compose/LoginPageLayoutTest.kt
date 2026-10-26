package gov.anzong.androidnga.ui.compose

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import sp.phone.view.webview.LoginWebView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class LoginPageLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun embeddedLoginFormHasVisibleHeightAndFitsResizedViewport() {
        val height = mutableStateOf(360.dp)
        val loaded = AtomicBoolean(false)
        lateinit var web: LoginWebView
        compose.setContent {
            Box(Modifier.height(height.value)) {
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
                    LoginWebView(context).apply {
                        web = this
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                fitLoginFrame()
                                loaded.set(true)
                            }
                        }
                        loadDataWithBaseURL("https://ngabbs.com/nuke.php?__lib=login&__act=account&login",
                            """<!doctype html><html style="height:0px"><body style="height:100%"><iframe id="iff" style="height:99.5%;border:0" srcdoc="<form><input name='username'><input type='password'></form>"></iframe></body></html>""",
                            "text/html", "UTF-8", "https://ngabbs.com/nuke.php?__lib=login&__act=account&login")
                    }
                }, onRelease = { it.destroy() })
            }
        }
        compose.waitUntil(10000) { loaded.get() }
        fun snapshot(): JSONObject {
            val value = AtomicReference<String?>()
            compose.runOnIdle {
                web.evaluateJavascript("""(function(){var f=document.getElementById('iff');return JSON.stringify({height:f.clientHeight,viewport:innerHeight,inputs:f.contentDocument.querySelectorAll('input').length})})()""") { value.set(it) }
            }
            compose.waitUntil(5000) { value.get() != null }
            return JSONObject(JSONTokener(value.get()!!).nextValue() as String)
        }
        val initial = snapshot()
        assertTrue(initial.getInt("height") > 100)
        assertEquals(initial.getInt("viewport"), initial.getInt("height"))
        assertEquals(2, initial.getInt("inputs"))
        compose.runOnIdle { height.value = 220.dp }
        compose.waitForIdle()
        val resized = snapshot()
        assertTrue(resized.getInt("height") > 100)
        assertTrue(resized.getInt("height") < initial.getInt("height"))
        assertEquals(resized.getInt("viewport"), resized.getInt("height"))
    }
}
