package sp.phone.http.article;

/** 主题响应解码的合成输入，覆盖 JSON、XML 与网页三种来源及其兼容处理。 */
final class DecodeGoldenInputs {

    private DecodeGoldenInputs() {}

    static final String JSON_QUIRKS =
            "﻿/*$js$*/{\"data\":{\"__T\":{\"tid\":12,\"fid\":-7,\"subject\":\"主题\",\"authorid\":9,\"postdate\":1700000000},"
                    + "\"__R\":{\"0\":{\"pid\":0,\"lou\":0,\"authorid\":9,\"content\":0123,\"subject\":+45,\"author\":007,"
                    + "\"postdate\":\"2024-01-02 03:04\",\"attachs\":{\"0\":{\"attachurl\":\"a.png\",\"type\":\"img\",\"size\":12}},"
                    + "\"hotreply\":{\"1\":{\"pid\":99,\"content\":\"热点\",\"authorid\":8},\"0\":{\"pid\":98,\"subject\":\"热点二\"}},"
                    + "\"from_client\":\"8 iOS\",\"score\":3,\"comment\":{\"0\":{\"pid\":5,\"content\":\"评\",\"authorid\":8}}},"
                    + "\"1\":{\"pid\":34,\"lou\":1,\"authorid\":8,\"content\":\"回复\",\"recommend\":\"1,2\",\"alterinfo\":\"[E1 2 3]\"}},"
                    + "\"__U\":{\"9\":{\"uid\":9,\"username\":\"用户\",\"avatar\":\"av\",\"rvrc\":10},\"8\":{\"uid\":\"8\",\"username\":\"另一人\"},"
                    + "\"__GROUPS\":{\"1\":\"会员\"}},"
                    + "\"__ROWS\":21,\"__R__ROWS\":2,\"__R__ROWS_PAGE\":20},\"encode\":\"gbk\",\"time\":1}"
                    + "/*error fill content*/{\"ignored\":";

    static final String JSON_EMPTY_CONTAINERS =
            "{\"data\":{\"__T\":{\"tid\":12},\"__R\":{},\"__U\":[],\"__ROWS\":0,\"__R__ROWS\":0,\"big\":12345678901,\"dec\":1.50}}";

    static final String JSON_ROOT_WITHOUT_DATA = "{\"__T\":{\"tid\":3},\"__R\":{\"0\":{\"pid\":1}},\"__ROWS\":1}";

    static final String JSON_ERROR_OBJECT = "{\"error\":{\"0\":\"错误零\",\"1\":\"无权访问\"},\"time\":1}";

    static final String JSON_ERROR_ARRAY = "{\"data\":{\"__MESSAGE\":[\"\",\"帖子已删除\"]}}";

    static final String JSON_ERROR_TEXT = "{\"error\":\"文本错误\"}";

    static final String XML =
            "<?xml version='1.0' encoding='utf-8'?><root><__T><tid>12</tid><subject><![CDATA[主题 & 标题]]></subject></__T>"
                    + "<__R><item><pid>0</pid><lou>0</lou><content><![CDATA[正文 <br> 内容]]></content><comment></comment>"
                    + "<attachs><item><attachurl>a.png</attachurl></item></attachs></item>"
                    + "<item><pid>34</pid><lou>1</lou><content>回复</content></item></__R>"
                    + "<__U><item><uid>56</uid><username>用户</username></item><item><username>无编号</username></item>"
                    + "<__GROUPS><_1>会员</_1><_-1>游客</_-1></__GROUPS><buffs></buffs></__U>"
                    + "<__ROWS>21</__ROWS></root>";

    static final String XML_EMPTY_ROWS = "<root><__T><tid>5</tid></__T><__R></__R><__U></__U><__ROWS>0</__ROWS></root>";

    static final String HTML_PLAIN = ArticleHtmlResponseTest.HTML;

    static final String HTML_ATTACHMENTS =
            ArticleHtmlResponseTest.HTML.replace(
                            "</td>",
                            "<script>ubbcode.attach.load($('postattach20'),"
                                    + " [{url:'a/test.png',type:'img',thumb:'1',name:'a,b'}]);</script></td>")
                    .replace(
                            "<!--postlistend-->",
                            "<script>commonui.loadAlertInfo('编辑说明','alertc20');var"
                                    + " __PAGE={0:'/read.php?tid=12',1:1,2:1,3:20};</script><!--postlistend-->");

    static final String HTML_HOT_REPLY =
            ArticleHtmlResponseTest.HTML.replace("postcontainer20", "postcontainer0")
                    .replace("postdate20", "postdate0")
                    .replace("postsubject20", "postsubject0")
                    .replace("postcontent20", "postcontent0")
                    .replace("proc(20,", "proc(0,")
                    .replace("34,0,null", "0,0,null")
                    .replace(
                            "</td>",
                            "<span id='hightlight_for_0'><span id='postcommentsubject__99'></span><span"
                                    + " id='postcomment__99'>热点正文 [b]加粗[/b]</span><span"
                                    + " id='commentInfo__99'>2026-10-26</span></span></td>")
                    .replace(
                            "<!--postlistend-->",
                            "<script>commonui.postArg.proc('__99',null,null,null,null,null,null,null,null,null,99,0,null,'9',"
                                    + "1792995370,'18,0,0','18','','','7 iOS','',null,0);</script><!--postlistend-->");

    static final String[] ALL = {
        JSON_QUIRKS, JSON_EMPTY_CONTAINERS, JSON_ROOT_WITHOUT_DATA, JSON_ERROR_OBJECT, JSON_ERROR_ARRAY,
        JSON_ERROR_TEXT, XML, XML_EMPTY_ROWS, HTML_PLAIN, HTML_ATTACHMENTS, HTML_HOT_REPLY
    };
}
