package gov.anzong.androidnga.core.decode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForumEmoticonDecoderTest {

    private final ForumEmoticonDecoder decoder = new ForumEmoticonDecoder();

    @Test
    public void knownEmoticonsMapToBundledImages() {
        assertEquals(
                "前<img class='emoticon invertFilter' src='file:///android_asset/ac/-47218_5052bca81a77f.png'>后",
                decoder.decode("前[s:ac:blink]后"));
        assertEquals("<img src='file:///android_asset/pg/pg01.png'>", decoder.decode("[s:pg:战斗力]"));
    }

    /** 表情选择器插入的代码与网页版一致，且正文显示时能还原为同一张本地图片。 */
    @Test
    public void pickerCodesRoundTripToBundledImages() {
        int[] expectedCounts = {45, 46, 15, 65, 33};
        for (int c = 0; c < ForumEmoticonDecoder.CATEGORIES.length; c++) {
            String category = ForumEmoticonDecoder.CATEGORIES[c];
            List<String[]> emoticons = ForumEmoticonDecoder.emoticons(category);
            assertEquals(category, expectedCounts[c], emoticons.size());
            Set<String> names = new HashSet<>();
            for (String[] emoticon : emoticons) {
                String name = emoticon[0];
                String image = emoticon[1];
                assertTrue("重复的表情名 " + category + ":" + name, names.add(name));
                String code = ForumEmoticonDecoder.code(category, name);
                assertEquals("[s:" + category + ":" + name + "]", code);
                assertTrue(code, decoder.decode(code).contains("file:///android_asset/" + category + "/" + image + "'"));
                // 单元测试的工作目录为应用模块，表情图片位于 lib_core 的 assets
                assertTrue("缺少图片 " + category + "/" + image,
                        new File("../lib_core/src/main/assets/" + category + "/" + image).isFile());
            }
        }
        assertTrue(ForumEmoticonDecoder.emoticons("xiongmao").isEmpty());
    }

    /** 未知分类或代码沿用旧行为：图片名为 null，不中断其余内容。 */
    @Test
    public void unknownEmoticonsKeepNullImage() {
        assertEquals("<img src='file:///android_asset/zz/null'>", decoder.decode("[s:zz:x]"));
        assertEquals(
                "<img class='emoticon invertFilter' src='file:///android_asset/ac/null'>",
                decoder.decode("[s:ac:不存在]"));
    }
}
