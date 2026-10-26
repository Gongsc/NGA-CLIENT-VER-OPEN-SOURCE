package sp.phone.ui.adapter;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import gov.anzong.androidnga.R;

import sp.phone.common.PhoneConfiguration;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.param.TopicTitleHelper;
import sp.phone.rxjava.RxUtils;
import sp.phone.theme.ThemeManager;

import java.util.List;

public class TopicListAdapter
        extends BaseAppendableAdapter<ThreadPageInfo, TopicListAdapter.TopicViewHolder> {

    public TopicListAdapter(Context context) {
        super(context);
    }

    @Override
    public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TopicViewHolder viewHolder =
                new TopicViewHolder(
                        LayoutInflater.from(mContext).inflate(R.layout.list_topic, parent, false));
        viewHolder.title.setTextSize(
                TypedValue.COMPLEX_UNIT_DIP, PhoneConfiguration.getInstance().getTopicTitleSize());
        RxUtils.clicks(viewHolder.itemView, mOnClickListener);
        viewHolder.itemView.setOnLongClickListener(mOnLongClickListener);
        return viewHolder;
    }

    @Override
    public void setData(List<ThreadPageInfo> dataList) {
        if (dataList == null) {
            super.setData(null);
        } else {
            super.appendData(dataList);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {

        ThreadPageInfo info = getItem(position);
        info.setPosition(position);
        holder.itemView.setTag(info);

        handleJsonList(holder, info);
        if (!PhoneConfiguration.getInstance().useSolidColorBackground()) {
            holder.itemView.setBackgroundResource(
                    ThemeManager.getInstance().getBackgroundColor(position));
        }
    }

    private void handleJsonList(TopicViewHolder holder, ThreadPageInfo entry) {

        if (entry == null) {
            return;
        }
        holder.author.setText(entry.getAuthor());
        holder.lastReply.setText(entry.getLastPoster());
        holder.num.setText(String.valueOf(entry.getReplies()));
        holder.title.setText(TopicTitleHelper.handleTitleFormat(entry));
    }

    public class TopicViewHolder extends RecyclerView.ViewHolder {
        public TextView num;
        public TextView title;
        public TextView author;
        public TextView lastReply;

        public TopicViewHolder(View itemView) {
            super(itemView);
            bindViews(itemView);
        }

        protected void bindViews(android.view.View root) {
            num = root.findViewById(R.id.num);
            title = root.findViewById(R.id.title);
            author = root.findViewById(R.id.author);
            lastReply = root.findViewById(R.id.last_reply);
        }
    }
}
