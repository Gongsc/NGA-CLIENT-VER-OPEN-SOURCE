package gov.anzong.androidnga.json;

import gov.anzong.androidnga.base.json.NgaJson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gov.anzong.androidnga.settings.FilterKeyword;
import gov.anzong.androidnga.account.User;
import gov.anzong.androidnga.network.bean.Attachment;
import gov.anzong.androidnga.network.bean.ThreadRowInfo;
import gov.anzong.androidnga.network.favorites.ForumFavoriteBoard;
import gov.anzong.androidnga.network.favorites.ForumFavoritesParser;
import gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo;

/**
 * 约束 {@link NgaJson} 的对象绑定规则，以及写入 SharedPreferences 和 Intent 参数的数据格式，期望值由 fastjson 1.1 实测得到。
 *
 * <p>本地已保存的数据由使用 fastjson 的旧版本写入，必须能原样读取，并继续写出可被旧版本读取的格式。两条绑定路径规则不同：
 * {@code parseObject/parseArray(text, Class)} 宽松匹配键名（忽略大小写与下划线），{@code toJavaObject}
 * 仅按属性名精确匹配，两者的类型转换规则也不同。
 */
public class NgaJsonBindingTest {

    // ---- 屏蔽用户：PreferenceKey.BLACK_LIST、旧版 USER_LIST ----

    @Test
    public void userIsWrittenWithBothPropertyAndFieldKeys() {
        User user = new User("42", "昵称", "cid-x");
        user.setAvatarUrl("https://img/a.png");
        assertEquals(
                "[{\"avatarUrl\":\"https://img/a.png\",\"cid\":\"cid-x\",\"mAvatarUrl\":\"https://img/a.png\","
                        + "\"mCid\":\"cid-x\",\"mNickName\":\"昵称\",\"mUserId\":\"42\",\"nickName\":\"昵称\","
                        + "\"userId\":\"42\"}]",
                NgaJson.toJSONString(Collections.singletonList(user)));
    }

    @Test
    public void userIsReadFromAnyStoredKeyStyle() {
        assertUser("1|n|c|a", "[{\"userId\":\"1\",\"nickName\":\"n\",\"cid\":\"c\",\"avatarUrl\":\"a\"}]");
        assertUser("1|n|c|a", "[{\"mUserId\":\"1\",\"mNickName\":\"n\",\"mCid\":\"c\",\"mAvatarUrl\":\"a\"}]");
        assertUser("1|n|null|null", "[{\"userid\":1,\"nickname\":\"n\"}]");
        assertUser("1|n|null|null", "[{\"user_id\":\"1\",\"nick_name\":\"n\"}]");
    }

    private static void assertUser(String expected, String source) {
        User user = NgaJson.parseArray(source, User.class).get(0);
        assertEquals(
                source,
                expected,
                user.getUserId() + "|" + user.getNickName() + "|" + user.getCid() + "|" + user.getAvatarUrl());
    }

    // ---- 屏蔽词：PreferenceKey.FILTER_KEYWORDS_LIST ----

    @Test
    public void filterKeywordFormat() {
        assertEquals(
                "[{\"enabled\":true,\"keyword\":\"词\"},{\"enabled\":false}]",
                NgaJson.toJSONString(Arrays.asList(new FilterKeyword("词"), new FilterKeyword())));
        assertKeyword("k|true", "[{\"keyword\":\"k\",\"enabled\":true}]");
        assertKeyword("k|false", "[{\"keyword\":\"k\"}]");
        assertKeyword("k|true", "[{\"keyword\":\"k\",\"enabled\":\"1\"}]");
        assertKeyword("k|true", "[{\"keyword\":\"k\",\"enabled\":1}]");
    }

    private static void assertKeyword(String expected, String source) {
        FilterKeyword keyword = NgaJson.parseArray(source, FilterKeyword.class).get(0);
        assertEquals(source, expected, keyword.getKeyword() + "|" + keyword.isEnabled());
    }

