package gov.anzong.androidnga.network.article;

import static org.junit.Assert.assertEquals;

import gov.anzong.androidnga.base.json.NgaJson;

import org.junit.Test;

/**
 * 迁移校验：主题响应解码结果。期望值由迁移前的 fastjson 实现生成。
 *
 * <p>fastjson 部分对象不保持键顺序，因此把期望与实际都解析为树后比较，键顺序不参与比较。
 */
public class ArticleDecodeGoldenTest {

    /** {输入, isIncomplete, businessError, decode 结果} */
    private static final Object[][] CASES = {
            {DecodeGoldenInputs.JSON_QUIRKS, "false", null,
                "{\"__R\":{\"0\":{\"attachs\":{\"0\":{\"attachurl\":\"a.png\",\"size\":12,\"type\":\"img\"}},\"author\":\"007\",\"authorid\":9,\"comment\":{\"0\":{\"authorid\":8,\"content\":\"评\",\"pid\":5}},\"content\":\"0123\",\"from_client\":\"8 iOS\",\"hotreply\":{\"0\":{\"pid\":98,\"subject\":\"热点二\"},\"1\":{\"authorid\":8,\"content\":\"热点\",\"pid\":99}},\"lou\":0,\"pid\":0,\"postdate\":\"2024-01-02 03:04\",\"score\":3,\"subject\":\"+45\"},\"1\":{\"alterinfo\":\"[E1 2 3]\",\"authorid\":8,\"content\":\"回复\",\"lou\":1,\"pid\":34,\"recommend\":\"1,2\"}},\"__ROWS\":21,\"__R__ROWS\":2,\"__R__ROWS_PAGE\":20,\"__T\":{\"authorid\":9,\"fid\":-7,\"postdate\":1700000000,\"subject\":\"主题\",\"tid\":12},\"__U\":{\"8\":{\"uid\":\"8\",\"username\":\"另一人\"},\"9\":{\"avatar\":\"av\",\"rvrc\":10,\"uid\":9,\"username\":\"用户\"},\"__GROUPS\":{\"1\":\"会员\"}}}"},
            {DecodeGoldenInputs.JSON_EMPTY_CONTAINERS, "false", null,
                "{\"__R\":{},\"__ROWS\":0,\"__R__ROWS\":0,\"__T\":{\"tid\":12},\"__U\":[],\"big\":12345678901,\"dec\":1.50}"},
            {DecodeGoldenInputs.JSON_ROOT_WITHOUT_DATA, "false", null,
                "{\"__R\":{\"0\":{\"pid\":1}},\"__ROWS\":1,\"__T\":{\"tid\":3}}"},
            {DecodeGoldenInputs.JSON_ERROR_OBJECT, "false", "无权访问",
                "{\"error\":{\"0\":\"错误零\",\"1\":\"无权访问\"},\"time\":1}"},
            {DecodeGoldenInputs.JSON_ERROR_ARRAY, "false", "帖子已删除",
                "{\"__MESSAGE\":[\"\",\"帖子已删除\"]}"},
            {DecodeGoldenInputs.JSON_ERROR_TEXT, "false", "文本错误",
                "{\"error\":\"文本错误\"}"},
            {DecodeGoldenInputs.XML, "false", null,
                "{\"__T\":{\"tid\":\"12\",\"subject\":\"主题 & 标题\"},\"__R\":{\"0\":{\"pid\":\"0\",\"lou\":\"0\",\"content\":\"正文 <br> 内容\",\"comment\":{},\"attachs\":{\"0\":{\"attachurl\":\"a.png\"}}},\"1\":{\"pid\":\"34\",\"lou\":\"1\",\"content\":\"回复\"}},\"__U\":{\"56\":{\"uid\":\"56\",\"username\":\"用户\"},\"1\":{\"username\":\"无编号\"},\"__GROUPS\":{\"1\":\"会员\",\"-1\":\"游客\"},\"buffs\":{}},\"__ROWS\":\"21\",\"__R__ROWS\":2}"},
            {DecodeGoldenInputs.XML_EMPTY_ROWS, "false", null,
                "{\"__T\":{\"tid\":\"5\"},\"__R\":{},\"__U\":{},\"__ROWS\":\"0\",\"__R__ROWS\":0}"},
            {DecodeGoldenInputs.HTML_PLAIN, "false", null,
                "{\"__R\":{\"0\":{\"authorid\":9,\"content\":\"正文 [b]内容[/b]<br><span>括号 (a,b) &amp; 引号</span>\\n<img src=\\\"https://img.nga.cn/a.png\\\">\",\"fid\":-7,\"from_client\":\"7 iOS (a,b)'x\",\"lou\":20,\"pid\":34,\"postdate\":\"2026-10-26 14:16\",\"postdatetimestamp\":1792995370,\"recommend\":\"0\",\"score\":\"2\",\"score_2\":\"1\",\"subject\":\"\",\"tid\":12,\"type\":0}},\"__ROWS\":43,\"__R__ROWS\":1,\"__T\":{\"author\":\"测试用户\",\"authorid\":9,\"fid\":-7,\"replies\":42,\"subject\":\"主题 & 标题\",\"tid\":12,\"type\":4096},\"__U\":{\"9\":{\"uid\":9,\"username\":\"测试用户\"}}}"},
            {DecodeGoldenInputs.HTML_ATTACHMENTS, "false", null,
                "{\"__R\":{\"0\":{\"alterinfo\":\"编辑说明\",\"attachs\":{\"0\":{\"attachurl\":\"a/test.png\",\"name\":\"a,b\",\"subid\":0,\"thumb\":\"1\",\"type\":\"img\",\"url\":\"a/test.png\"}},\"authorid\":9,\"content\":\"正文 [b]内容[/b]<br><span>括号 (a,b) &amp; 引号</span>\\n<img src=\\\"https://img.nga.cn/a.png\\\">\",\"fid\":-7,\"from_client\":\"7 iOS (a,b)'x\",\"lou\":20,\"pid\":34,\"postdate\":\"2026-10-26 14:16\",\"postdatetimestamp\":1792995370,\"recommend\":\"0\",\"score\":\"2\",\"score_2\":\"1\",\"subject\":\"\",\"tid\":12,\"type\":0}},\"__ROWS\":1,\"__R__ROWS\":1,\"__T\":{\"author\":\"测试用户\",\"authorid\":9,\"fid\":-7,\"replies\":42,\"subject\":\"主题 & 标题\",\"tid\":12,\"type\":4096},\"__U\":{\"9\":{\"uid\":9,\"username\":\"测试用户\"}}}"},
            {DecodeGoldenInputs.HTML_HOT_REPLY, "false", null,
                "{\"__R\":{\"0\":{\"authorid\":9,\"content\":\"正文 [b]内容[/b]<br><span>括号 (a,b) &amp; 引号</span>\\n<img src=\\\"https://img.nga.cn/a.png\\\">\",\"fid\":-7,\"from_client\":\"7 iOS (a,b)'x\",\"hotreply\":{\"0\":{\"authorid\":9,\"content\":\"热点正文 [b]加粗[/b]\",\"fid\":-7,\"from_client\":\"7 iOS\",\"lou\":-1,\"pid\":99,\"postdate\":\"2026-10-26\",\"postdatetimestamp\":1792995370,\"score\":\"18\",\"score_2\":\"0\",\"subject\":\"\",\"tid\":12,\"type\":0}},\"lou\":0,\"pid\":0,\"postdate\":\"2026-10-26 14:16\",\"postdatetimestamp\":1792995370,\"recommend\":\"0\",\"score\":\"2\",\"score_2\":\"1\",\"subject\":\"\",\"tid\":12,\"type\":0}},\"__ROWS\":43,\"__R__ROWS\":1,\"__T\":{\"author\":\"测试用户\",\"authorid\":9,\"fid\":-7,\"replies\":42,\"subject\":\"主题 & 标题\",\"tid\":12,\"type\":4096},\"__U\":{\"9\":{\"uid\":9,\"username\":\"测试用户\"}}}"},
    };

    @Test
    public void decodedTreesMatchRecordedOutput() throws Exception {
        for (Object[] c : CASES) {
            String input = (String) c[0];
            assertEquals(Boolean.parseBoolean((String) c[1]), ArticleResponse.isIncomplete(input));
            String actual = ArticleResponse.decode(input).toJSONString();
            assertEquals(actual, NgaJson.parse((String) c[3]), NgaJson.parse(actual));
            assertEquals(c[2], ArticleResponse.businessError(ArticleResponse.decode(input)));
        }
    }
}
