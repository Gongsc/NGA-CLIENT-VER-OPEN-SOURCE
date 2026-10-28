package gov.anzong.androidnga.activity;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import sp.phone.ui.fragment.TopicCacheFragment;

/**
 * @author Justwen
 */
public class TopicCacheActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setToolbarEnabled(true);
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // 重建时沿用已恢复的 Fragment，保留已读取的缓存列表
            Fragment fragment = new TopicCacheFragment();
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
        }
    }
}
