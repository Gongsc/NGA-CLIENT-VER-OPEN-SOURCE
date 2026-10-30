package gov.anzong.androidnga.json;

import static org.junit.Assert.assertEquals;

import gov.anzong.androidnga.base.json.NgaJson;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;

import gov.anzong.androidnga.network.bean.CategoryBean;

/**
 * 迁移校验：接口实体、内置版面列表与本地记录的绑定结果。
 *
 * <p>期望值在删除 fastjson 前生成，生成时已确认与 fastjson 的绑定结果一致（树形比较）。内置版面列表较大，记录序列化结果的
 * SHA-256。
 */
public class BeanBindingGoldenTest {

    /** {实体类, obj 或 list, 输入, NgaJson 序列化后的绑定结果} */
    private static final String[][] CASES = {
            {"gov.anzong.androidnga.network.bean.TopicListBean", "obj",
                "{\"data\":{\"__CU\":{\"uid\":\"9\",\"group_bit\":622816,\"admincheck\":\"\",\"rvrc\":-10},\"__GLOBAL\":\"./template/js/nga_global.xml\",\"__F\":{\"topped_topic\":\"\",\"sub_forums\":{\"t1\":{\"0\":\"42\",\"1\":\"子版\",\"2\":\"说明\",\"3\":\"7\",\"4\":1}},\"fid\":\"-7\",\"name\":\"测试版面\"},\"__ROWS\":2,\"__T\":{\"0\":{\"tid\":11915941,\"fid\":275,\"quote_from\":0,\"quote_to\":\"\",\"topic_misc\":\"\",\"author\":\"作者\",\"authorid\":9,\"subject\":\"主题\",\"type\":516,\"postdate\":1498529634,\"lastpost\":1499236460,\"lastposter\":\"lp\",\"replies\":\"13\",\"lastmodify\":1500443085,\"recommend\":1,\"titlefont\":\"\",\"admin_ui\":1,\"tpcurl\":\"/read.php?tid=1\",\"parent\":{\"0\":275,\"2\":\"测试版面\"},\"topic_misc_var\":{\"3\":-1459709,\"1\":32},\"__P\":{\"tid\":1,\"pid\":2,\"authorid\":9,\"type\":0,\"postdate\":1,\"subject\":\"s\",\"content\":\"c\"}},\"1\":{\"tid\":\"2\",\"fid\":-7,\"author\":\"#anony_x\",\"authorid\":\"#anony_x\",\"subject\":\"匿名\",\"type\":0,\"postdate\":\"\",\"replies\":0,\"extra\":\"忽略\"}},\"__T__ROWS\":2,\"__T__ROWS_PAGE\":35,\"__R__ROWS_PAGE\":20},\"encode\":\"gbk\",\"time\":1511446433}",
                "{\"data\":{\"_CU\":{\"admincheck\":\"\",\"group_bit\":622816,\"rvrc\":-10,\"uid\":9},\"_F\":{\"fid\":-7,\"name\":\"测试版面\",\"sub_forums\":\"{\\\"t1\\\":{\\\"0\\\":\\\"42\\\",\\\"1\\\":\\\"子版\\\",\\\"2\\\":\\\"说明\\\",\\\"3\\\":\\\"7\\\",\\\"4\\\":1}}\",\"topped_topic\":\"\"},\"_GLOBAL\":\"./template/js/nga_global.xml\",\"_ROWS\":\"2\",\"_R__ROWS_PAGE\":20,\"_T\":{\"0\":{\"_P\":{\"authorid\":\"9\",\"content\":\"c\",\"pid\":2,\"postdate\":1,\"subject\":\"s\",\"tid\":1,\"type\":0},\"admin_ui\":1,\"author\":\"作者\",\"authorid\":\"9\",\"fid\":275,\"lastmodify\":1500443085,\"lastpost\":1499236460,\"lastposter\":\"lp\",\"parent\":{\"0\":\"275\",\"2\":\"测试版面\"},\"postdate\":1498529634,\"quote_from\":0,\"quote_to\":\"\",\"recommend\":1,\"replies\":13,\"subject\":\"主题\",\"tid\":11915941,\"titlefont\":\"\",\"topic_misc\":\"\",\"topic_misc_var\":{\"3\":\"-1459709\",\"1\":\"32\"},\"tpcurl\":\"/read.php?tid=1\",\"type\":516},\"1\":{\"admin_ui\":0,\"author\":\"#anony_x\",\"authorid\":\"#anony_x\",\"fid\":-7,\"lastmodify\":0,\"lastpost\":0,\"postdate\":0,\"quote_from\":0,\"recommend\":0,\"replies\":0,\"subject\":\"匿名\",\"tid\":2,\"type\":0}},\"_T__ROWS\":2,\"_T__ROWS_PAGE\":35},\"encode\":\"gbk\",\"time\":1511446433}"},
            {"gov.anzong.androidnga.mvvm.model.ForumsListModel", "obj",
                "{\"code\":0,\"msg\":\"ok\",\"result\":[{\"id\":\"1\",\"name\":\"分区\",\"groups\":[{\"id\":\"g\",\"name\":\"组\",\"forums\":[{\"id\":\"-7\",\"name\":\"版\",\"stid\":0,\"extra\":1},{\"id\":7,\"name\":\"x\",\"stid\":\"42\"}]}]}]}",
                "{\"code\":0,\"msg\":\"ok\",\"result\":[{\"groups\":[{\"forums\":[{\"id\":-7,\"name\":\"版\",\"stid\":0},{\"id\":7,\"name\":\"x\",\"stid\":42}],\"id\":\"g\",\"name\":\"组\"}],\"id\":\"1\",\"name\":\"分区\"}]}"},
            {"gov.anzong.androidnga.account.User", "list",
                "[{\"avatarUrl\":\"a\",\"cid\":\"c\",\"mAvatarUrl\":\"a\",\"mCid\":\"c\",\"mNickName\":\"n\",\"mUserId\":\"1\",\"nickName\":\"n\",\"userId\":\"1\"},{\"userid\":2,\"nick_name\":\"m\"}]",
                "[{\"avatarUrl\":\"a\",\"cid\":\"c\",\"mAvatarUrl\":\"a\",\"mCid\":\"c\",\"mNickName\":\"n\",\"mUserId\":\"1\",\"nickName\":\"n\",\"userId\":\"1\"},{\"mNickName\":\"m\",\"mUserId\":\"2\",\"nickName\":\"m\",\"userId\":\"2\"}]"},
            {"gov.anzong.androidnga.settings.FilterKeyword", "list",
                "[{\"enabled\":true,\"keyword\":\"词\"},{\"keyword\":\"k\",\"enabled\":\"0\"},null]",
                "[{\"enabled\":true,\"keyword\":\"词\"},{\"enabled\":false,\"keyword\":\"k\"},null]"},
            {"java.lang.String", "list",
                "[\"a\",1,null,\"中\"]",
                "[\"a\",\"1\",null,\"中\"]"},
            {"gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo", "obj",
                "{\"anonymity\":true,\"author\":\"作者\",\"authorId\":9,\"board\":\"版面\",\"fid\":-7,\"historyVisitedAt\":1700000000123,\"lastPoster\":\"lp\",\"mirrorBoard\":true,\"page\":2,\"pid\":77,\"position\":5,\"postDate\":1700000000,\"replies\":3,\"replyInfo\":{\"authorId\":\"9\",\"content\":\"回复\",\"pidStr\":\"77\",\"postDate\":\"2024\",\"subject\":\"rs\",\"tidStr\":\"42\"},\"subject\":\"主题\",\"tid\":\"42\",\"titleFont\":\"tf\",\"topicMisc\":\"misc\",\"type\":1024}",
                "{\"anonymity\":true,\"author\":\"作者\",\"authorId\":9,\"board\":\"版面\",\"fid\":-7,\"historyVisitedAt\":1700000000123,\"lastPoster\":\"lp\",\"mirrorBoard\":false,\"page\":2,\"pid\":77,\"position\":5,\"postDate\":1700000000,\"replies\":3,\"replyInfo\":{\"authorId\":\"9\",\"content\":\"回复\",\"pidStr\":\"77\",\"postDate\":\"2024\",\"subject\":\"rs\",\"tidStr\":\"42\"},\"subject\":\"主题\",\"tid\":42,\"titleFont\":\"tf\",\"topicMisc\":\"misc\",\"type\":1024}"},
            {"gov.anzong.androidnga.mvvm.model.entity.ThreadPageInfo", "list",
                "[{\"anonymity\":true,\"author\":\"作者\",\"authorId\":9,\"board\":\"版面\",\"fid\":-7,\"historyVisitedAt\":1700000000123,\"lastPoster\":\"lp\",\"mirrorBoard\":true,\"page\":2,\"pid\":77,\"position\":5,\"postDate\":1700000000,\"replies\":3,\"replyInfo\":{\"authorId\":\"9\",\"content\":\"回复\",\"pidStr\":\"77\",\"postDate\":\"2024\",\"subject\":\"rs\",\"tidStr\":\"42\"},\"subject\":\"主题\",\"tid\":\"42\",\"titleFont\":\"tf\",\"topicMisc\":\"misc\",\"type\":1024},{\"tid\":1}]",
                "[{\"anonymity\":true,\"author\":\"作者\",\"authorId\":9,\"board\":\"版面\",\"fid\":-7,\"historyVisitedAt\":1700000000123,\"lastPoster\":\"lp\",\"mirrorBoard\":false,\"page\":2,\"pid\":77,\"position\":5,\"postDate\":1700000000,\"replies\":3,\"replyInfo\":{\"authorId\":\"9\",\"content\":\"回复\",\"pidStr\":\"77\",\"postDate\":\"2024\",\"subject\":\"rs\",\"tidStr\":\"42\"},\"subject\":\"主题\",\"tid\":42,\"titleFont\":\"tf\",\"topicMisc\":\"misc\",\"type\":1024},{\"anonymity\":false,\"authorId\":0,\"fid\":0,\"historyVisitedAt\":0,\"mirrorBoard\":false,\"page\":0,\"pid\":0,\"position\":0,\"postDate\":0,\"replies\":0,\"tid\":1,\"type\":0}]"},
    };

