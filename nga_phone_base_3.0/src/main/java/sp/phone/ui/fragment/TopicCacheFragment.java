package sp.phone.ui.fragment;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.activity.ArticleCacheActivity;
import gov.anzong.androidnga.base.util.ToastUtils;

import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.mvp.model.entity.TopicListInfo;
import sp.phone.param.ArticleListParam;
import sp.phone.param.ParamKey;
import sp.phone.util.StringUtils;

/**
 * @author Justwen
 */
public class TopicCacheFragment extends TopicSearchFragment implements View.OnLongClickListener {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ToastUtils.success("长按可删除缓存的帖子");
        mAdapter.setOnLongClickListener(this);
        mPresenter.getRemovedTopic().observe(getViewLifecycleOwner(), this::removeTopic);
    }

    @Override
    public void setData(TopicListInfo result) {
        super.setData(result);
        mAdapter.setNextPageEnabled(false);
        mSwipeRefreshLayout.setEnabled(false);
    }

    @Override
    public void removeTopic(int position) {
        mAdapter.removeItem(position);
    }

    @Override
    public void removeTopic(ThreadPageInfo pageInfo) {
        mAdapter.removeItem(pageInfo);
    }

    @Override
    public boolean onLongClick(final View view) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(this.getString(R.string.delete_favo_confirm_text))
                .setPositiveButton(
                        android.R.string.ok,
                        (dialog, which) -> {
                    ThreadPageInfo info = (ThreadPageInfo) view.getTag();
                    mPresenter.removeCacheTopic(info);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create()
                .show();
        return true;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_cache_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int selectedResourceId = item.getItemId();
        if (selectedResourceId == R.id.menu_cache_export) {
                mPresenter.exportCacheTopic(this);
        } else if (selectedResourceId == R.id.menu_cache_import) {
                mPresenter.showFileChooser(mImportLauncher);
        } else {
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        ThreadPageInfo info = (ThreadPageInfo) view.getTag();
        ArticleListParam param = new ArticleListParam();
        param.tid = info.getTid();
        param.loadCache = true;
        param.title = StringUtils.unEscapeHtml(info.getSubject());
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ParamKey.KEY_PARAM, param);
        intent.putExtras(bundle);
        intent.setClass(getContext(), ArticleCacheActivity.class);
        startActivity(intent);
    }

    private final ActivityResultLauncher<String[]> mImportLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    mPresenter.importCacheTopic(uri);
                }
            });
}
