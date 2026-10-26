package gov.anzong.androidnga.base.widget;

import android.content.Context;
import android.util.AttributeSet;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

/** Material tabs for the remaining ViewPager-based article reader. */
public class TabLayoutEx extends TabLayout {
    private ViewPager pager;

    public TabLayoutEx(Context context) { super(context); }
    public TabLayoutEx(Context context, AttributeSet attrs) { super(context, attrs); }
    public TabLayoutEx(Context context, AttributeSet attrs, int style) { super(context, attrs, style); }

    public void setUpWithViewPager(ViewPager viewPager) {
        pager = viewPager;
        setupWithViewPager(viewPager);
    }

    public void notifyDataSetChanged() {
        if (pager != null) setupWithViewPager(pager);
    }

    public void setTabOnScreenLimit(int limit) {
        setTabMode(limit > 0 && limit <= 5 ? MODE_FIXED : MODE_SCROLLABLE);
    }
}
