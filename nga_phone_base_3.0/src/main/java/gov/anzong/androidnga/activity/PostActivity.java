package gov.anzong.androidnga.activity;

import android.content.Intent;
import android.os.Bundle;


import gov.anzong.androidnga.R;

import sp.phone.param.ParamKey;
import sp.phone.param.PostParam;
import sp.phone.ui.fragment.TopicPostFragment;

public class PostActivity extends BaseActivity {

    private TopicPostFragment mPostFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setToolbarEnabled(true);
        super.onCreate(savedInstanceState);
        PostParam act = getPostParam();
        if (act.getPostAction() == null) {
            finish();
            return;
        }
        setTitle(getTitleResId(act.getPostAction()));
        Bundle bundle = new Bundle();
        bundle.putString(ParamKey.KEY_ACTION, act.getPostAction());
        bundle.putParcelable("param", act);
        mPostFragment =
                (TopicPostFragment)
                        getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (mPostFragment == null) {
            mPostFragment = new TopicPostFragment();
            mPostFragment.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, mPostFragment)
                    .commit();
        }
    }

    private int getTitleResId(String action) {
        switch (action) {
            case "reply":
                return R.string.reply_thread;
            case "modify":
                return R.string.modify_thread;
            default:
                return R.string.new_thread;
        }
    }

    private PostParam getPostParam() {
        Intent intent = getIntent();
        String tid = intent.getStringExtra(ParamKey.KEY_TID);
        int fid = intent.getIntExtra(ParamKey.KEY_FID, -7);
        String title = intent.getStringExtra("title");
        String pid = intent.getStringExtra(ParamKey.KEY_PID);
        String action = intent.getStringExtra(ParamKey.KEY_ACTION);
        String prefix = intent.getStringExtra("prefix");
        String stid = intent.getStringExtra(ParamKey.KEY_STID);
        PostParam act = new PostParam(tid, "", "");
        act.setPostAction(action);
        act.setPostFid(fid);
        act.setPostPid(pid);
        act.setPostContent(prefix);
        act.setPostSubject(title);
        act.setStid(stid);
        return act;
    }

    @Override
    protected boolean usesComposeContent() {
        return true;
    }
}
