package gov.anzong.androidnga.json

import gov.anzong.androidnga.account.User
import gov.anzong.androidnga.base.json.NgaJson
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo
import gov.anzong.androidnga.network.bean.ThreadRowInfo
import gov.anzong.androidnga.network.favorites.ForumFavoriteBoard
import gov.anzong.androidnga.network.favorites.ForumFavoritesParser
import gov.anzong.androidnga.settings.FilterKeyword
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * 约束 [NgaJson] 的对象绑定规则，以及写入 SharedPreferences 和 Intent 参数的数据格式，期望值由 fastjson 1.1 实测得到。
 *
 * 本地已保存的数据由使用 fastjson 的旧版本写入，必须能原样读取，并继续写出可被旧版本读取的格式。两条绑定路径规则不同：
 * `parseObject/parseArray(text, Class)` 宽松匹配键名（忽略大小写与下划线），`toJavaObject`
 * 仅按属性名精确匹配，两者的类型转换规则也不同。
 */
class NgaJsonBindingTest {

    // ---- 屏蔽用户：PreferenceKey.BLACK_LIST、旧版 USER_LIST ----

    @Test
    fun userIsWrittenWithBothPropertyAndFieldKeys() {
        val user = User("42", "昵称", "cid-x")
        user.avatarUrl = "https://img/a.png"
        assertEquals(
            "[{\"avatarUrl\":\"https://img/a.png\",\"cid\":\"cid-x\",\"mAvatarUrl\":\"https://img/a.png\"," +
                "\"mCid\":\"cid-x\",\"mNickName\":\"昵称\",\"mUserId\":\"42\",\"nickName\":\"昵称\"," +
                "\"userId\":\"42\"}]",
            NgaJson.toJSONString(listOf(user)),
        )
    }

    @Test
    fun userIsReadFromAnyStoredKeyStyle() {
        assertUser("1|n|c|a", "[{\"userId\":\"1\",\"nickName\":\"n\",\"cid\":\"c\",\"avatarUrl\":\"a\"}]")
        assertUser("1|n|c|a", "[{\"mUserId\":\"1\",\"mNickName\":\"n\",\"mCid\":\"c\",\"mAvatarUrl\":\"a\"}]")
        assertUser("1|n|null|null", "[{\"userid\":1,\"nickname\":\"n\"}]")
        assertUser("1|n|null|null", "[{\"user_id\":\"1\",\"nick_name\":\"n\"}]")
    }

    private fun assertUser(expected: String, source: String) {
        val user = NgaJson.parseArray(source, User::class.java)!![0]
        assertEquals(source, expected, "${user.userId}|${user.nickName}|${user.cid}|${user.avatarUrl}")
    }

    // ---- 屏蔽词：PreferenceKey.FILTER_KEYWORDS_LIST ----

    @Test
    fun filterKeywordFormat() {
        assertEquals(
            "[{\"enabled\":true,\"keyword\":\"词\"},{\"enabled\":false}]",
            NgaJson.toJSONString(listOf(FilterKeyword("词"), FilterKeyword())),
        )
        assertKeyword("k|true", "[{\"keyword\":\"k\",\"enabled\":true}]")
        assertKeyword("k|false", "[{\"keyword\":\"k\"}]")
        assertKeyword("k|true", "[{\"keyword\":\"k\",\"enabled\":\"1\"}]")
        assertKeyword("k|true", "[{\"keyword\":\"k\",\"enabled\":1}]")
    }

    private fun assertKeyword(expected: String, source: String) {
        val keyword = NgaJson.parseArray(source, FilterKeyword::class.java)!![0]
        assertEquals(source, expected, "${keyword.keyword}|${keyword.isEnabled}")
    }

    // ---- 浏览历史与主题参数：KEY_TOPIC_HISTORY、topicInfo ----

    private fun sampleTopic() = ThreadPageInfo().apply {
        tid = 42
        fid = -7
        author = "作者"
        authorId = 9
        lastPoster = "lp"
        replies = 3
        subject = "主题"
        titleFont = "tf"
        type = 1024
        topicMisc = "misc"
        page = 2
        pid = 77
        position = 5
        isAnonymity = true
        postDate = 1700000000
        board = "版面"
        historyVisitedAt = 1700000000123L
        replyInfo = ThreadPageInfo.ReplyInfo().apply {
            pidStr = "77"
            content = "回复"
            subject = "rs"
            postDate = "2024"
            authorId = "9"
            tidStr = "42"
        }
    }

