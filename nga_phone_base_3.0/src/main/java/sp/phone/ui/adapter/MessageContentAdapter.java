package sp.phone.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.base.util.ToastUtils;

import sp.phone.http.bean.MessageArticlePageInfo;
import sp.phone.http.bean.MessageDetailInfo;
import sp.phone.theme.ThemeManager;
import sp.phone.ui.adapter.beta.BaseAppendableAdapterEx;
import sp.phone.util.FunctionUtils;
import sp.phone.view.RecyclerViewEx;

import java.util.ArrayList;
import java.util.List;

/** Created by Justwen on 2017/10/15. */
public class MessageContentAdapter
        extends BaseAppendableAdapterEx<MessageDetailInfo, MessageContentAdapter.MessageViewHolder>
        implements RecyclerViewEx.IAppendableAdapter {

    private List<MessageDetailInfo> mInfoList = new ArrayList<>();

    private boolean mPrompted;

    private int mTotalCount;

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView nickName;
        public TextView floor;
        public TextView postTime;
        public WebView content;

        public MessageViewHolder(View itemView) {
            super(itemView);
            bindViews(itemView);
        }

        protected void bindViews(android.view.View root) {
            nickName = root.findViewById(R.id.nickName);
            floor = root.findViewById(R.id.floor);
            postTime = root.findViewById(R.id.postTime);
            content = root.findViewById(R.id.content);
        }
    }

    public MessageContentAdapter(Context context) {
        super(context);
    }

    @Override
    protected void onBindItemViewHolder(MessageViewHolder holder, int position) {
        handleJsonList(holder, position);
        if (position + 1 == getItemCount() && !hasNextPage() && !mPrompted) {
            ToastUtils.showToast(R.string.last_page_prompt_message_detail);
            mPrompted = true;
        }
    }

    @Override
    protected MessageViewHolder onCreateItemViewHolder(ViewGroup parent, LayoutInflater inflater) {
        return new MessageViewHolder(
                inflater.inflate(R.layout.list_message_content, parent, false));
    }

    private MessageArticlePageInfo getEntry(int position) {
        return mInfoList.get(position / 20).getMessageEntryList().get(position % 20);
    }

    /** 显示已加载的全部页；换成新的第一页（刷新）时重新提示最后一页。 */
    public void setPages(List<MessageDetailInfo> pages) {
        if (pages.isEmpty() || mInfoList.isEmpty() || pages.get(0) != mInfoList.get(0)) {
            mPrompted = false;
        }
        mInfoList.clear();
        mInfoList.addAll(pages);
        mTotalCount = 0;
        for (MessageDetailInfo page : pages) {
            mTotalCount += page.getMessageEntryList().size();
        }
        setNextPageEnabled(pages.isEmpty() || pages.get(pages.size() - 1).get__nextPage() > 0);
        notifyDataSetChanged();
    }

    @Override
    public int getNextPage() {
        return mInfoList.size() + 1;
    }

    private void handleJsonList(MessageViewHolder holder, int position) {
        final MessageArticlePageInfo entry = getEntry(position);
        if (entry == null) {
            return;
        }
        Resources res = mContext.getResources();
        ThemeManager theme = ThemeManager.getInstance();
        holder.postTime.setText(entry.getTime());
        String floor = String.valueOf(entry.getLou());
        holder.floor.setText("[" + floor + " ¥]");
        holder.nickName.setTextColor(res.getColor(theme.getForegroundColor()));
        holder.postTime.setTextColor(res.getColor(theme.getForegroundColor()));
        holder.floor.setTextColor(res.getColor(theme.getForegroundColor()));

        FunctionUtils.handleNickName(
                entry, res.getColor(theme.getForegroundColor()), holder.nickName, mContext);

        int colorId = theme.getBackgroundColor(position + 1);
        final int bgColor = res.getColor(colorId);
        int fgColorId = theme.getForegroundColor();
        final int fgColor = res.getColor(fgColorId);
        holder.itemView.setBackgroundResource(colorId);
        FunctionUtils.handleContentTV(holder.content, entry, bgColor, fgColor, mContext);
    }

    @Override
    protected int getItemViewCount() {
        return mTotalCount;
    }
}
