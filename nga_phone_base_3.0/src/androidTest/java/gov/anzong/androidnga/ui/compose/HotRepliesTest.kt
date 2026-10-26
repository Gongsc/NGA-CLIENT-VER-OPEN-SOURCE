package gov.anzong.androidnga.ui.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import sp.phone.http.bean.ThreadRowInfo
import sp.phone.mvp.model.convert.ArticleConvertFactory

class HotRepliesTest {
    @get:Rule val compose = createComposeRule()
    private fun row(floor: Int) = ThreadRowInfo().apply { tid=12; pid=if(floor==0) 0 else 100+floor; lou=floor; author="用户 $floor"; content="正文 $floor"; postdate="2026-10-26" }

    @Test fun hotRepliesUseSameConversionAndSurviveCacheWithMalformedItemsSkipped() {
        val source="""{"data":{"__T":{"tid":12,"fid":7},"__ROWS":40,"__R__ROWS":2,"__U":{"9":{"uid":9,"username":"测试用户"}},"__R":{"0":{"tid":12,"fid":7,"pid":0,"lou":0,"content":"主楼","hotreply":{"2":{"pid":115,"lou":15,"authorid":9,"content":"[img]./mon_test/a.webp[/img]","from_client":"7 iOS"},"10":{"pid":115,"lou":15,"content":"重复"},"11":{"pid":118,"content":"异常","score":{}},"12":{"pid":119,"content":"另一个热点","authorid":9}}},"1":{"tid":12,"pid":101,"lou":1,"content":"第一楼"}}}}"""
        val data=ArticleConvertFactory.getArticleInfo(source)
        assertNotNull(data)
        assertEquals(2,data.rowList.size)
        assertEquals(listOf(115,119),data.hotReplyList.map{it.pid})
        assertEquals(12,data.hotReplyList.first().tid)
        assertEquals("测试用户",data.hotReplyList.first().author)
        assertEquals("ios",data.hotReplyList.first().fromClientModel)
        assertTrue(data.hotReplyList.first().imageUrls.single().startsWith("https://img.nga.cn/"))
        assertEquals(-1,data.hotReplyList.last().lou)
        assertEquals(2,ArticleConvertFactory.getArticleInfo(data.rawData).hotReplyList.size)
    }

    @Test fun hotSectionHasThemeBackgroundAndQuoteKeepsOriginalFloor() {
        val state=ArticlePageState().apply{loading=false; rows=listOf(row(0),row(1)); hotReplies=listOf(row(15))}
        val scheme=mutableStateOf(ngaColorScheme(AppPalette.GREEN,false))
        var quoted: ThreadRowInfo?=null
        compose.setContent { MaterialTheme(colorScheme=scheme.value) {
            ArticlePageScreen(state,{}, {quoted=it},{},{},{emptyList()},{_,_->})
        } }
        for(palette in AppPalette.entries) for(dark in listOf(false,true)) {
            val expected=ngaColorScheme(palette,dark)
            compose.runOnIdle {scheme.value=expected}
            val image=compose.onNodeWithTag("hot_replies_header").assertIsDisplayed().captureToImage().toPixelMap()
            assertEquals(expected.secondaryContainer,image[image.width/2,2])
            assertTrue(expected.secondaryContainer != expected.surface)
        }
        val main=compose.onNodeWithTag("floor_0").fetchSemanticsNode().boundsInRoot
        val header=compose.onNodeWithTag("hot_replies_header").fetchSemanticsNode().boundsInRoot
        val hot=compose.onNodeWithTag("hot_reply_115").fetchSemanticsNode().boundsInRoot
        val first=compose.onNodeWithTag("floor_1").fetchSemanticsNode().boundsInRoot
        assertTrue(main.bottom <= header.top && header.bottom <= hot.top && hot.bottom <= first.top)
        compose.onAllNodesWithContentDescription("引用回复").filterToOne(hasAnyAncestor(hasTestTag("hot_reply_115"))).performClick()
        compose.runOnIdle {assertEquals(15,quoted?.lou);assertEquals(115,quoted?.pid)}
    }

    @Test fun jumpTargetsNormalFloorRatherThanPromotedDuplicate() {
        val state=ArticlePageState().apply {loading=false; rows=(0..19).map(::row); hotReplies=listOf(row(15))}
        compose.setContent { NgaTheme { ArticlePageScreen(state,{},{},{},{},{emptyList()},{_,_->}) } }
        compose.runOnIdle {state.targetFloor=15;state.scrollRequest++}
        compose.onNodeWithTag("floor_15").assertIsDisplayed()
        compose.onNodeWithTag("hot_replies_header").assertDoesNotExist()
    }

    @Test fun hotTitleAndQuoteRemainReachableInConstrainedWindow() {
        val state=ArticlePageState().apply {loading=false; rows=listOf(row(0),row(1)); hotReplies=listOf(row(15))}
        var quoted=false
        compose.setContent { NgaTheme { ArticlePageScreen(state,{}, {quoted=true},{},{},{emptyList()},{_,_->}) } }
        compose.onNodeWithTag("article_floors").performScrollToNode(hasTestTag("hot_replies_header"))
        compose.onNodeWithText("热点回复").assertIsDisplayed()
        compose.onNodeWithTag("article_floors").performScrollToNode(hasTestTag("hot_reply_115"))
        compose.onAllNodesWithContentDescription("引用回复").filterToOne(hasAnyAncestor(hasTestTag("hot_reply_115"))).performClick()
        compose.runOnIdle {assertTrue(quoted)}
    }
}
