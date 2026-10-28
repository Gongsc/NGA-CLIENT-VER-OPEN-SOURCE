package sp.phone.mvp.presenter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import gov.anzong.androidnga.activity.ArticleListActivity;
import gov.anzong.androidnga.activity.TopicListActivity;
import gov.anzong.androidnga.arouter.ARouterConstants;
import gov.anzong.androidnga.base.util.ToastUtils;

import sp.phone.common.PhoneConfiguration;
import sp.phone.common.User;
import sp.phone.common.UserManager;
import sp.phone.common.UserManagerImpl;
import sp.phone.mvp.contract.BoardContract;
import sp.phone.mvp.model.BoardModel;
import sp.phone.mvp.model.entity.Board;
import sp.phone.mvp.model.entity.BoardCategory;
import sp.phone.param.ParamKey;
import sp.phone.ui.fragment.NavigationDrawerFragment;
import sp.phone.util.ARouterUtils;
import sp.phone.util.ActivityUtils;
import sp.phone.util.HttpUtil;
import sp.phone.util.NLog;
import sp.phone.util.StringUtils;

import java.util.List;

/** 版块管理 Created by Justwen on 2017/6/29. */
public class BoardPresenter extends BasePresenter<NavigationDrawerFragment, BoardModel>
        implements BoardContract.Presenter {

    private UserManager mUserManager;

    public BoardPresenter() {
        super();
        mUserManager = UserManagerImpl.getInstance();
    }

    @Override
    public void loadBoardInfo() {}

    @Override
    public boolean addBoard(String fidStr, String stidStr) {
        try {
            int fid = TextUtils.isEmpty(fidStr.trim()) ? 0 : Integer.parseInt(fidStr.trim());
            int stid = TextUtils.isEmpty(stidStr.trim()) ? 0 : Integer.parseInt(stidStr.trim());
            if (fid == 0 && stid == 0) {
                ToastUtils.error("请输入版块编号或合集编号");
                return false;
            }
            String name = mBaseModel.getBoardName(fid, stid);
            if (TextUtils.isEmpty(name)) name = stid != 0 ? "合集 " + stid : "版块 " + fid;
            addBookmarkBoard(fid, stid, name);
            return true;
        } catch (NumberFormatException e) {
            ToastUtils.error("版块编号或合集编号格式不正确");
            return false;
        }
    }

    @Override
    public void toggleUser(List<User> userList) {
        if (userList != null && userList.size() > 1) {
            int index = mBaseView.switchToNextUser();
            mUserManager.setActiveUser(index);
            ToastUtils.showToast("切换账户成功,当前账户名:" + mUserManager.getActiveUser().getNickName());
        } else {
            mBaseView.jumpToLogin();
        }
    }

    /**
     * 跳转到对应版块
     *
     * @param position
     * @param fidString
     */
    @Override
    public void toTopicListPage(int position, String fidString) {
        if (position != 0 && !HttpUtil.HOST_PORT.equals("")) {
            HttpUtil.HOST = HttpUtil.HOST_PORT + HttpUtil.Servlet_timer;
        }
        int fid = 0;
        try {
            fid = Integer.parseInt(fidString);
        } catch (Exception e) {
            final String tag = this.getClass().getSimpleName();
            NLog.e(tag, NLog.getStackTraceString(e));
            NLog.e(tag, "invalid fid " + fidString);
        }
        if (fid == 0) {
            String tip = fidString + "绝对不存在";
            ToastUtils.showToast(tip);
            return;
        }

        NLog.i(this.getClass().getSimpleName(), "set host:" + HttpUtil.HOST);

        String url = HttpUtil.Server + "/thread.php?fid=" + fidString + "&rss=1";
        PhoneConfiguration config = PhoneConfiguration.getInstance();
        if (!StringUtils.isEmpty(config.getCookie())) {
            url = url + "&" + config.getCookie().replace("; ", "&");
        } else if (fid < 0) {
            mBaseView.jumpToLogin();
            return;
        }
        if (!StringUtils.isEmpty(url)) {
            Intent intent = new Intent();
            intent.putExtra("tab", "1");
            intent.putExtra("fid", fid);
            intent.setClass(mBaseView.getContext(), config.topicActivityClass);
            mBaseView.getContext().startActivity(intent);
        }
    }

    @Override
    public void notifyDataSetChanged() {
        loadBoardInfo();
        mBaseView.notifyDataSetChanged();
    }

    @Override
    public void startUserProfile(String uid) {
        ARouterUtils.build(ARouterConstants.ACTIVITY_PROFILE)
                .withString("mode", "uid")
                .withString("uid", uid)
                .navigation(mBaseView.getContext());
    }

    @Override
    public void startLogin() {
        mBaseView.jumpToLogin();
    }

    @Override
    public BoardCategory getBookmarkCategory() {
        return mBaseModel.getBookmarkCategory();
    }

    @Override
    public List<BoardCategory> getBoardCategories() {
        return mBaseModel.getBoardCategories();
    }

    @Override
    public void addBookmarkBoard(int fid, int stid, String name) {
        if (!mUserManager.hasValidUser()) {
            startLogin();
            return;
        }
        if (mBaseModel.isBookmark(fid, stid)) {
            ToastUtils.showToast("该版面已存在");
        } else {
            mBaseModel.addBookmark(fid, stid, name);
            mBaseView.notifyDataSetChanged();
        }
    }

    @Override
    public void showTopicList(Board board) {
        ARouterUtils.build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                .withInt(ParamKey.KEY_FID, board.getFid())
                .withInt(ParamKey.KEY_STID, board.getStid())
                .withString(ParamKey.KEY_TITLE, board.getName())
                .withString(ParamKey.BOARD_HEAD, board.getBoardHead())
                .navigation(mBaseView.getContext());
    }

    @Override
    public void showTopicList(int fid, int stid, String boardName) {
        ARouterUtils.build(ARouterConstants.ACTIVITY_TOPIC_LIST)
                .withInt(ParamKey.KEY_FID, fid)
                .withInt(ParamKey.KEY_STID, stid)
                .withString(ParamKey.KEY_TITLE, boardName)
                .navigation(mBaseView.getContext());
    }

    @Override
    public void showTopicList(String url) {
        Intent intent = new Intent(mBaseView.getContext(), TopicListActivity.class);
        intent.setData(Uri.parse(url));
        if (mBaseView.getContext() != null) {
            mBaseView.getContext().startActivity(intent);
        }
    }

    @Override
    public void showTopicContent(String url) {
        Intent intent = new Intent(mBaseView.getContext(), ArticleListActivity.class);
        intent.setData(Uri.parse(url));
        if (mBaseView.getContext() != null) {
            mBaseView.getContext().startActivity(intent);
        }
    }

    @Override
    protected BoardModel onCreateModel() {
        return BoardModel.getInstance();
    }
}
