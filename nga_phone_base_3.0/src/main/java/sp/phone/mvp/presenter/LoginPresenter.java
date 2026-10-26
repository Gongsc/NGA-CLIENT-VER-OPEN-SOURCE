package sp.phone.mvp.presenter;

import android.text.TextUtils;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.http.OnHttpCallBack;

import sp.phone.common.UserManagerImpl;
import sp.phone.mvp.contract.LoginContract;
import sp.phone.mvp.model.LoginModel;
import sp.phone.param.LoginParam;
import sp.phone.ui.fragment.LoginFragment;
import sp.phone.util.ActivityUtils;

/** Created by Justwen on 2017/6/16. */
public class LoginPresenter extends BasePresenter<LoginFragment, LoginModel>
        implements LoginContract.Presenter {

    private LoginParam mLoginParam;

    private static final String TAG_UID = "ngaPassportUid";

    private static final String TAG_CID = "ngaPassportCid";

    private static final String TAG_USER_NAME = "ngaPassportUrlencodedUname";

    @Override
    protected LoginModel onCreateModel() {
        return new LoginModel();
    }

    @Override
    public void loadAuthCode() {
        mBaseModel.loadAuthCode(
                new OnHttpCallBack<LoginParam>() {
                    @Override
                    public void onError(String text) {
                        ActivityUtils.showToast("载入验证码失败");
                    }

                    @Override
                    public void onSuccess(LoginParam param) {
                        mLoginParam = param;
                        mBaseView.setAuthCodeImg(param.getDataUrl());
                    }
                });
    }

    @Override
    public void login(String userName, String password, String authCode) {
        if (TextUtils.isEmpty(userName)
                || TextUtils.isEmpty(password)
                || TextUtils.isEmpty(authCode)) {
            ActivityUtils.showToast("内容缺少，请检查后再试");
            return;
        }

        mLoginParam.setAuthCode(authCode);
        mLoginParam.setUserName(userName);
        mLoginParam.setPassword(password);

        mBaseModel.login(
                mLoginParam,
                new OnHttpCallBack<String>() {
                    @Override
                    public void onError(String text) {
                        ActivityUtils.showToast(text);
                        loadAuthCode();
                    }

                    @Override
                    public void onSuccess(String data) {}
                });
    }

    @Override
    public void parseCookie(String cookies) {
        gov.anzong.androidnga.ui.compose.LoginSession session =
                gov.anzong.androidnga.ui.compose.LoginSessionKt.parseLoginSession(cookies);
        if (session != null) saveCookie(session.getUid(), session.getCid(), session.getUserName());
    }

    private void saveCookie(String uid, String cid, String userName) {
        UserManagerImpl.getInstance().addUser(uid, cid, userName);
        if (mBaseView != null) {
            mBaseView.showToast(R.string.login_successfully);
            mBaseView.setResult(true);
        }
    }
}
