package gov.anzong.androidnga.json

import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.mvvm.model.convert.ErrorConvertFactory
import gov.anzong.androidnga.mvvm.model.convert.ForumNotificationFactory
import gov.anzong.androidnga.mvvm.viewmodel.homeReplyError
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 迁移校验：接口树形取值的转换结果。期望值由迁移前的 fastjson 实现生成，迁移后须保持不变。
 *
 * 只覆盖可在 JVM 中运行的转换；会调用 android.util.Log 或依赖 Android 初始化的路径未包含在内。
 */
class ConverterGoldenTest {

    @Test
    fun notificationLists() {
        assertEquals("[" + REPLY_ROWS + "]", json(ForumNotificationFactory.buildRecentReplyList(REPLIES)))
        assertEquals(
                "[" + REPLY_ROWS + ",{\"type\":5,\"unread\":true,\"userName\":\"发信人\"}]",
                json(ForumNotificationFactory.buildNotificationList(REPLIES)))
        // 缺少 unread 时按无未读处理。
        assertEquals(
                "[{\"pidStr\":\"34\",\"tidStr\":\"12\",\"timeStamp\":\"1\",\"title\":\"主题\",\"type\":1,\"unread\":false," +
                        "\"userId\":\"9\",\"userName\":\"用户\"}]",
                json(
                        ForumNotificationFactory.buildRecentReplyList(
                                "{\"data\":{\"0\":{\"0\":[{\"0\":1,\"1\":9,\"2\":\"用户\",\"5\":\"主题\",\"6\":12,\"7\":34,\"9\":1}]}}}")))
        val read = "{\"data\":{\"0\":{\"0\":[{\"0\":1,\"1\":9,\"2\":\"u\",\"5\":\"t\",\"6\":1,\"7\":2,\"9\":3}],\"unread\":0}}}"
        val readRows =
                "[{\"pidStr\":\"2\",\"tidStr\":\"1\",\"timeStamp\":\"3\",\"title\":\"t\",\"type\":1,\"unread\":false," +
                        "\"userId\":\"9\",\"userName\":\"u\"}]"
        assertEquals(readRows, json(ForumNotificationFactory.buildRecentReplyList(read)))
        assertEquals(readRows, json(ForumNotificationFactory.buildNotificationList(read)))
        assertEquals("[]", json(ForumNotificationFactory.buildRecentReplyList("{\"data\":{}}")))
        assertEquals("[]", json(ForumNotificationFactory.buildNotificationList("{\"data\":{}}")))
        assertEquals("[]", json(ForumNotificationFactory.buildRecentReplyList("{\"data\":{\"0\":{}}}")))
    }

    @Test
    fun errorMessages() {
        assertEquals("操作成功", ErrorConvertFactory.getErrorMessage("{\"data\":{\"__MESSAGE\":{\"1\":\"操作成功\",\"2\":3}}}"))
        assertEquals("12", ErrorConvertFactory.getErrorMessage("{\"data\":{\"__MESSAGE\":{\"1\":12}}}"))
        assertEquals(null, ErrorConvertFactory.getErrorMessage("{\"data\":{}}"))
        assertEquals(null, ErrorConvertFactory.getErrorMessage("[1]"))
        assertEquals(null, ErrorConvertFactory.getErrorMessage("{bad"))
    }

    @Test
    fun homeReplyErrors() {
        assertEquals("你需要登录", homeReplyError("{\"error\":{\"0\":\"你需要登录\"}}"))
        assertEquals("数组错误", homeReplyError("{\"error\":[\"数组错误\"]}"))
        assertEquals("文本错误", homeReplyError("{\"error\":\"文本错误\"}"))
        assertEquals("7", homeReplyError("{\"error\":7}"))
        assertEquals(null, homeReplyError("{\"data\":{}}"))
        assertEquals("回复通知加载失败，请稍后重试", homeReplyError(""))
        assertEquals(null, homeReplyError("not json"))
        assertEquals("{\"1\":\"x\"}", homeReplyError("window.script_muti_get_var_store={\"error\":{\"1\":\"x\"}}"))
    }

    private fun json(value: Any?): String = NgaJson.toJSONString(value)

    private companion object {

        const val REPLIES =
            "window.script_muti_get_var_store={\"data\":{\"0\":{\"0\":[{\"0\":\"1\",\"1\":9,\"2\":\"用户\",\"5\":\"主题\"," +
            "\"6\":12,\"7\":34,\"9\":1700000000},{\"0\":2,\"1\":\"8\",\"2\":\"另一个\",\"5\":\"标题\",\"6\":\"13\"," +
            "\"8\":\"35\",\"9\":\"1700000001\"}],\"1\":[{\"0\":\"5\",\"2\":\"发信人\"}],\"unread\":\"1\"}},\"time\":1}"

        const val REPLY_ROWS =
            "{\"pidStr\":\"34\",\"tidStr\":\"12\",\"timeStamp\":\"1700000000\",\"title\":\"主题\",\"type\":1,\"unread\":true," +
            "\"userId\":\"9\",\"userName\":\"用户\"},{\"pidStr\":\"35\",\"tidStr\":\"13\",\"timeStamp\":\"1700000001\"," +
            "\"title\":\"标题\",\"type\":2,\"unread\":true,\"userId\":\"8\",\"userName\":\"另一个\"}"
    }
}
