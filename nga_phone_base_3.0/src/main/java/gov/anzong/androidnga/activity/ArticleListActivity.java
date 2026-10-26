package gov.anzong.androidnga.activity;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import sp.phone.param.ArticleListParam;
import sp.phone.param.ParamKey;
import sp.phone.ui.fragment.ArticleSearchFragment;
import sp.phone.ui.fragment.ArticleTabFragment;
import android.net.Uri;

/** Compose 帖子详情入口，保留深链接与既有请求参数。 */
public class ArticleListActivity extends BaseActivity {

    private ArticleListParam mRequestParam;

    private void setupFragment() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(android.R.id.content);

        if (fragment == null) {
            if (mRequestParam.searchPost == 0) {
                fragment = new ArticleTabFragment();
            } else {
                fragment = new ArticleSearchFragment();
            }

            Bundle bundle = new Bundle();
            bundle.putParcelable(ParamKey.KEY_PARAM, mRequestParam);
            fragment.setArguments(bundle);
            fm.beginTransaction().replace(android.R.id.content, fragment).commit();
        }
    }

    private ArticleListParam getArticleListParam() {

        Bundle bundle = getIntent().getExtras();
        String url = getIntent().getDataString();
        ArticleListParam param = null;
        if (url != null) {
            param = new ArticleListParam();
            param.tid = queryInt(Uri.parse(url), "tid");
            param.pid = queryInt(Uri.parse(url), "pid");
            param.authorId = queryInt(Uri.parse(url), "authorid");
            param.page = queryInt(Uri.parse(url), "page");
            param.searchPost = queryInt(Uri.parse(url), ParamKey.KEY_SEARCH_POST);
        } else if (bundle != null) {
            param = bundle.getParcelable(ParamKey.KEY_PARAM);
            if (param == null) {
                param = new ArticleListParam();
                param.tid = bundle.getInt(ParamKey.KEY_TID, 0);
                param.pid = bundle.getInt(ParamKey.KEY_PID, 0);
                param.authorId = bundle.getInt(ParamKey.KEY_AUTHOR_ID, 0);
                param.searchPost = bundle.getInt(ParamKey.KEY_SEARCH_POST, 0);
                param.title = bundle.getString(ParamKey.KEY_TITLE);
            }
        }

        return param;
    }

    private static int queryInt(Uri uri, String name) {
        try {
            return Integer.parseInt(uri.getQueryParameter(name));
        } catch (IllegalArgumentException error) {
            return 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setToolbarEnabled(true);
        mRequestParam = getArticleListParam();
        super.onCreate(savedInstanceState);
        if (mRequestParam == null) {
            finish();
            return;
        }
        setupFragment();
        if (mRequestParam.title != null) {
            setTitle(mRequestParam.title);
        }
    }

    @Override
    protected boolean usesComposeContent() {
        return true;
    }
}
