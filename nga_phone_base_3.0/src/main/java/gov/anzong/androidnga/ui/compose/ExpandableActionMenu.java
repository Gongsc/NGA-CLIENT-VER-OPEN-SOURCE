package gov.anzong.androidnga.ui.compose;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/** Small Material bridge for the WebView reader's existing reply and refresh actions. */
public final class ExpandableActionMenu extends LinearLayout {
    public static final int EXPAND_UP = 0;
    public static final int LABELS_ON_RIGHT_SIDE = 1;
    private FloatingActionButton toggle;
    private boolean expanded;

    public ExpandableActionMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.END);
        int padding = Math.round(8 * getResources().getDisplayMetrics().density);
        setPadding(padding, padding, padding, padding);
    }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        int gap = Math.round(8 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < getChildCount(); i++) {
            LayoutParams params = (LayoutParams) getChildAt(i).getLayoutParams();
            params.bottomMargin = gap;
        }
        toggle = new FloatingActionButton(getContext());
        toggle.setImageResource(android.R.drawable.ic_input_add);
        toggle.setContentDescription("展开操作");
        toggle.setOnClickListener(view -> { if (expanded) collapse(); else expand(); });
        addView(toggle, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        collapse();
    }

    private void expand() {
        expanded = true;
        for (int i = 0; i < getChildCount(); i++) getChildAt(i).setVisibility(VISIBLE);
        toggle.setContentDescription("收起操作");
    }

    public void collapse() {
        expanded = false;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setVisibility(child == toggle ? VISIBLE : GONE);
        }
        if (toggle != null) toggle.setContentDescription("展开操作");
    }
    public FloatingActionButton getAddFloatingActionButton() { return toggle; }
    public int getAddButtonMarginBottom() { return getPaddingBottom(); }
    public void setExpandDirection(int direction, int labels) { setGravity(Gravity.START); }
}
