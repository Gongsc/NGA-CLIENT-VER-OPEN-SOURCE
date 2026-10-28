package gov.anzong.androidnga.core.decode;

import gov.anzong.androidnga.common.util.EmoticonUtils;
import gov.anzong.androidnga.core.data.HtmlData;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 图片 BBCode 和网页图片标签使用一致的地址、缩略图及图库规则。 */
public class ForumImageDecoder implements IForumDecoder {
    private final List<String> mImageUrls = new ArrayList<>();
    private static final Pattern BBCODE =
            Pattern.compile("(?i)\\[img]\\s*([^\\[\\]]+?)\\s*\\[/img]");
    private static final Pattern IMAGE = Pattern.compile("(?i)<img\\b[^>]*>");
    private static final Pattern SOURCE = Pattern.compile("(?i)\\bsrc\\s*=\\s*(['\"])(.*?)\\1");
    private static final Pattern LINK =
            Pattern.compile("(?i)(<a\\b[^>]*\\bhref\\s*=\\s*)(['\"])(.*?)\\2");
    private static final String OFFLINE = "<img src='file:///android_asset/ic_offline_image.png'>";

    @Override
    public String decode(String content) {
        return decode(content, null);
    }

    @Override
    public String decode(String content, HtmlData htmlData) {
        Matcher bbcode = BBCODE.matcher(content);
        StringBuffer converted = new StringBuffer();
        while (bbcode.find()) {
            String url = ForumImageUrl.normalize(bbcode.group(1));
            String image =
                    url == null
                            ? OFFLINE
                            : "<a href='"
                                    + ForumImageUrl.attribute(ForumImageUrl.original(url))
                                    + "'><img src='"
                                    + ForumImageUrl.attribute(url)
                                    + "'></a>";
            bbcode.appendReplacement(converted, Matcher.quoteReplacement(image));
        }
        bbcode.appendTail(converted);
        mImageUrls.clear();
        Matcher images = IMAGE.matcher(converted.toString());
        StringBuffer result = new StringBuffer();
        while (images.find()) {
            String tag = images.group();
            Matcher source = SOURCE.matcher(tag);
            if (!source.find()) continue;
            String url = ForumImageUrl.normalize(source.group(2));
            String replacement = tag;
            if (url != null && !url.startsWith("file:")) {
                String path = emoticonPath(url);
                if (path != null)
                    replacement = "<img class='emoticon' src='file:///android_asset/" + path + "'>";
                else {
                    String original = ForumImageUrl.original(url);
                    if (!mImageUrls.contains(original)) mImageUrls.add(original);
                    replacement =
                            htmlData != null && !htmlData.isShowImage()
                                    ? OFFLINE
                                    : tag.substring(0, source.start(2))
                                            + ForumImageUrl.attribute(url)
                                            + tag.substring(source.end(2));
                }
            }
            images.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        images.appendTail(result);
        Matcher links = LINK.matcher(result.toString());
        StringBuffer normalized = new StringBuffer();
        while (links.find()) {
            String url = ForumImageUrl.normalize(links.group(3));
            String replacement =
                    url == null
                            ? links.group()
                            : links.group(1)
                                    + links.group(2)
                                    + ForumImageUrl.attribute(ForumImageUrl.original(url))
                                    + links.group(2);
            links.appendReplacement(normalized, Matcher.quoteReplacement(replacement));
        }
        links.appendTail(normalized);
        return normalized.toString();
    }

    /** 旧帖以图片形式插入的表情使用内置图片；按路径匹配，与图片域名无关。 */
    private static String emoticonPath(String url) {
        String path = EmoticonUtils.getPathByURI(url);
        return path != null ? path : EmoticonUtils.getPathByForumPath(ForumImageUrl.forumPath(url));
    }

    @Override
    public List<String> getImageUrls() {
        return mImageUrls;
    }
}
