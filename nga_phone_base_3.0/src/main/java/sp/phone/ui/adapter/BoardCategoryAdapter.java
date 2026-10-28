package sp.phone.ui.adapter;

import gov.anzong.androidnga.coroutines.UiEventBus;
import gov.anzong.androidnga.util.ThrottledClickListener;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.util.GlideApp;

import sp.phone.common.ApiConstants;
import sp.phone.mvp.model.entity.Board;
import sp.phone.mvp.model.entity.BoardCategory;
import gov.anzong.androidnga.coroutines.UiEvent;

import java.util.ArrayList;
import java.util.List;

/** 版块Grid Adapter */
public class BoardCategoryAdapter
        extends RecyclerView.Adapter<BoardCategoryAdapter.BoardViewHolder> {

    public static final int BOARD_ITEM = 1;

    public static final int TITLE_ITEM = 0;

    private BoardCategory mCategory;

    private Activity mActivity;

    private List<Integer> mTitlePositions;

    private int mTotalCount;

    class BoardViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView name;

        BoardViewHolder(View itemView) {
            super(itemView);
            bindViews(itemView);
        }

        protected void bindViews(android.view.View root) {
            icon = root.findViewById(R.id.icon_board_img);
            name = root.findViewById(R.id.text_board_name);
        }
    }

    public BoardCategoryAdapter(Activity activity, BoardCategory category) {
        mActivity = activity;
        mCategory = category;

        if (mCategory.getSubCategoryList() != null) {
            mTitlePositions = new ArrayList<>();
            for (BoardCategory subCategory : mCategory.getSubCategoryList()) {
                mTitlePositions.add(mTotalCount);
                mTotalCount += subCategory.getBoardList().size();
                mTotalCount++;
            }
        } else {
            mTotalCount = mCategory.getBoardList().size();
        }
    }

    @Override
    public BoardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId =
                viewType == BOARD_ITEM
                        ? R.layout.list_board_item
                        : R.layout.list_board_category_item;
        View view = getLayoutInflater().inflate(layoutId, parent, false);
        return new BoardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final BoardViewHolder holder, int position) {
        if (getItemViewType(position) == BOARD_ITEM) {
            Board board;
            if (mTitlePositions != null) {
                int subCategoryIndex;
                int realPosition = 0;
                for (subCategoryIndex = 0;
                        subCategoryIndex < mTitlePositions.size();
                        subCategoryIndex++) {
                    if (mTitlePositions.get(subCategoryIndex) > position) {
                        break;
                    }
                }
                subCategoryIndex--;
                realPosition = position - mTitlePositions.get(subCategoryIndex) - 1;
                BoardCategory subCategory = mCategory.getSubCategory(subCategoryIndex);
                board = subCategory.getBoard(realPosition);
            } else {
                board = mCategory.getBoard(position);
            }

            Drawable draw = getDrawable(board);
            if (draw == null) {
                String url;
                if (board.getStid() != 0) {
                    url = String.format(ApiConstants.URL_BOARD_ICON_STID, board.getStid());
                } else {
                    url = String.format(ApiConstants.URL_BOARD_ICON, board.getFid());
                }
                GlideApp.with(mActivity)
                        .load(url)
                        .placeholder(R.drawable.default_board_icon)
                        .dontAnimate()
                        .into(holder.icon);
            } else {
                holder.icon.setImageDrawable(draw);
            }
            holder.itemView.setTag(board);
            holder.name.setText(board.getName());
            ThrottledClickListener.clicks(
                    holder.itemView,
                    v ->
                            UiEventBus.post(new UiEvent(UiEvent.EVENT_SHOW_TOPIC_LIST, board)));
        } else {
            int subCategoryIndex = mTitlePositions.indexOf(position);
            BoardCategory subCategory = mCategory.getSubCategory(subCategoryIndex);
            holder.name.setText(subCategory.getName());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mTitlePositions == null) {
            return BOARD_ITEM;
        } else {
            return mTitlePositions.contains(position) ? TITLE_ITEM : BOARD_ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return mTotalCount;
    }

    private int getResId(Board board) {
        if (board.getStid() != 0) {
            return 0;
        }
        int fid = board.getFid();
        String resName = fid > 0 ? "p" + fid : "p_" + Math.abs(fid);
        return mActivity
                .getResources()
                .getIdentifier(resName, "drawable", mActivity.getPackageName());
    }

    private Drawable getDrawable(Board board) {
        Drawable drawable = null;
        int resId = getResId(board);
        if (resId != 0) {
            drawable = ContextCompat.getDrawable(mActivity, resId);
        }

        return drawable;
    }

    public LayoutInflater getLayoutInflater() {
        return mActivity.getLayoutInflater();
    }
}
