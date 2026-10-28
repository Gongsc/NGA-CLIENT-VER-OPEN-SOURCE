package sp.phone.ui.adapter;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

import sp.phone.param.ArticleListParam;
import sp.phone.param.ParamKey;
import sp.phone.ui.fragment.ArticleListFragment;

/**
 * 帖子详情分页Adapter
 * Created by Justwen on 2017/7/9.
 */

public class ArticlePagerAdapter extends FragmentStateAdapter {

    private int mCount = 1;

    private ArticleListParam mRequestParam;

    private List<String> mPageIndexList;

    public ArticlePagerAdapter(FragmentActivity activity, ArticleListParam param) {
        super(activity);
        mRequestParam = param;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = new ArticleListFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ParamKey.KEY_PARAM, getRequestParam(position));
        fragment.setArguments(bundle);
        return fragment;
    }

    private ArticleListParam getRequestParam(int position) {
        ArticleListParam param = (ArticleListParam) mRequestParam.clone();
        if (mPageIndexList != null) {
            param.page = Integer.parseInt(mPageIndexList.get(position));
        } else {
            param.page = position + 1;
        }
        return param;
    }

    @Override
    public int getItemCount() {
        return mCount;
    }

    public void setCount(int count) {
        if (mCount != count) {
            mCount = count;
            notifyDataSetChanged();
        }
    }

    public void setPageIndexList(List<String> pageIndexList) {
        mPageIndexList = pageIndexList;
        setCount(pageIndexList.size());
    }

    /** 分页标签文字，由 TabLayoutMediator 使用。 */
    public CharSequence getPageTitle(int position) {
        return mPageIndexList == null ? String.valueOf(position + 1) : mPageIndexList.get(position);
    }
}