    @Test
    fun topicHistoryFormat() {
        assertEquals(SAMPLE_TOPIC_JSON, NgaJson.toJSONString(sampleTopic()))
        assertEquals(
            "{\"anonymity\":false,\"authorId\":0,\"fid\":0,\"historyVisitedAt\":0,\"mirrorBoard\":false," +
                "\"page\":0,\"pid\":0,\"position\":0,\"postDate\":0,\"replies\":0,\"tid\":0,\"type\":0}",
            NgaJson.toJSONString(ThreadPageInfo()),
        )
        assertEquals(
            SAMPLE_TOPIC_JSON,
            NgaJson.toJSONString(NgaJson.parseArray("[$SAMPLE_TOPIC_JSON]", ThreadPageInfo::class.java)!![0]),
        )
    }

    @Test
    fun olderTopicHistoryIsReadWithCoercion() {
        val topic = NgaJson.parseObject(
            "{\"tid\":\"42\",\"page\":3,\"subject\":\"离线\",\"mirrorBoard\":true,\"anonymity\":1," +
                "\"historyVisitedAt\":\"5\"}",
            ThreadPageInfo::class.java,
        )!!
        assertEquals(42, topic.tid)
        assertEquals(3, topic.page)
        assertEquals("离线", topic.subject)
        assertTrue(topic.isAnonymity)
        assertEquals(5, topic.historyVisitedAt)
        // mirrorBoard 只有读取方法，读入时被忽略。
        assertFalse(topic.isMirrorBoard)
    }

    // ---- 搜索历史与版面收藏缓存 ----

    @Test
    fun stringListAndForumFavoriteCacheFormat() {
        assertEquals("[\"a\",\"中\"]", NgaJson.toJSONString(listOf("a", "中")))
        val boards = listOf(ForumFavoriteBoard(-7, 0, "版块", null), ForumFavoriteBoard(0, 42, "合集", "置顶"))
        val cache = ForumFavoritesParser.serialize(boards)
        assertEquals(
            "[{\"fid\":-7,\"stid\":0,\"name\":\"版块\"},{\"fid\":0,\"stid\":42,\"name\":\"合集\",\"topped_topic\":\"置顶\"}]",
            cache,
        )
        assertEquals(boards, ForumFavoritesParser.parseCache(cache))
    }

    // ---- 帖子数据绑定：ArticleConvertFactory 使用 toJavaObject ----

    @Test
    fun topicHeaderBindsOnlyExactPropertyNames() {
        val topic = NgaJson.toJavaObject(
            NgaJson.parseObject(
                "{\"tid\":12,\"fid\":-7,\"subject\":\"主题\",\"author\":\"作者\",\"authorid\":\"9\"," +
                    "\"postdate\":1700000000,\"type\":\"1024\",\"topic_misc\":\"m\",\"titlefont\":\"tf\"," +
                    "\"lastposter\":\"lp\",\"replies\":\"3\",\"quote_from\":0,\"this_visit_rows\":20}",
            ),
            ThreadPageInfo::class.java,
        )!!
        assertEquals(12, topic.tid)
        assertEquals(-7, topic.fid)
        assertEquals("主题", topic.subject)
        assertEquals("作者", topic.author)
        assertEquals(1024, topic.type)
        assertEquals(3, topic.replies)
        // 大小写或下划线不同的键不会绑定。
        assertEquals(0, topic.authorId)
        assertEquals(0, topic.postDate)
        assertNull(topic.topicMisc)
        assertNull(topic.titleFont)
        assertNull(topic.lastPoster)
    }