    // ---- 浏览历史与主题参数：KEY_TOPIC_HISTORY、topicInfo ----

    private static ThreadPageInfo sampleTopic() {
        ThreadPageInfo topic = new ThreadPageInfo();
        topic.setTid(42);
        topic.setFid(-7);
        topic.setAuthor("作者");
        topic.setAuthorId(9);
        topic.setLastPoster("lp");
        topic.setReplies(3);
        topic.setSubject("主题");
        topic.setTitleFont("tf");
        topic.setType(1024);
        topic.setTopicMisc("misc");
        topic.setPage(2);
        topic.setPid(77);
        topic.setPosition(5);
        topic.setAnonymity(true);
        topic.setPostDate(1700000000);
        topic.setBoard("版面");
        topic.setHistoryVisitedAt(1700000000123L);
        ThreadPageInfo.ReplyInfo reply = new ThreadPageInfo.ReplyInfo();
        reply.setPidStr("77");
        reply.setContent("回复");
        reply.setSubject("rs");
        reply.setPostDate("2024");
        reply.setAuthorId("9");
        reply.setTidStr("42");
        topic.setReplyInfo(reply);
        return topic;
    }

    private static final String SAMPLE_TOPIC_JSON =
            "{\"anonymity\":true,\"author\":\"作者\",\"authorId\":9,\"board\":\"版面\",\"fid\":-7,"
                    + "\"historyVisitedAt\":1700000000123,\"lastPoster\":\"lp\",\"mirrorBoard\":false,\"page\":2,"
                    + "\"pid\":77,\"position\":5,\"postDate\":1700000000,\"replies\":3,\"replyInfo\":{\"authorId\":\"9\","
                    + "\"content\":\"回复\",\"pidStr\":\"77\",\"postDate\":\"2024\",\"subject\":\"rs\",\"tidStr\":\"42\"},"
                    + "\"subject\":\"主题\",\"tid\":42,\"titleFont\":\"tf\",\"topicMisc\":\"misc\",\"type\":1024}";

    @Test
    public void topicHistoryFormat() {
        assertEquals(SAMPLE_TOPIC_JSON, NgaJson.toJSONString(sampleTopic()));
        assertEquals(
                "{\"anonymity\":false,\"authorId\":0,\"fid\":0,\"historyVisitedAt\":0,\"mirrorBoard\":false,"
                        + "\"page\":0,\"pid\":0,\"position\":0,\"postDate\":0,\"replies\":0,\"tid\":0,\"type\":0}",
                NgaJson.toJSONString(new ThreadPageInfo()));
        assertEquals(
                SAMPLE_TOPIC_JSON,
                NgaJson.toJSONString(NgaJson.parseArray("[" + SAMPLE_TOPIC_JSON + "]", ThreadPageInfo.class).get(0)));
    }

    @Test
    public void olderTopicHistoryIsReadWithCoercion() {
        ThreadPageInfo topic =
                NgaJson.parseObject(
                        "{\"tid\":\"42\",\"page\":3,\"subject\":\"离线\",\"mirrorBoard\":true,\"anonymity\":1,"
                                + "\"historyVisitedAt\":\"5\"}",
                        ThreadPageInfo.class);
        assertEquals(42, topic.getTid());
        assertEquals(3, topic.getPage());
        assertEquals("离线", topic.getSubject());
        assertTrue(topic.isAnonymity());
        assertEquals(5, topic.getHistoryVisitedAt());
        // mirrorBoard 只有读取方法，读入时被忽略。
        assertFalse(topic.isMirrorBoard());
    }

    // ---- 搜索历史与版面收藏缓存 ----