    @Test
    public void entitiesBindAsRecorded() throws ClassNotFoundException {
        for (String[] c : CASES) {
            Class<?> type = Class.forName(c[0]);
            Object bound = "list".equals(c[1]) ? NgaJson.parseArray(c[2], type) : NgaJson.parseObject(c[2], type);
            assertEquals(c[0] + " " + c[2], c[3], NgaJson.toJSONString(bound));
        }
    }

    @Test
    public void bundledCategoriesBindAsRecorded() throws Exception {
        String[][] files = {
            {"category.json", "5", "11962", "2be815523b85276835799f620d0ca92b3eb786d2907fdaa73f26c37641131d01"},
            {"category_old.json", "8", "143", "b04fb011b1127e88f9da579aa62da6ba1bdbe2894c9082c27e1b5d5940df488b"},
        };
        for (String[] f : files) {
            String text =
                    new String(Files.readAllBytes(Paths.get("src/main/assets/json", f[0])), StandardCharsets.UTF_8);
            List<CategoryBean> categories = NgaJson.parseArray(text, CategoryBean.class);
            String bound = NgaJson.toJSONString(categories);
            assertEquals(f[0], Integer.parseInt(f[1]), categories.size());
            assertEquals(f[0], Integer.parseInt(f[2]), bound.length());
            assertEquals(f[0], f[3], sha256(bound));
        }
    }

    private static String sha256(String text) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
