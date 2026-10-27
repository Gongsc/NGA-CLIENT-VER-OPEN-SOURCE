package gov.anzong.androidnga.core.decode;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ForumEmoticonDecoderTest {

    private final ForumEmoticonDecoder decoder = new ForumEmoticonDecoder();

    @Test
    public void knownEmoticonsMapToBundledImages() {
        assertEquals(
                "前<img class='emoticon invertFilter' src='file:///android_asset/ac/-47218_5052bca81a77f.png'>后",
                decoder.decode("前[s:ac:blink]后"));
        assertEquals("<img src='file:///android_asset/pg/pg01.png'>", decoder.decode("[s:pg:战斗力]"));
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
