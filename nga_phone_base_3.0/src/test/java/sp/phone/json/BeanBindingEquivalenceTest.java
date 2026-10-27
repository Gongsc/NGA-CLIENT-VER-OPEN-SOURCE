package sp.phone.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import gov.anzong.androidnga.base.json.NgaJson;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import sp.phone.common.FilterKeyword;
import sp.phone.common.User;
import sp.phone.http.bean.CategoryBean;
import sp.phone.http.bean.TopicListBean;
import sp.phone.http.bean.TopicPostBean;
import sp.phone.mvp.model.ForumsListModel;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.task.ReportTask;

/**
 * 迁移校验：同一份文本分别用 fastjson 与 {@link NgaJson} 绑定为业务实体，结果须一致。
 *
 * <p>两个结果统一用 fastjson 序列化后再解析为树比较，排除 HashMap 键顺序的影响，只比较绑定得到的内容。删除 fastjson 时，
 * 本类改为与固定的期望文本比较或移除。
 */
public class BeanBindingEquivalenceTest {

    private static final String TOPIC_LIST =
            "{\"data\":{\"__CU\":{\"uid\":\"9\",\"group_bit\":622816,\"admincheck\":\"\",\"rvrc\":-10},"
                    + "\"__GLOBAL\":\"./template/js/nga_global.xml\","
                    + "\"__F\":{\"topped_topic\":\"\",\"sub_forums\":{\"t1\":{\"0\":\"42\",\"1\":\"子版\",\"2\":\"说明\",\"3\":\"7\",\"4\":1}},"
                    + "\"fid\":\"-7\",\"name\":\"测试版面\"},"
                    + "\"__ROWS\":2,"
                    + "\"__T\":{\"0\":{\"tid\":11915941,\"fid\":275,\"quote_from\":0,\"quote_to\":\"\",\"topic_misc\":\"\","
                    + "\"author\":\"作者\",\"authorid\":9,\"subject\":\"主题\",\"type\":516,\"postdate\":1498529634,"
                    + "\"lastpost\":1499236460,\"lastposter\":\"lp\",\"replies\":\"13\",\"lastmodify\":1500443085,"
                    + "\"recommend\":1,\"titlefont\":\"\",\"admin_ui\":1,\"tpcurl\":\"/read.php?tid=1\","
                    + "\"parent\":{\"0\":275,\"2\":\"测试版面\"},\"topic_misc_var\":{\"3\":-1459709,\"1\":32},"
                    + "\"__P\":{\"tid\":1,\"pid\":2,\"authorid\":9,\"type\":0,\"postdate\":1,\"subject\":\"s\",\"content\":\"c\"}},"
                    + "\"1\":{\"tid\":\"2\",\"fid\":-7,\"author\":\"#anony_x\",\"authorid\":\"#anony_x\",\"subject\":\"匿名\","
                    + "\"type\":0,\"postdate\":\"\",\"replies\":0,\"extra\":\"忽略\"}},"
                    + "\"__T__ROWS\":2,\"__T__ROWS_PAGE\":35,\"__R__ROWS_PAGE\":20},\"encode\":\"gbk\",\"time\":1511446433}";

    private static final String TOPIC_POST =
            "{\"data\":{\"action\":\"new\",\"fid\":-7,\"auth\":\"x\",\"if_moderator\":0,\"tid\":123,"
                    + "\"__CU\":{\"uid\":9,\"group_bit\":\"1\",\"admincheck\":\"\",\"rvrc\":0},\"__GLOBAL\":\"g\","
                    + "\"__F\":{\"bit_data\":1,\"fid\":\"-7\",\"name\":\"版\"},\"attach_url\":\"https://img/upload\"},"
                    + "\"encode\":\"utf-8\",\"time\":1,\"debug\":{\"a\":[1,\"x\"]}}";

    private static final String FORUMS =
            "{\"code\":0,\"msg\":\"ok\",\"result\":[{\"id\":\"1\",\"name\":\"分区\",\"groups\":[{\"id\":\"g\",\"name\":\"组\","
                    + "\"forums\":[{\"id\":\"-7\",\"name\":\"版\",\"stid\":0,\"extra\":1},{\"id\":7,\"name\":\"x\",\"stid\":\"42\"}]}]}]}";

