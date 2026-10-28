package sp.phone.ui.fragment;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.activity.ArticleCacheActivity;
import gov.anzong.androidnga.activity.BaseActivity;
import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.util.ToastUtils;
import gov.anzong.androidnga.base.widget.DividerItemDecorationEx;
import gov.anzong.androidnga.mvvm.viewmodel.TopicCacheViewModel;
import sp.phone.common.PhoneConfiguration;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.param.ArticleListParam;
import sp.phone.param.ParamKey;
import sp.phone.ui.adapter.TopicListAdapter;
import sp.phone.util.ActivityUtils;
import sp.phone.util.StringUtils;
import sp.phone.view.RecyclerViewEx;

/** 离线缓存的主题列表：点击阅读缓存，长按删除，菜单中导入或导出缓存。 */
public class TopicCacheFragment extends BaseFragment implements View.OnClickListener, View.OnLongClickListener {

    private TopicCacheViewModel mViewModel;

    private TopicListAdapter mAdapter;

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private View mLoadingView;

    private final ActivityResultLauncher<String[]> mImportLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    mViewModel.importFile(uri);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(TopicCacheViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_topic_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((BaseActivity) requireActivity()).setupToolbar();
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        mLoadingView = view.findViewById(R.id.loading_view);
        RecyclerViewEx listView = view.findViewById(R.id.list);

        mAdapter = new TopicListAdapter(getContext());
        mAdapter.setOnClickListener(this);
        mAdapter.setOnLongClickListener(this);
        mAdapter.setNextPageEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.setEmptyView(view.findViewById(R.id.empty_view));
        listView.setAdapter(mAdapter);
        if (PhoneConfiguration.getInstance().useSolidColorBackground()) {
            int padding = ContextUtils.getDimension(R.dimen.topic_list_item_padding);
            listView.addItemDecoration(new DividerItemDecorationEx(view.getContext(), padding, DividerItemDecoration.VERTICAL));
        }

        // 缓存列表不支持下拉刷新
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setVisibility(View.GONE);
        ((TextView) mLoadingView.findViewById(R.id.saying)).setText(ActivityUtils.getSaying());

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.menu_cache_list, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_cache_export) {
                    mViewModel.exportFile();
                } else if (item.getItemId() == R.id.menu_cache_import) {
                    chooseImportFile();
                } else {
                    return false;
                }
                return true;
            }
        }, getViewLifecycleOwner());

        mViewModel.getTopics().observe(getViewLifecycleOwner(), info -> {
            hideLoadingView();
            mAdapter.setData(info.getThreadPageList());
            mAdapter.setNextPageEnabled(false);
        });
        mViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (!loading) {
                hideLoadingView();
            }
        });
        mViewModel.getRemovedTopic().observe(getViewLifecycleOwner(), mAdapter::removeItem);
        mViewModel.getNotice().observe(getViewLifecycleOwner(), notice -> {
            if (notice == null) {
                return;
            }
            if (notice.success) {
                ToastUtils.success(notice.text);
            } else {
                ToastUtils.error(notice.text);
            }
            mViewModel.noticeShown();
        });

        if (savedInstanceState == null) {
            ToastUtils.success("长按可删除缓存的帖子");
        }
        mViewModel.start();
    }

    private void hideLoadingView() {
        mLoadingView.setVisibility(View.GONE);
        mSwipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void chooseImportFile() {
        try {
            mImportLauncher.launch(new String[]{"application/zip"});
        } catch (ActivityNotFoundException error) {
            ToastUtils.warn("系统不支持导入");
        }
    }

    @Override
    public boolean onLongClick(final View view) {
        new AlertDialog.Builder(getContext())
                .setMessage(getString(R.string.delete_favo_confirm_text))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> mViewModel.delete((ThreadPageInfo) view.getTag()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        return true;
    }

    @Override
    public void onClick(View view) {
        ThreadPageInfo info = (ThreadPageInfo) view.getTag();
        ArticleListParam param = new ArticleListParam();
        param.tid = info.getTid();
        param.loadCache = true;
        param.title = StringUtils.unEscapeHtml(info.getSubject());
        startActivity(new Intent(getContext(), ArticleCacheActivity.class).putExtra(ParamKey.KEY_PARAM, param));
    }
}
