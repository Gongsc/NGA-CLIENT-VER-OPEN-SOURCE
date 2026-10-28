package sp.phone.ui.fragment;

import androidx.core.os.BundleCompat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import gov.anzong.androidnga.R;

import sp.phone.mvp.model.entity.BoardCategory;
import sp.phone.ui.adapter.BoardCategoryAdapter;

/** 版块分页 */
public class BoardCategoryFragment extends Fragment {

    private static final String TAG = BoardCategoryFragment.class.getSimpleName();

    private RecyclerView mListView;

    private BoardCategoryAdapter mAdapter;

    private BoardCategory mBoardCategory;

    public static final int COLUMN_NUMBER = 3;

    private static final int COLUMN_NUMBER_LAND = 5;

    public static Fragment newInstance(BoardCategory category) {
        Fragment f = new BoardCategoryFragment();
        Bundle args = new Bundle();
        args.putParcelable("category", category);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBoardCategory = BundleCompat.getParcelable(getArguments(), "category", BoardCategory.class);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_board_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mListView = view.findViewById(R.id.list);

        mAdapter = new BoardCategoryAdapter(getActivity(), mBoardCategory);
        mListView.setAdapter(mAdapter);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), COLUMN_NUMBER);
        layoutManager.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return mAdapter.getItemViewType(position) == BoardCategoryAdapter.TITLE_ITEM
                                ? COLUMN_NUMBER
                                : 1;
                    }
                });
        mListView.setLayoutManager(layoutManager);

        mListView.setAdapter(mAdapter);
    }
}
