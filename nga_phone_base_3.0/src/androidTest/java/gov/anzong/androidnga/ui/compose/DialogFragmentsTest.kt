package gov.anzong.androidnga.ui.compose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.fragment.app.DialogFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import gov.anzong.androidnga.activity.AboutActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import gov.anzong.androidnga.ui.dialog.*

/** 迁移到 Compose 的对话框；以关于页为宿主，只使用本地数据，不点击会提交到论坛的按钮。 */
class DialogFragmentsTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun withHost(block: (ActivityScenario<AboutActivity>) -> Unit) =
        ActivityScenario.launch<AboutActivity>(Intent(context, AboutActivity::class.java)).use(block)

    private fun ActivityScenario<AboutActivity>.show(dialog: Class<out DialogFragment>, args: Bundle? = null) =
        onActivity { BaseDialogFragment.show(it.supportFragmentManager, args, dialog) }

    @Test fun addBoardReturnsIdsAndKeepsInputAcrossRecreation() = withHost { scenario ->
        val results = mutableListOf<String>()
        scenario.onActivity { activity ->
            activity.supportFragmentManager.setFragmentResultListener(AddBoardDialogFragment.RESULT_KEY, activity) { _, result ->
                results += "${result.getString("fid")}/${result.getString("stid")}"
            }
        }
        scenario.show(AddBoardDialogFragment::class.java)
        compose.onNodeWithText("添加").assertIsNotEnabled()
        compose.onNodeWithText("版面ID（fid）").performTextInput("-7")
        scenario.recreate()
        scenario.onActivity { activity ->
            activity.supportFragmentManager.setFragmentResultListener(AddBoardDialogFragment.RESULT_KEY, activity) { _, result ->
                results += "${result.getString("fid")}/${result.getString("stid")}"
            }
        }
        compose.onNodeWithText("-7").assertExists()
        compose.onNodeWithText("添加").performClick()
        compose.runOnIdle { assertEquals(listOf("-7/"), results) }
    }

    @Test fun urlInputRejectsNonForumAddresses() = withHost { scenario ->
        scenario.show(UrlInputDialogFragment::class.java)
        compose.onNodeWithText("由URL读取").assertIsDisplayed()
        compose.onNodeWithText("输入URL地址(板块或帖子)").performTextClearance()
        compose.onNodeWithText("打开").performClick()
        compose.onNodeWithText("请输入URL地址").assertIsDisplayed()
        compose.onNodeWithText("输入URL地址(板块或帖子)").performTextInput("https://example.com/")
        compose.onNodeWithText("打开").performClick()
        compose.onNodeWithText("并非NGA", substring = true).assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("由URL读取").assertDoesNotExist()
    }

    @Test fun reportShowsReasonFieldAndCancelsWithoutSubmitting() = withHost { scenario ->
        scenario.show(ReportDialogFragment::class.java, Bundle().apply { putInt("tid", 1); putInt("pid", 2) })
        compose.onNodeWithText("不要仅仅因为意见不同而举报").assertIsDisplayed()
        compose.onNodeWithText("举报理由").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("举报理由").assertDoesNotExist()
    }

    @Test fun avatarSignatureVoteAndReleaseNotesShowAndClose() = withHost { scenario ->
        scenario.show(AvatarDialogFragment::class.java, Bundle().apply { putString("name", "甲"); putString("url", "") })
        compose.onNodeWithText("甲的头像").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()

        scenario.show(SignatureDialogFragment::class.java, Bundle().apply { putString("name", "乙"); putString("html", "<html><body>签名</body></html>") })
        compose.onNodeWithText("乙的签名").assertIsDisplayed()
        compose.onNodeWithTag("article_html").assertExists()
        compose.onNodeWithText("关闭").performClick()

        scenario.show(VoteDialogFragment::class.java, Bundle().apply { putString("html", "<html><body>本楼没有投票/投注内容</body></html>") })
        compose.onNodeWithText("投票/投注").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        compose.onNodeWithText("投票/投注").assertDoesNotExist()

        if (VersionUpgradeDialogFragment.upgradeTip(gov.anzong.androidnga.BuildConfig.VERSION_CODE) != null) {
            scenario.show(VersionUpgradeDialogFragment::class.java)
            compose.onNodeWithText("提示").assertIsDisplayed()
            compose.onNodeWithText("确定").performClick()
        }
    }
}
