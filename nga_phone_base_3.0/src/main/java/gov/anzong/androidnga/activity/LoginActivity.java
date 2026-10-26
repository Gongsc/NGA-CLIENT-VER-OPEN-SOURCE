package gov.anzong.androidnga.activity;

import android.app.Activity;
import android.os.Bundle;

import sp.phone.ui.fragment.BaseFragment;
import sp.phone.ui.fragment.LoginWebFragment;

public class LoginActivity extends BaseActivity {

    private BaseFragment mLoginFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setToolbarEnabled(true);
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_CANCELED);
        mLoginFragment =
                (BaseFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (mLoginFragment == null) {
            mLoginFragment = new LoginWebFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, mLoginFragment)
                    .commit();
        }
    }

    @Override
    protected boolean handleBackNavigation() {
        return mLoginFragment != null && mLoginFragment.onBackPressed();
    }

    @Override
    protected boolean usesComposeContent() {
        return true;
    }
}
