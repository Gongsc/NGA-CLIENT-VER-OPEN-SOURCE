package gov.anzong.androidnga.mvvm.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import gov.anzong.androidnga.coroutines.PendingResponse
import gov.anzong.androidnga.mvvm.model.ImageFile
import gov.anzong.androidnga.mvvm.model.TopicPostRepository
import gov.anzong.androidnga.mvvm.model.UploadResult
import gov.anzong.androidnga.mvvm.model.parseUploadResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import gov.anzong.androidnga.mvvm.model.param.PostParam
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TopicPostViewModelTest {
    private val main = UnconfinedTestDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + main)
    @Before fun setMain() = Dispatchers.setMain(main)
    @After fun resetMain() = Dispatchers.resetMain()

    private class FakeRepository : TopicPostRepository {
        var prepare: () -> String? = { "auth" }
        var submit: suspend (PostParam) -> String = { "发贴完毕" }
        val uploads = mutableListOf<Boolean>()
        var upload: (Boolean) -> UploadResult = { UploadResult.Uploaded("mon_1/a.jpg", "att", "chk") }
        val submitted = mutableListOf<Triple<String?, String?, Boolean>>()

        override suspend fun prepare(param: PostParam) = prepare()
        override suspend fun loadCategories(fid: Int) = listOf("[讨论]", "[求助]", "[讨论]")
        override suspend fun submit(param: PostParam): String {
            submitted += Triple(param.postSubject, param.postContent, param.isAnonymous)
            return submit.invoke(param)
        }
        override suspend fun upload(image: ImageFile, param: PostParam): UploadResult {
            val compress = image.name == "compressed"
            uploads += compress
            return upload(compress)
        }
    }

    private fun param(action: String, subject: String? = null, content: String? = null) =
        PostParam("1", subject, content).apply { postAction = action; postFid = 7 }

    private fun viewModel(param: PostParam, repository: FakeRepository, notices: MutableList<String> = mutableListOf()) =
        TopicPostViewModel(param, repository, scope, main) { notices += it }

    @Test fun startFillsEditorOnceAndPrepares() {
        val param = param("modify", "原标题", "原正文")
        val vm = viewModel(param, FakeRepository())
        vm.start(null, null, -1, false)
        assertEquals("原标题", vm.editor.title.text)
        assertEquals(TextFieldValue("原正文", TextRange(3)), vm.editor.body)
        assertFalse(vm.editor.dirty)
        assertTrue(vm.editor.attachmentReady)
        assertFalse(vm.editor.preparing)
        assertEquals("auth", param.authCode)

        vm.editor.body = TextFieldValue("改过的正文")
        vm.start("别的", "别的", 0, true)
        assertEquals("改过的正文", vm.editor.body.text)
        assertEquals("原标题", vm.editor.title.text)
    }

    @Test fun restoredDraftAfterProcessDeathKeepsCursor() {
        val vm = viewModel(param("reply", content = "[quote]引用[/quote]"), FakeRepository())
        vm.start("", "草稿正文", 2, true)
        assertEquals(TextFieldValue("草稿正文", TextRange(2)), vm.editor.body)
        assertTrue(vm.editor.anonymous)
        assertTrue(vm.editor.dirty)
    }

    @Test fun failedPreparationCanBeRetried() {
        val repository = FakeRepository().apply { prepare = { throw IOException("需要登录") } }
        val vm = viewModel(param("reply"), repository)
        vm.start(null, null, -1, false)
        assertEquals("需要登录", vm.editor.error)
        assertFalse(vm.editor.attachmentReady)
        assertFalse(vm.editor.preparing)
        repository.prepare = { "auth" }
        vm.prepare()
        assertNull(vm.editor.error)
        assertTrue(vm.editor.attachmentReady)
    }

    @Test fun submitValidatesAndPostsOnce() {
        val pending = PendingResponse<String>()
        val repository = FakeRepository().apply { submit = { pending.await() } }
        val vm = viewModel(param("new"), repository)
        vm.start(null, null, -1, false)
        vm.editor.body = TextFieldValue("正文")
        vm.submit()
        assertEquals("请填写标题及正文后提交。", vm.editor.error)
        assertTrue(repository.submitted.isEmpty())

        vm.editor.title = TextFieldValue("标题")
        vm.editor.anonymous = true
        vm.submit()
        assertTrue(vm.editor.submitting)
        vm.submit()
        assertEquals(listOf(Triple("标题", "正文", true)), repository.submitted)
        assertFalse(vm.posted)
        pending.complete("发贴完毕")
        assertTrue(vm.posted)
        assertFalse(vm.editor.submitting)
    }

    @Test fun failedSubmitShowsReasonAndKeepsEditor() {
        val repository = FakeRepository().apply { submit = { throw IOException("帖子已锁定") } }
        val vm = viewModel(param("reply"), repository)
        vm.start(null, "回复", -1, false)
        vm.submit()
        assertEquals("帖子已锁定", vm.editor.error)
        assertFalse(vm.posted)
        assertFalse(vm.editor.submitting)
        assertEquals("回复", vm.editor.body.text)
    }

    @Test fun uploadInsertsImageAtCursorAndRecordsAttachment() {
        val param = param("reply")
        val notices = mutableListOf<String>()
        val repository = FakeRepository().apply { upload = { compress -> if (compress) UploadResult.Uploaded("mon_1/a.jpg", "att", "chk") else UploadResult.TooLarge } }
        val vm = viewModel(param, repository, notices)
        vm.start(null, "前后", -1, false)
        vm.editor.body = vm.editor.body.copy(selection = TextRange(1))
        vm.upload { compress -> ImageFile(if (compress) "compressed" else "original", ByteArray(0)) }
        assertEquals(listOf(false, true), repository.uploads)
        assertEquals("前\n[img]./mon_1/a.jpg[/img]\n后", vm.editor.body.text)
        assertEquals("%09att", param.attachments)
        assertEquals("%09chk", param.attachmentsCheck)
        assertEquals(listOf("附件过大，无法上传，重新进行压缩并上传", "上传成功"), notices)
        assertFalse(vm.editor.uploading)
    }

    @Test fun uploadStillTooLargeAfterCompressionReportsError() {
        val repository = FakeRepository().apply { upload = { UploadResult.TooLarge } }
        val vm = viewModel(param("reply"), repository)
        vm.start(null, "正文", -1, false)
        vm.upload { compress -> ImageFile(if (compress) "compressed" else "original", ByteArray(0)) }
        assertEquals("附件过大，无法上传", vm.editor.error)
        assertEquals("正文", vm.editor.body.text)
    }

    @Test fun categoriesAreDeduplicated() {
        val vm = viewModel(param("new"), FakeRepository())
        vm.loadCategories()
        assertEquals(listOf("[讨论]", "[求助]"), vm.editor.categories)
        assertFalse(vm.editor.categoryLoading)
    }

    @Test fun uploadResponseParsing() {
        val ok = "window.script_muti_get_var_store={\"data\":{\"url\":\"mon_1/a.jpg\",\"attachments\":\"att\",\"attachments_check\":\"chk\"}}"
        assertEquals(UploadResult.Uploaded("mon_1/a.jpg", "att", "chk"), parseUploadResponse(ok))
        assertEquals(UploadResult.TooLarge, parseUploadResponse("{\"error_code\":9}"))
        assertThrows(Exception::class.java) { parseUploadResponse("{\"error_code\":1}") }
    }
}