    @Test
    public void responseBeansBindIdentically() throws Exception {
        assertSame(TopicListBean.class, TOPIC_LIST);
        assertSame(TopicPostBean.class, TOPIC_POST);
        assertSame(ForumsListModel.class, FORUMS);
        assertSame(
                ReportTask.ResultBean.class,
                "{\"error\":{\"0\":\"你在217秒后方可举报\"},\"data\":{\"0\":\"操作成功\",\"1\":2},\"time\":\"1601530856\"}");
        Class<?> upload = Class.forName("sp.phone.task.AvatarFileUploadTask$NonameUploadResponse");
        assertSame(upload, "{\"error\":false,\"errorinfo\":\"\",\"data\":\"https://img/a.jpg\"}");
        assertSame(upload, "{\"error\":1,\"errorinfo\":\"上传失败\"}");
    }

    @Test
    public void bundledCategoriesBindIdentically() throws IOException {
        for (String name : new String[] {"category.json", "category_old.json"}) {
            String text =
                    new String(
                            Files.readAllBytes(Paths.get("src/main/assets/json", name)), StandardCharsets.UTF_8);
            List<CategoryBean> expected = JsonBackend.FASTJSON.parseArray(text, CategoryBean.class);
            List<CategoryBean> actual = JsonBackend.NGA_JSON.parseArray(text, CategoryBean.class);
            assertEquals(name, canonical(expected), canonical(actual));
        }
    }

    @Test
    public void storedRecordsBindIdentically() {
        assertSameList(
                User.class,
                "[{\"avatarUrl\":\"a\",\"cid\":\"c\",\"mAvatarUrl\":\"a\",\"mCid\":\"c\",\"mNickName\":\"n\",\"mUserId\":\"1\","
                        + "\"nickName\":\"n\",\"userId\":\"1\"},{\"userid\":2,\"nick_name\":\"m\"}]");
        assertSameList(FilterKeyword.class, "[{\"enabled\":true,\"keyword\":\"词\"},{\"keyword\":\"k\",\"enabled\":\"0\"},null]");
        assertSameList(String.class, "[\"a\",1,null,\"中\"]");
        String topic =
                "{\"anonymity\":true,\"author\":\"作者\",\"authorId\":9,\"board\":\"版面\",\"fid\":-7,"
                        + "\"historyVisitedAt\":1700000000123,\"lastPoster\":\"lp\",\"mirrorBoard\":true,\"page\":2,"
                        + "\"pid\":77,\"position\":5,\"postDate\":1700000000,\"replies\":3,\"replyInfo\":{\"authorId\":\"9\","
                        + "\"content\":\"回复\",\"pidStr\":\"77\",\"postDate\":\"2024\",\"subject\":\"rs\",\"tidStr\":\"42\"},"
                        + "\"subject\":\"主题\",\"tid\":\"42\",\"titleFont\":\"tf\",\"topicMisc\":\"misc\",\"type\":1024}";
        assertSame(ThreadPageInfo.class, topic);
        assertSameList(ThreadPageInfo.class, "[" + topic + ",{\"tid\":1}]");
    }

    private static void assertSame(Class<?> type, String text) {
        assertEquals(
                type.getSimpleName(),
                canonical(JsonBackend.FASTJSON.parseObject(text, type)),
                canonical(JsonBackend.NGA_JSON.parseObject(text, type)));
    }

    private static void assertSameList(Class<?> type, String text) {
        assertEquals(
                type.getSimpleName(),
                canonical(JsonBackend.FASTJSON.parseArray(text, type)),
                canonical(JsonBackend.NGA_JSON.parseArray(text, type)));
    }

    /** 用同一序列化器输出，再解析为保持顺序无关比较的树。 */
    private static Object canonical(Object bound) {
        String text = JsonBackend.FASTJSON.toJSONString(bound);
        assertTrue("绑定结果为空：" + text, text.length() > 16);
        return Arrays.asList(NgaJson.parse(text), text.length());
    }
}