    @Test
    public void stringListAndForumFavoriteCacheFormat() {
        assertEquals("[\"a\",\"中\"]", NgaJson.toJSONString(Arrays.asList("a", "中")));
        List<ForumFavoriteBoard> boards =
                Arrays.asList(new ForumFavoriteBoard(-7, 0, "版块", null), new ForumFavoriteBoard(0, 42, "合集", "置顶"));
        String cache = ForumFavoritesParser.INSTANCE.serialize(boards);
        assertEquals(
                "[{\"fid\":-7,\"stid\":0,\"name\":\"版块\"},{\"fid\":0,\"stid\":42,\"name\":\"合集\",\"topped_topic\":\"置顶\"}]",
                cache);
        assertEquals(boards, ForumFavoritesParser.INSTANCE.parseCache(cache));
    }

    // ---- 帖子数据绑定：ArticleConvertFactory / ArticleUtil 使用 toJavaObject ----

    @Test
    public void topicHeaderBindsOnlyExactPropertyNames() {
        ThreadPageInfo topic =
                NgaJson.toJavaObject(
                        NgaJson.parseObject(
                                "{\"tid\":12,\"fid\":-7,\"subject\":\"主题\",\"author\":\"作者\",\"authorid\":\"9\","
                                        + "\"postdate\":1700000000,\"type\":\"1024\",\"topic_misc\":\"m\",\"titlefont\":\"tf\","
                                        + "\"lastposter\":\"lp\",\"replies\":\"3\",\"quote_from\":0,\"this_visit_rows\":20}"),
                        ThreadPageInfo.class);
        assertEquals(12, topic.getTid());
        assertEquals(-7, topic.getFid());
        assertEquals("主题", topic.getSubject());
        assertEquals("作者", topic.getAuthor());
        assertEquals(1024, topic.getType());
        assertEquals(3, topic.getReplies());
        // 大小写或下划线不同的键不会绑定。
        assertEquals(0, topic.getAuthorId());
        assertEquals(0, topic.getPostDate());
        assertNull(topic.getTopicMisc());
        assertNull(topic.getTitleFont());
        assertNull(topic.getLastPoster());
    }

    @Test
    public void replyRowBindsWithCoercionAndNestedAttachments() {
        ThreadRowInfo row =
                NgaJson.toJavaObject(
                        NgaJson.parseObject(
                                "{\"tid\":\"12\",\"fid\":-7,\"author\":\"作者\",\"authorid\":\"9\",\"subject\":\"标题\","
                                        + "\"vote\":\"v\",\"postdate\":\"2024-01-02 03:04\",\"postdatetimestamp\":1700000000,"
                                        + "\"pid\":\"0\",\"isanonymous\":\"1\",\"alterinfo\":\"[E1]\",\"content\":\"正文\","
                                        + "\"lou\":\"3\",\"attachs\":{\"0\":{\"attachurl\":\"a.png\",\"type\":\"img\",\"size\":\"12\"}},"
                                        + "\"level\":\"lv\",\"yz\":\"1\",\"js_escap_avatar\":\"av\",\"muteTime\":\"0\","
                                        + "\"aurvrc\":\"10\",\"signature\":\"sig\",\"comment\":{\"0\":{\"pid\":5}},"
                                        + "\"hotReplies\":[\"1\"],\"from_client\":\"8 ios\",\"from_client_model\":\"m\","
                                        + "\"score\":\"4\",\"score_2\":2,\"_IsInBlackList\":true,\"recommend\":\"1,2\"}"),
                        ThreadRowInfo.class);
        assertEquals(12, row.getTid());
        assertEquals(9, row.getAuthorid());
        assertEquals(3, row.getLou());
        assertEquals(4, row.getScore());
        assertEquals(2, row.getScore_2());
        assertEquals(10, row.getAurvrc());
        assertEquals("作者", row.getAuthor());
        assertEquals("标题", row.getSubject());
        assertEquals("正文", row.getContent());
        assertEquals("2024-01-02 03:04", row.getPostdate());
        assertEquals("[E1]", row.getAlterinfo());
        assertEquals("av", row.getJs_escap_avatar());
        assertEquals("sig", row.getSignature());
        assertEquals("v", row.getVote());
        assertEquals("lv", row.getLevel());
        assertEquals("1", row.getYz());
        assertEquals("0", row.getMuteTime());
        assertEquals(Collections.singletonList("1"), row.hotReplies);
        Attachment attachment = row.getAttachs().get("0");
        assertEquals("a.png", attachment.getAttachurl());
        assertEquals("img", attachment.getType());
        assertEquals(12, attachment.getSize());
        // 以下键与属性名不一致，不会绑定，由转换代码另行读取。
        assertEquals(0, row.getFid());
        assertFalse(row.getISANONYMOUS());
        assertFalse(row.get_isInBlackList());
        assertNull(row.getFromClient());
        assertNull(row.getFromClientModel());
        assertNull(row.getComments());
    }

