package gov.anzong.androidnga.ui.compose
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import gov.anzong.androidnga.activity.AboutScreen
import gov.anzong.androidnga.util.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class AboutScreenTest {
    @get:Rule val compose=createComposeRule()

    private fun show(result: UpdateResult, opened: MutableList<String>) = compose.setContent {
        NgaTheme { AboutScreen(Modifier,"4.0.0",4000,{result},{},{},{opened+=it},{opened+=it}) }
    }

    @Test fun showsVersionProjectLinksAndNoQqGroups() {
        val opened=mutableListOf<String>()
        show(UpdateResult.UpToDate(null),opened)
        compose.onNodeWithText("版本 4.0.0（4000）· 查看更新内容").assertIsDisplayed()
        compose.onNodeWithText("项目主页").performClick()
        compose.onNodeWithText("问题反馈").performClick()
        compose.runOnIdle { assertEquals(listOf(PROJECT_URL,ISSUES_URL),opened) }
        compose.onAllNodes(hasText("QQ",substring=true)).assertCountEquals(0)
    }

    @Test fun newerReleaseOffersDownloadPage() {
        val opened=mutableListOf<String>()
        show(UpdateResult.Available(ReleaseInfo("4.1.0","$RELEASES_URL/tag/v4.1.0","修复问题")),opened)
        compose.onNodeWithText("检查更新").performClick()
        compose.onNodeWithText("发现新版本 4.1.0").assertIsDisplayed()
        compose.onNodeWithText("修复问题").assertIsDisplayed()
        compose.onNodeWithText("前往下载").performClick()
        compose.runOnIdle { assertEquals(listOf("$RELEASES_URL/tag/v4.1.0"),opened) }
        compose.onAllNodesWithText("发现新版本 4.1.0").assertCountEquals(0)
    }

    @Test fun missingReleaseAndFailuresAreExplained() {
        val opened=mutableListOf<String>()
        show(UpdateResult.UpToDate(null),opened)
        compose.onNodeWithText("检查更新").performClick()
        compose.onNodeWithText("暂无发布版本").assertIsDisplayed()
        compose.onNodeWithText("查看发布页").performClick()
        compose.runOnIdle { assertEquals(listOf(RELEASES_URL),opened) }
    }
}
