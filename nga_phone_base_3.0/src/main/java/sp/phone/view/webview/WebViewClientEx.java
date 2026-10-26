package sp.phone.view.webview;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import gov.anzong.androidnga.activity.ArticleListActivity;
import gov.anzong.androidnga.activity.TopicListActivity;
import gov.anzong.androidnga.arouter.ARouterConstants;
import gov.anzong.androidnga.arouter.AppNavigator;
import gov.anzong.androidnga.gallery.ImageZoomActivity;

import sp.phone.util.ForumUtils;

import java.util.ArrayList;
import java.util.List;

public class WebViewClientEx extends WebViewClient {
    private List<String> mImgUrlList;

    public WebViewClientEx(Context context) {
        super();
    }

    public WebViewClientEx() {
        super();
    }

    public void setImgUrls(List<String> list) {
        mImgUrlList = list;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return request.isForMainFrame()
                && shouldOverrideUrlLoading(view, request.getUrl().toString());
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url == null || url.trim().startsWith("#")) return false;
        ForumLink link = ForumLink.resolve(url, view.getUrl(), ForumUtils.getAvailableDomain());
        // 阻止脚本、自定义协议等地址通过论坛正文启动其他应用。
        if (link == null) return true;
        Context context = view.getContext();
        String resolved = link.url.toString();
        Intent intent;
        switch (link.target) {
            case ARTICLE:
                intent = new Intent(context, ArticleListActivity.class);
                intent.setData(Uri.parse(resolved));
                intent.putExtra("fromreplyactivity", 1);
                break;
            case FORUM:
                intent = new Intent(context, TopicListActivity.class);
                intent.setData(Uri.parse(resolved));
                break;
            case PROFILE:
                AppNavigator.getInstance()
                        .build(ARouterConstants.ACTIVITY_PROFILE)
                        .withString("mode", "username")
                        .withString("username", link.url.queryParameter("username"))
                        .navigation(context);
                return true;
            case IMAGE:
                intent = new Intent(context, ImageZoomActivity.class);
                List<String> images =
                        mImgUrlList == null ? new ArrayList<>() : new ArrayList<>(mImgUrlList);
                if (!images.contains(resolved)) images.add(resolved);
                intent.putExtra(ImageZoomActivity.KEY_GALLERY_URLS, images.toArray(new String[0]));
                intent.putExtra(ImageZoomActivity.KEY_GALLERY_CUR_URL, resolved);
                break;
            default:
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(resolved));
                break;
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            // 用户设备可能没有可处理外部链接的应用。
        }
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        view.getSettings().setBlockNetworkImage(false);
        super.onPageFinished(view, url);
    }
}
