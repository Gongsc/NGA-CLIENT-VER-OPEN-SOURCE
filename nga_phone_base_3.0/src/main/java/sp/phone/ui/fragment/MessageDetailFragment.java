package sp.phone.ui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.arouter.ARouterConstants;
import gov.anzong.androidnga.arouter.AppNavigator;
import gov.anzong.androidnga.mvvm.viewmodel.MessageDetailViewModel;
import sp.phone.ui.adapter.MessageContentAdapter;
import sp.phone.view.RecyclerViewEx;

public class MessageDetailFragment extends BaseFragment {

    private SwipeRefreshLayout mSwipeRefreshLayout;

    private MessageDetailViewModel mViewModel;

    private MessageContentAdapter mAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MessageDetailViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message_detail, container, false);
        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

        mAdapter = new MessageContentAdapter(getContext());

        RecyclerViewEx listView = view.findViewById(R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.setAdapter(mAdapter);
        listView.setItemViewCacheSize(20);
        listView.setOnNextPageLoadListener(mViewModel::loadNextPage);

        mSwipeRefreshLayout.setOnRefreshListener(mViewModel::refresh);

        FloatingActionButton fab = requireActivity().findViewById(R.id.fab);
        fab.setOnClickListener(v -> startMessagePost());

        // 首屏加载由列表底部的加载提示表示，加载出内容后才允许下拉刷新
        mSwipeRefreshLayout.setEnabled(false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel.getPages().observe(getViewLifecycleOwner(), pages -> {
            if (!pages.isEmpty()) {
                showContent();
            }
            mAdapter.setPages(pages);
        });
        mViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (mSwipeRefreshLayout.isEnabled()) {
                mSwipeRefreshLayout.setRefreshing(loading);
            }
        });
        mViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error == null) {
                return;
            }
            showContent();
            if (TextUtils.isEmpty(error)) {
                showToast(R.string.error_network);
            } else {
                showToast(error);
            }
            mViewModel.errorShown();
        });
        if (!mViewModel.isLoaded()) {
            mViewModel.refresh();
        }
    }

    private void showContent() {
        mAdapter.hideLoadingView();
        mSwipeRefreshLayout.setEnabled(true);
    }

    private void startMessagePost() {
        AppNavigator.getInstance()
                .build(ARouterConstants.ACTIVITY_MESSAGE_POST)
                .withInt("mid", mViewModel.getMid())
                .withString("action", "reply")
                .withString("to", mViewModel.getRecipient())
                .withString("title", mViewModel.getTitle())
                .navigation(getContext());
    }
}
