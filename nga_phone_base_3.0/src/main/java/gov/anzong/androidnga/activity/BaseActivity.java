package gov.anzong.androidnga.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import gov.anzong.androidnga.R;
import gov.anzong.androidnga.base.util.PreferenceUtils;
import gov.anzong.androidnga.common.PreferenceKey;

import sp.phone.common.NotificationController;
import sp.phone.common.PhoneConfiguration;
import sp.phone.theme.ThemeManager;

/** Created by liuboyu on 16/6/28. */
public abstract class BaseActivity extends AppCompatActivity {

    protected PhoneConfiguration mConfig;

    private boolean mToolbarEnabled;

    private boolean mHardwareAcceleratedEnabled = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mConfig = PhoneConfiguration.getInstance();
        updateWindowFlag();
        updateThemeUi();
        onCreateBeforeSuper(savedInstanceState);
        super.onCreate(savedInstanceState);
        gov.anzong.androidnga.ui.compose.ModernWindowKt.configureModernWindow(this);
        onCreateAfterSuper(savedInstanceState);
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new androidx.activity.OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                if (!handleBackNavigation()) {
                                    setEnabled(false);
                                    getOnBackPressedDispatcher().onBackPressed();
                                    setEnabled(true);
                                }
                            }
                        });
        ThemeManager.getInstance().initializeWebTheme(this);
        }

    protected boolean handleBackNavigation() {
        return false;
    }

    protected boolean usesComposeContent() {
        return false;
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!usesComposeContent()) {
            gov.anzong.androidnga.ui.compose.ModernWindowKt.applyLegacyWindowInsets(this);
        }
    }

    protected void onCreateBeforeSuper(@Nullable Bundle savedInstanceState) {}

    protected void onCreateAfterSuper(@Nullable Bundle savedInstanceState) {}

    protected void setToolbarEnabled(boolean enabled) {
        mToolbarEnabled = enabled;
    }

    protected void setHardwareAcceleratedEnabled(boolean enabled) {
        mHardwareAcceleratedEnabled = enabled;
    }

    protected void updateThemeUi() {
        ThemeManager tm = ThemeManager.getInstance();
        setTheme(tm.getTheme(mToolbarEnabled));
    }

    protected void updateWindowFlag() {
        int flag = 0;

        if (mHardwareAcceleratedEnabled) {
            flag = flag | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }
        getWindow().addFlags(flag);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int selectedResourceId = item.getItemId();
        if (selectedResourceId == android.R.id.home) {
                finish();
        } else {
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        try {
            return super.dispatchTouchEvent(ev);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        try {
            return super.dispatchKeyEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onResume() {
        NotificationController.getInstance().checkNotificationDelay();
        super.onResume();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        try {
            super.startActivityForResult(intent, requestCode, options);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }
}
