package gov.anzong.androidnga.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import gov.anzong.androidnga.arouter.NavigationRequest;
import gov.anzong.androidnga.arouter.AppNavigator;

import gov.anzong.androidnga.NgaClientApp;
import gov.anzong.androidnga.R;
import gov.anzong.androidnga.arouter.ARouterConstants;
import gov.anzong.androidnga.base.util.ThemeUtils;

import sp.phone.common.User;
import sp.phone.common.UserManagerImpl;
import sp.phone.param.ParamKey;
import sp.phone.theme.ThemeManager;
import sp.phone.ui.fragment.BaseFragment;
import sp.phone.ui.fragment.NavigationDrawerFragment;
import sp.phone.ui.fragment.dialog.UrlInputDialogFragment;
import sp.phone.ui.fragment.dialog.VersionUpgradeDialogFragment;
import sp.phone.util.ARouterUtils;
import sp.phone.util.ActivityUtils;

public class MainActivity extends BaseActivity {

    private boolean mIsNightMode;

    private BaseFragment mBoardFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setToolbarEnabled(true);
        super.onCreate(savedInstanceState);
        ThemeUtils.init(this);
        checkPermission();
        checkNewVersion();
        initView();
        mIsNightMode = ThemeManager.getInstance().isNightMode();
        setTitle(R.string.start_title);
        fixMultiMainActivityIssue();
    }

    private void fixMultiMainActivityIssue() {
        if (!isTaskRoot()) {
            finish();
        }
    }

    @Override
    protected void onCreateAfterSuper(@Nullable Bundle savedInstanceState) {}

    private void checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33
                && androidx.core.content.ContextCompat.checkSelfPermission(
                                this, Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(
                    this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void checkNewVersion() {
        Application app = getApplication();
        if (app instanceof NgaClientApp) {
            if (NgaClientApp.isNewVersion()) {
                new VersionUpgradeDialogFragment().show(getSupportFragmentManager(), null);
            }
        }
    }

    @Override
    protected void onResume() {
        if (mIsNightMode != ThemeManager.getInstance().isNightMode()) {
            finish();
            startActivity(getIntent());
        }
        super.onResume();
    }

    private void initView() {
        FragmentManager fm = getSupportFragmentManager();
        mBoardFragment =
                (BaseFragment) fm.findFragmentByTag(NavigationDrawerFragment.class.getSimpleName());
        if (mBoardFragment== null) {
            mBoardFragment = new NavigationDrawerFragment();
            fm.beginTransaction()
                    .replace(
                            android.R.id.content,
                            mBoardFragment,
                            NavigationDrawerFragment.class.getSimpleName())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return handleNavigation(item.getItemId()) || super.onOptionsItemSelected(item);
    }

    public boolean handleNavigation(int itemId) {
        int selectedResourceId = itemId;
        if (selectedResourceId == R.id.menu_appearance) {
                startActivity(new Intent(this, AppearanceActivity.class));
        } else if (selectedResourceId == R.id.menu_setting) {
                startSettingActivity();
        } else if (selectedResourceId == R.id.menu_bookmark) {
                startFavoriteTopicActivity();
        } else if (selectedResourceId == R.id.menu_msg) {
                startMessageActivity();
        } else if (selectedResourceId == R.id.menu_post) {
                startPostActivity(false);
        } else if (selectedResourceId == R.id.menu_reply) {
                startPostActivity(true);
        } else if (selectedResourceId == R.id.menu_about) {
                aboutNgaClient();
        } else if (selectedResourceId == R.id.menu_search) {
                startSearchActivity();
        } else if (selectedResourceId == R.id.menu_forward) {
                new UrlInputDialogFragment().show(getSupportFragmentManager());
        } else if (selectedResourceId == R.id.menu_gun) {
                startNotificationActivity();
        } else if (selectedResourceId == R.id.menu_download) {
                startActivity(new Intent(this,TopicCacheActivity.class));
        } else {
            return false;
        }
        return true;
    }

    private void startSearchActivity() {
        AppNavigator.getInstance().build(ARouterConstants.ACTIVITY_SEARCH).navigation(this);
    }

    private void startMessageActivity() {
        ARouterUtils.build(ARouterConstants.ACTIVITY_MESSAGE_LIST).navigation(this);
    }

    private void aboutNgaClient() {
       // new AboutClientDialogFragment().show(getSupportFragmentManager());
        startActivity(new Intent(this,AboutActivity.class));
    }

    private void startSettingActivity() {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, SettingsActivity.class);
        startActivityForResult(intent, ActivityUtils.REQUEST_CODE_SETTING);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ActivityUtils.REQUEST_CODE_SETTING && resultCode == Activity.RESULT_OK) {
            recreate();
        }
    }

    private void startNotificationActivity() {
        ARouterUtils.build(ARouterConstants.ACTIVITY_NOTIFICATION).navigation(this);
    }

    // NPE问题
    private void startPostActivity(boolean isReply) {
        User user = UserManagerImpl.getInstance().getActiveUser();
        String userName = user != null ? user.getNickName() : "";
        int uid = user != null ? Integer.parseInt(user.getUserId()) : 0;
        NavigationRequest postcard =
                ARouterUtils.build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                .withInt(ParamKey.KEY_AUTHOR_ID, uid)
                .withString(ParamKey.KEY_AUTHOR, userName);
        if (isReply) {
            postcard.withInt(ParamKey.KEY_SEARCH_POST, 1);
        }
        postcard.navigation(this);
    }

    private void startFavoriteTopicActivity() {
        ARouterUtils.build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                .withInt(ParamKey.KEY_FAVOR, 1)
                .navigation(this);
    }

    @Override
    protected boolean usesComposeContent() {
        return true;
}
}