    @Test
    fun replyRowBindsWithCoercionAndNestedAttachments() {
        val row = NgaJson.toJavaObject(
            NgaJson.parseObject(
                "{\"tid\":\"12\",\"fid\":-7,\"author\":\"作者\",\"authorid\":\"9\",\"subject\":\"标题\"," +
                    "\"vote\":\"v\",\"postdate\":\"2024-01-02 03:04\",\"postdatetimestamp\":1700000000," +
                    "\"pid\":\"0\",\"isanonymous\":\"1\",\"alterinfo\":\"[E1]\",\"content\":\"正文\"," +
                    "\"lou\":\"3\",\"attachs\":{\"0\":{\"attachurl\":\"a.png\",\"type\":\"img\",\"size\":\"12\"}}," +
                    "\"level\":\"lv\",\"yz\":\"1\",\"js_escap_avatar\":\"av\",\"muteTime\":\"0\"," +
                    "\"aurvrc\":\"10\",\"signature\":\"sig\",\"comment\":{\"0\":{\"pid\":5}}," +
                    "\"hotReplies\":[\"1\"],\"from_client\":\"8 ios\",\"from_client_model\":\"m\"," +
                    "\"score\":\"4\",\"score_2\":2,\"_IsInBlackList\":true,\"recommend\":\"1,2\"}",
            ),
            ThreadRowInfo::class.java,
        )!!
        assertEquals(12, row.tid)
        assertEquals(9, row.authorid)
        assertEquals(3, row.lou)
        assertEquals(4, row.score)
        assertEquals(2, row.score_2)
        assertEquals(10, row.aurvrc)
        assertEquals("作者", row.author)
        assertEquals("标题", row.subject)
        assertEquals("正文", row.content)
        assertEquals("2024-01-02 03:04", row.postdate)
        assertEquals("[E1]", row.alterinfo)
        assertEquals("av", row.js_escap_avatar)
        assertEquals("sig", row.signature)
        assertEquals("v", row.vote)
        assertEquals("lv", row.level)
        assertEquals("1", row.yz)
        assertEquals("0", row.muteTime)
        assertEquals(listOf("1"), row.hotReplies)
        val attachment = row.attachs!!["0"]!!
        assertEquals("a.png", attachment.attachurl)
        assertEquals("img", attachment.type)
        assertEquals(12, attachment.size)
        // 以下键与属性名不一致，不会绑定，由转换代码另行读取。
        assertEquals(0, row.fid)
        assertFalse(row.isAnonymous)
        assertFalse(row.isInBlackList)
        assertNull(row.fromClient)
        assertNull(row.fromClientModel)
        assertNull(row.comments)
    }

    @Test
    fun replyRowKeyMatchingIsCaseSensitive() {
        val row = NgaJson.toJavaObject(
            NgaJson.parseObject("{\"FROM_CLIENT\":\"x\",\"fromClient\":\"y\",\"isAnonymous\":true,\"Lou\":2}"),
            ThreadRowInfo::class.java,
        )!!
        assertEquals("y", row.fromClient)
        assertFalse(row.isAnonymous)
        assertEquals(0, row.lou)
    }

    @Test
    fun replyRowEmptyContainersBecomeNull() {
        val row = NgaJson.toJavaObject(NgaJson.parseObject("{\"attachs\":\"\",\"comment\":\"\"}"), ThreadRowInfo::class.java)!!
        assertNull(row.attachs)
        assertNull(row.comments)
    }

    @Test
    fun replyRowRejectsUnconvertibleValues() {
        for (source in listOf("{\"lou\":\"\"}", "{\"pid\":\"abc\"}", "{\"attachs\":[{\"attachurl\":\"a\"}]}")) {
            try {
                NgaJson.toJavaObject(NgaJson.parseObject(source), ThreadRowInfo::class.java)
                fail("应拒绝：$source")
            } catch (expected: RuntimeException) {
                // 当前转换整页失败；替换时保持一致，放宽另行处理。
            }
        }
    }

    // ---- 两条绑定路径的类型转换差异 ----

    /** 绑定为 ThreadPageInfo 后的 tid|anonymity|subject，失败记为 "!"。 */
    private fun topicFields(source: String, strict: Boolean): String = try {
        val topic = topic(source, strict)
        "${topic.tid}|${topic.isAnonymity}|${topic.subject}"
    } catch (e: RuntimeException) {
        "!"
    }

    @Test
    fun textAndTreeBindingCoerceScalarsDifferently() {
        // 输入、parseObject(text, Class) 的结果、toJavaObject 的结果
        val cases = listOf(
            Triple("{\"tid\":\"\"}", "0|false|null", "!"),
            Triple("{\"tid\":null}", "0|false|null", "!"),
            Triple("{\"tid\":\"abc\"}", "!", "!"),
            Triple("{\"tid\":1.9}", "!", "1|false|null"),
            Triple("{\"tid\":3000000000}", "!", "-1294967296|false|null"),
            Triple("{\"tid\":true}", "!", "!"),
            Triple("{\"anonymity\":\"\"}", "0|false|null", "!"),
            Triple("{\"anonymity\":\"Y\"}", "!", "!"),
            Triple("{\"anonymity\":2}", "0|false|null", "0|false|null"),
            Triple("{\"anonymity\":1}", "0|true|null", "0|true|null"),
            Triple("{\"subject\":12}", "0|false|12", "0|false|12"),
            Triple("{\"subject\":{\"a\":1}}", "0|false|{\"a\":1}", "0|false|{\"a\":1}"),
            Triple("{\"subject\":[1]}", "0|false|[1]", "0|false|[1]"),
            Triple("{\"TID\":5}", "5|false|null", "0|false|null"),
            Triple("{\"t_i_d\":5}", "5|false|null", "0|false|null"),
            Triple("{\"t-id\":5}", "5|false|null", "0|false|null"),
        )
        for ((source, text, tree) in cases) {
            assertEquals("parseObject $source", text, topicFields(source, false))
            assertEquals("toJavaObject $source", tree, topicFields(source, true))
        }
        assertEquals(7, NgaJson.parseObject("{\"historyvisitedat\":7}", ThreadPageInfo::class.java)!!.historyVisitedAt)
    }