    @Test
    public void replyRowKeyMatchingIsCaseSensitive() {
        ThreadRowInfo row =
                NgaJson.toJavaObject(
                        NgaJson.parseObject(
                                "{\"FROM_CLIENT\":\"x\",\"fromClient\":\"y\",\"isAnonymous\":true,\"Lou\":2}"),
                        ThreadRowInfo.class);
        assertEquals("y", row.getFromClient());
        assertFalse(row.getISANONYMOUS());
        assertEquals(0, row.getLou());
    }

    @Test
    public void replyRowEmptyContainersBecomeNull() {
        ThreadRowInfo row =
                NgaJson.toJavaObject(
                        NgaJson.parseObject("{\"attachs\":\"\",\"comment\":\"\"}"), ThreadRowInfo.class);
        assertNull(row.getAttachs());
        assertNull(row.getComments());
    }

    @Test
    public void replyRowRejectsUnconvertibleValues() {
        for (String source :
                new String[] {
                    "{\"lou\":\"\"}", "{\"pid\":\"abc\"}", "{\"attachs\":[{\"attachurl\":\"a\"}]}"
                }) {
            try {
                NgaJson.toJavaObject(NgaJson.parseObject(source), ThreadRowInfo.class);
                fail("应拒绝：" + source);
            } catch (RuntimeException expected) {
                // 当前转换整页失败；替换时保持一致，放宽另行处理。
            }
        }
    }

    // ---- 两条绑定路径的类型转换差异 ----

    /** 绑定为 ThreadPageInfo 后的 tid|anonymity|subject，失败记为 "!"。 */
    private static String topicFields(String source, boolean strict) {
        try {
            ThreadPageInfo topic =
                    strict
                            ? NgaJson.toJavaObject(NgaJson.parseObject(source), ThreadPageInfo.class)
                            : NgaJson.parseObject(source, ThreadPageInfo.class);
            return topic.getTid() + "|" + topic.isAnonymity() + "|" + topic.getSubject();
        } catch (RuntimeException e) {
            return "!";
        }
    }

    @Test
    public void textAndTreeBindingCoerceScalarsDifferently() {
        // {输入, parseObject(text, Class) 的结果, toJavaObject 的结果}
        String[][] cases = {
            {"{\"tid\":\"\"}", "0|false|null", "!"},
            {"{\"tid\":null}", "0|false|null", "!"},
            {"{\"tid\":\"abc\"}", "!", "!"},
            {"{\"tid\":1.9}", "!", "1|false|null"},
            {"{\"tid\":3000000000}", "!", "-1294967296|false|null"},
            {"{\"tid\":true}", "!", "!"},
            {"{\"anonymity\":\"\"}", "0|false|null", "!"},
            {"{\"anonymity\":\"Y\"}", "!", "!"},
            {"{\"anonymity\":2}", "0|false|null", "0|false|null"},
            {"{\"anonymity\":1}", "0|true|null", "0|true|null"},
            {"{\"subject\":12}", "0|false|12", "0|false|12"},
            {"{\"subject\":{\"a\":1}}", "0|false|{\"a\":1}", "0|false|{\"a\":1}"},
            {"{\"subject\":[1]}", "0|false|[1]", "0|false|[1]"},
            {"{\"TID\":5}", "5|false|null", "0|false|null"},
            {"{\"t_i_d\":5}", "5|false|null", "0|false|null"},
            {"{\"t-id\":5}", "5|false|null", "0|false|null"},
        };
        for (String[] c : cases) {
            assertEquals("parseObject " + c[0], c[1], topicFields(c[0], false));
            assertEquals("toJavaObject " + c[0], c[2], topicFields(c[0], true));
        }
        assertEquals(7, NgaJson.parseObject("{\"historyvisitedat\":7}", ThreadPageInfo.class).getHistoryVisitedAt());
    }

