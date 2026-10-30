package gov.anzong.androidnga.ui.compose

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import org.json.JSONTokener
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.network.bean.ThreadRowInfo
import gov.anzong.androidnga.settings.PhoneConfiguration
import java.util.concurrent.atomic.AtomicReference

class ArticleTypographyTest {
    @get:Rule val compose=createComposeRule()
    @Test fun nativeAndHtmlFontsMatchAcrossReadingZoomAndSystemScaleWithInsetCards() {
        val config=PhoneConfiguration.getInstance()
        val original=config.webViewTextZoom
        val scale=mutableFloatStateOf(1f)
        val root=AtomicReference<View>()
        val state=ArticlePageState().apply {
            loading=false
            rows=listOf(ThreadRowInfo().apply {tid=12;pid=0;lou=0;author="主楼";content="同一段正文"},
                ThreadRowInfo().apply {tid=12;pid=1;lou=1;author="富文本";content="[b]同一段正文[/b]";
                    formattedHtmlData="<html><head></head><body><b id='font_probe'>同一段正文</b></body></html>"})
        }
        compose.setContent {
            root.set(LocalView.current)
            val density=LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density,scale.floatValue)) {
                NgaTheme {ArticlePageScreen(state,{},{},{},{},{emptyList()},{_,_->})}
            }
        }
        fun find(view:View): ArticleContentWebView? {
            if(view is ArticleContentWebView)return view
            if(view is ViewGroup)for(i in 0 until view.childCount)find(view.getChildAt(i))?.let{return it}
            return null
        }
        try {
            for(fontScale in listOf(1f,1.5f,2f)) for(zoom in listOf(70,90)) {
                compose.runOnIdle {scale.floatValue=fontScale;config.webViewTextZoom=zoom}
                compose.onNodeWithTag("article_floors").performScrollToNode(hasTestTag("floor_0"))
                val layouts=mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("article_native_text").performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
                assertEquals(articleFontSize(config.topicContentSize,zoom),layouts.single().layoutInput.style.fontSize.value,0.01f)
                compose.onNodeWithTag("article_floors").performScrollToNode(hasTestTag("floor_1"))
                val fontDensity=Density(root.get().resources.displayMetrics.density,fontScale)
                val expected=with(fontDensity){layouts.single().layoutInput.style.fontSize.toPx()/density}
                val result=AtomicReference<Float?>()
                try { compose.waitUntil(10000) {
                    find(root.get())?.post {find(root.get())?.evaluateJavascript("""(function(){var e=document.getElementById('font_probe');return e?parseFloat(getComputedStyle(e).fontSize):0;})()"""){value->
                        (JSONTokener(value).nextValue() as? Number)?.toFloat()?.let {result.set(it)}
                    }}
                    result.get()?.let{kotlin.math.abs(it-expected)<0.05f} == true
                }
                } catch (error: androidx.compose.ui.test.ComposeTimeoutException) {
                    fail("字号验证超时 zoom=$zoom scale=$fontScale expected=$expected css=${result.get()} browser=${find(root.get()) != null}")
                }
                compose.runOnIdle {assertEquals(100,find(root.get())!!.settings.textZoom)}
            }
            compose.onNodeWithTag("article_floors").performScrollToNode(hasTestTag("floor_0"))
            val list=compose.onNodeWithTag("article_floors").fetchSemanticsNode().boundsInRoot
            val card=compose.onNodeWithTag("floor_0").fetchSemanticsNode().boundsInRoot
            val density=root.get().resources.displayMetrics.density
            assertEquals(8*density,card.left-list.left,0.5f)
            assertEquals(8*density,list.right-card.right,0.5f)
        } finally {compose.runOnIdle{config.webViewTextZoom=original}}
    }
}
