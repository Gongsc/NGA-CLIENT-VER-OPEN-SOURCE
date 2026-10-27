package sp.phone.ui.fragment.dialog;

import gov.anzong.androidnga.coroutines.UiEventBus;
import gov.anzong.androidnga.util.ThrottledClickListener;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import gov.anzong.androidnga.R;

import sp.phone.rxjava.RxEvent;

public class LoginDialogFragment extends BaseDialogFragment {
    public WebView mAuthCodeWebView;
    public TextInputLayout mAuthCodeEditView;

    private String mDataUrl;

    private OnAuthCodeLoadCallback mAuthCodeLoadCallback;
    public TextView mUpdateAuthCodeBtn;

    public interface OnAuthCodeLoadCallback {

        void loadAuthCodeImage(WebView webView);

        void login(String authCode);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mDataUrl = bundle.getString("data_url");

        // 随对话框销毁结束订阅，原先的订阅从未释放。
        UiEventBus.observe(
                this,
                rxEvent -> {
                    if (rxEvent.what == RxEvent.EVENT_LOGIN_AUTH_CODE_UPDATE
                            && mAuthCodeWebView != null) {
                        mAuthCodeWebView.loadUrl(String.valueOf(rxEvent.obj));
                    }
                });
    }

    public void setAuthCodeLoadCallback(OnAuthCodeLoadCallback authCodeLoadCallback) {
        mAuthCodeLoadCallback = authCodeLoadCallback;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_login, null, false);

        bindViews(view);
        mAuthCodeWebView.setBackgroundColor(Color.TRANSPARENT);
        mAuthCodeWebView.loadUrl(mDataUrl);

        ThrottledClickListener.clicks(
                mUpdateAuthCodeBtn,
                new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuthCodeLoadCallback.loadAuthCodeImage(mAuthCodeWebView);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("验证码")
                .setPositiveButton(
                        "登录",
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                                mAuthCodeLoadCallback.login(
                                        mAuthCodeEditView.getEditText().getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setView(view);

        return builder.create();
    }

    protected void bindViews(android.view.View root) {
        mAuthCodeWebView = root.findViewById(R.id.wv_auth_code);
        mAuthCodeEditView = root.findViewById(R.id.et_auth_code);
        mUpdateAuthCodeBtn = root.findViewById(R.id.btn_update);
}
}