    @Test
    public void nestedObjectsAndContainers() {
        for (boolean strict : new boolean[] {false, true}) {
            assertNull(topic("{\"replyInfo\":\"\"}", strict).getReplyInfo());
            assertEquals("5", topic("{\"replyInfo\":{\"pidStr\":5}}", strict).getReplyInfo().getPidStr());
            assertEquals(Arrays.asList("1", null), row("{\"hotReplies\":[1,null]}", strict).hotReplies);
            assertNull(row("{\"hotReplies\":\"\"}", strict).hotReplies);
            ThreadRowInfo empty = row("{\"attachs\":{\"0\":\"\"}}", strict);
            assertTrue(empty.getAttachs().containsKey("0"));
            assertNull(empty.getAttachs().get("0"));
            assertEquals(0, row("{\"attachs\":{}}", strict).getAttachs().size());
            assertEquals(2, row("{\"comments\":[{\"lou\":2}]}", strict).getComments().get(0).getLou());
            assertEquals(1.5f, row("{\"reputation\":\"1.5\"}", strict).getReputation(), 0);
            assertNull(row("{\"content\":null}", strict).getContent());
            try {
                topic("{\"replyInfo\":[]}", strict);
                fail("对象属性应拒绝数组");
            } catch (RuntimeException expected) {
            }
        }
        // 基本类型遇到 null：文本绑定保留默认值，树形绑定失败。
        assertEquals(0, row("{\"lou\":null}", false).getLou());
        try {
            row("{\"lou\":null}", true);
            fail("toJavaObject 应拒绝 null 写入 int");
        } catch (RuntimeException expected) {
        }
        // 文本绑定可通过读取方法填充没有写入方法的集合。
        assertEquals(Collections.singletonList("a"), row("{\"imageUrls\":[\"a\"]}", false).getImageUrls());
    }

    private static ThreadPageInfo topic(String source, boolean strict) {
        return strict
                ? NgaJson.toJavaObject(NgaJson.parseObject(source), ThreadPageInfo.class)
                : NgaJson.parseObject(source, ThreadPageInfo.class);
    }

    private static ThreadRowInfo row(String source, boolean strict) {
        return strict
                ? NgaJson.toJavaObject(NgaJson.parseObject(source), ThreadRowInfo.class)
                : NgaJson.parseObject(source, ThreadRowInfo.class);
    }

    @Test
    public void typedDocumentEdges() {
        List<FilterKeyword> keywords = NgaJson.parseArray("[null,{\"keyword\":\"k\"}]", FilterKeyword.class);
        assertNull(keywords.get(0));
        assertEquals("k", keywords.get(1).getKeyword());
        assertNull(NgaJson.parseObject("", ThreadPageInfo.class));
        try {
            NgaJson.parseObject("[]", ThreadPageInfo.class);
            fail("实体不应从数组绑定");
        } catch (RuntimeException expected) {
        }
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> map = NgaJson.parseObject("{\"b\":{\"x\":\"1\"},\"a\":2}", java.util.Map.class);
        assertEquals(2, map.get("a"));
        assertEquals("1", ((java.util.Map<?, ?>) map.get("b")).get("x"));
    }
}
