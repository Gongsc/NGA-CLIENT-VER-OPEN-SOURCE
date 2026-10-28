package sp.phone.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.ViewModelProvider;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.mvvm.viewmodel.MessagePostViewModel;
import sp.phone.util.ActivityUtils;
import sp.phone.util.StringUtils;

/** Created by Justwen on 2017/5/28. */
public class MessagePostFragment extends BaseFragment {
    private EditText mTitleEditor;
    private EditText mRecipientEditor;
    private EditText mBodyEditor;

    private MessagePostViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(MessagePostViewModel.class);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_message_post, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTitleEditor = view.findViewById(R.id.et_title);
        mRecipientEditor = view.findViewById(R.id.et_recipient);
        mBodyEditor = view.findViewById(R.id.et_body);
        if (!mViewModel.isNewMessage()) {
            view.findViewById(R.id.panel_recipient).setVisibility(View.GONE);
        } else if (savedInstanceState == null) {
            setRecipient(mViewModel.getInitialRecipient());
        }
        mBodyEditor.requestFocus();

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                inflater.inflate(R.menu.message_post_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() != R.id.send) {
                    return false;
                }
                send();
                return true;
            }
        }, getViewLifecycleOwner());

        mViewModel.getResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) {
                return;
            }
            mViewModel.resultShown();
            ActivityUtils.getInstance().dismiss();
            showToast(result.message);
            if (result.success) {
                requireActivity().setResult(Activity.RESULT_OK);
                requireActivity().finish();
            }
        });
    }

    private void send() {
        String title = mTitleEditor.getText().toString();
        String recipient = mRecipientEditor.getText().toString();
        String body = mBodyEditor.getText().toString();
        if (StringUtils.isEmpty(recipient) && mViewModel.isNewMessage()) {
            mRecipientEditor.setError("请输入收件人");
            mRecipientEditor.requestFocus();
        } else if (StringUtils.isEmpty(body)) {
            mBodyEditor.setError("请输入内容");
            mBodyEditor.requestFocus();
        } else if (mViewModel.send(title, recipient, body)) {
            ActivityUtils.getInstance().noticeSaying(requireActivity());
        } else {
            showToast(R.string.avoidWindfury);
        }
    }

    private void setRecipient(String recipient) {
        mRecipientEditor.setText(recipient);
        if (recipient != null) {
            mRecipientEditor.setSelection(recipient.length());
        }
    }
}