    @Test
    fun nestedObjectsAndContainers() {
        for (strict in listOf(false, true)) {
            assertNull(topic("{\"replyInfo\":\"\"}", strict).replyInfo)
            assertEquals("5", topic("{\"replyInfo\":{\"pidStr\":5}}", strict).replyInfo!!.pidStr)
            assertEquals(listOf("1", null), row("{\"hotReplies\":[1,null]}", strict).hotReplies)
            assertNull(row("{\"hotReplies\":\"\"}", strict).hotReplies)
            val empty = row("{\"attachs\":{\"0\":\"\"}}", strict)
            assertTrue(empty.attachs!!.containsKey("0"))
            assertNull(empty.attachs!!["0"])
            assertEquals(0, row("{\"attachs\":{}}", strict).attachs!!.size)
            assertEquals(2, row("{\"comments\":[{\"lou\":2}]}", strict).comments!![0].lou)
            assertEquals(1.5f, row("{\"reputation\":\"1.5\"}", strict).reputation, 0f)
            assertNull(row("{\"content\":null}", strict).content)
            try {
                topic("{\"replyInfo\":[]}", strict)
                fail("对象属性应拒绝数组")
            } catch (expected: RuntimeException) {
            }
        }
        // 基本类型遇到 null：文本绑定保留默认值，树形绑定失败。
        assertEquals(0, row("{\"lou\":null}", false).lou)
        try {
            row("{\"lou\":null}", true)
            fail("toJavaObject 应拒绝 null 写入 int")
        } catch (expected: RuntimeException) {
        }
        // 文本绑定可通过读取方法填充没有写入方法的集合。
        assertEquals(listOf("a"), row("{\"imageUrls\":[\"a\"]}", false).imageUrls)
    }

    private fun topic(source: String, strict: Boolean): ThreadPageInfo =
        if (strict) {
            NgaJson.toJavaObject(NgaJson.parseObject(source), ThreadPageInfo::class.java)!!
        } else {
            NgaJson.parseObject(source, ThreadPageInfo::class.java)!!
        }

    private fun row(source: String, strict: Boolean): ThreadRowInfo =
        if (strict) {
            NgaJson.toJavaObject(NgaJson.parseObject(source), ThreadRowInfo::class.java)!!
        } else {
            NgaJson.parseObject(source, ThreadRowInfo::class.java)!!
        }

    @Test
    fun typedDocumentEdges() {
        val keywords: List<FilterKeyword?> = NgaJson.parseArray("[null,{\"keyword\":\"k\"}]", FilterKeyword::class.java)!!
        assertNull(keywords[0])
        assertEquals("k", keywords[1]!!.keyword)
        assertNull(NgaJson.parseObject("", ThreadPageInfo::class.java))
        try {
            NgaJson.parseObject("[]", ThreadPageInfo::class.java)
            fail("实体不应从数组绑定")
        } catch (expected: RuntimeException) {
        }
        val map = NgaJson.parseObject("{\"b\":{\"x\":\"1\"},\"a\":2}", Map::class.java)!!
        assertEquals(2, map["a"])
        assertEquals("1", (map["b"] as Map<*, *>)["x"])
    }

    private companion object {
        const val SAMPLE_TOPIC_JSON =
            "{\"anonymity\":true,\"author\":\"作者\",\"authorId\":9,\"board\":\"版面\",\"fid\":-7," +
                "\"historyVisitedAt\":1700000000123,\"lastPoster\":\"lp\",\"mirrorBoard\":false,\"page\":2," +
                "\"pid\":77,\"position\":5,\"postDate\":1700000000,\"replies\":3,\"replyInfo\":{\"authorId\":\"9\"," +
                "\"content\":\"回复\",\"pidStr\":\"77\",\"postDate\":\"2024\",\"subject\":\"rs\",\"tidStr\":\"42\"}," +
                "\"subject\":\"主题\",\"tid\":42,\"titleFont\":\"tf\",\"topicMisc\":\"misc\",\"type\":1024}"
    }
}
