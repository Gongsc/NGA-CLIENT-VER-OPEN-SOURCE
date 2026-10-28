package gov.anzong.androidnga.ui.compose

import gov.anzong.androidnga.core.data.HtmlData
import gov.anzong.androidnga.core.decode.ForumBasicDecoder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 帖子中的视频与音频使用可用的附件域名；ForumBasicDecoder 依赖 Android，放在设备测试中。 */
class ForumMediaHostTest {
    @Test fun videoAndAudioUseWorkingAttachmentHost() {
        val html = ForumBasicDecoder().decode("[flash=video]./mon_202610/26/v.mp4[/flash][flash=audio]./mon_202610/26/a.mp3?duration=3[/flash]",
            HtmlData("").apply { ngaHost = "https://bbs.nga.cn/" })
        assertTrue(html, html.contains("<video src='https://img.nga.cn/attachments/mon_202610/26/v.mp4'"))
        assertTrue(html, html.contains("<audio src='https://img.nga.cn/attachments/mon_202610/26/a.mp3?duration=3&filename=nga_audio.mp3'"))
        assertFalse(html, html.contains("ngacn.cc") || html.contains("nga.178.com"))
    }
}
