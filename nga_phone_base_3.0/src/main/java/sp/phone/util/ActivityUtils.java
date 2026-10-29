package sp.phone.util;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.activity.TopicHistoryActivity;
import gov.anzong.androidnga.base.util.ContextUtils;
import gov.anzong.androidnga.base.util.ToastUtils;
import sp.phone.common.PhoneConfiguration;
import sp.phone.common.UserManagerImpl;

public class ActivityUtils {

    public static final String dialogTag = "saying";
    static final String TAG = ActivityUtils.class.getSimpleName();
    static ActivityUtils instance;
    static Object lock = new Object();
    private DialogFragment df = null;

    public static final int REQUEST_CODE_TOPIC_POST = 3;

    public static final int REQUEST_CODE_JUMP_PAGE = 4;

    private ActivityUtils() {
    }

    public static void showToast(Context context, int resId) {
        if (context != null) {
            ToastUtils.showToast(resId);
        }
    }

    public static void showToast(Context context, String res) {
        if (context != null) {
            ToastUtils.showToast(res);
        }
    }

    public static void showToast(String res) {
        showToast(ContextUtils.getContext(), res);
    }

    public static void showToast(int resId) {
        showToast(ContextUtils.getContext(), resId);
    }

    public static ActivityUtils getInstance() {
        if (instance == null) {
            instance = new ActivityUtils();
        }
        return instance;//instance;

    }

    static public String getSaying() {
        String str = StringUtils.getSaying();
        if (str.indexOf(";") != -1) {
            str = str.replace(";", "-----");
        }

        return str;

    }

    public void noticeSaying(Context context) {

        String str = StringUtils.getSaying();
        if (str.indexOf(";") != -1) {
            notice("", str.replace(";", "-----"), context);
        } else {
            notice("", str, context);
        }
    }

    public void noticeSayingWithProgressBar(Context context) {

        String str = StringUtils.getSaying();
        if (str.indexOf(";") != -1) {
            noticebar("", str.replace(";", "-----"), context);
        } else {
            noticebar("", str, context);
        }
    }

    public void noticeSaying(String str, Context context) {

        if (str.indexOf(";") != -1) {
            notice("", str.replace(";", "-----"), context);
        } else {
            notice("", str, context);
        }
    }

    public void noticeError(String error, Context context) {
        if (context != null) {
            HttpUtil.switchServer();
            notice(context.getString(R.string.error), error, context);
        }
    }

    private void notice(String title, String content, Context c) {

        if (c == null)
            return;
        NLog.d(TAG, "saying dialog");
        Bundle b = new Bundle();
        b.putString("title", title);
        b.putString("content", content);
        synchronized (lock) {
            try {

                DialogFragment df = new SayingDialogFragment();
                df.setArguments(b);

                FragmentActivity fa = (FragmentActivity) c;
                FragmentManager fm = fa.getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();

                Fragment prev = fm.findFragmentByTag(dialogTag);
                if (prev != null) {
                    ft.remove(prev);
                }

                ft.commit();
                df.show(fm, dialogTag);
                this.df = df;
            } catch (Exception e) {
                NLog.e(this.getClass().getSimpleName(), NLog.getStackTraceString(e));

            }

        }

    }

    private void noticebar(String title, String content, Context c) {

        if (c == null)
            return;
        NLog.d(TAG, "saying dialog");
        Bundle b = new Bundle();
        b.putString("title", title);
        b.putString("content", content);
        synchronized (lock) {
            try {

                DialogFragment df = new SayingDialogFragmentWithProgressBar();
                df.setArguments(b);

                FragmentActivity fa = (FragmentActivity) c;
                FragmentManager fm = fa.getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();

                Fragment prev = fm.findFragmentByTag(dialogTag);
                if (prev != null) {
                    ft.remove(prev);
                }

                ft.commit();
                df.show(fm, dialogTag);
                this.df = df;
            } catch (Exception e) {
                NLog.e(this.getClass().getSimpleName(), NLog.getStackTraceString(e));

            }

        }

    }

    public void noticebarsetprogress(int i) {
        NLog.d(TAG, "trying setprocess" + String.valueOf(i));
        if (df != null && df.getActivity() != null) {
            if (df instanceof SayingDialogFragmentWithProgressBar) {
                ((SayingDialogFragmentWithProgressBar) df).setProgress(i);
            }
        }
    }

    public void clear() {
        synchronized (lock) {
            this.df = null;
        }
    }

    public void dismiss() {

        synchronized (lock) {
            NLog.d(TAG, "trying dissmiss dialog");

            if (df != null && df.getActivity() != null) {
                NLog.d(TAG, "dissmiss dialog");

                try {
                    FragmentActivity fa = (FragmentActivity) (df.getActivity());
                    FragmentManager fm = fa.getSupportFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();

                    Fragment prev = fm.findFragmentByTag(dialogTag);
                    if (prev != null) {
                        ft.remove(prev);

                    }

                    ft.commit();
                } catch (Exception e) {
                    NLog.e(this.getClass().getSimpleName(), NLog.getStackTraceString(e));
                }

                df = null;

            } else {
                df = null;
            }

        }
    }

    /** 带进度条的提示框（如上传头像），进度由 {@link #setProgress} 更新。 */
    public static class SayingDialogFragmentWithProgressBar extends DialogFragment {

        LinearProgressIndicator progress;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            progress = new LinearProgressIndicator(requireContext());
            progress.setMax(100);
            return progressDialog(requireContext(), getArguments(), progress, false);
        }

        public void setProgress(int i) {
            if (progress != null) {
                progress.setProgressCompat(i, true);
            }
        }

    }

    /** 不确定进度的提示框（如“正在提交”）。 */
    public static class SayingDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            CircularProgressIndicator progress = new CircularProgressIndicator(requireContext());
            progress.setIndeterminate(true);
            return progressDialog(requireContext(), getArguments(), progress, true);
        }

    }

    /** 替代已弃用的 ProgressDialog：标题与提示文字取自参数，提示为空时显示随机名言；点击外部可关闭。 */
    private static Dialog progressDialog(Context context, Bundle args, View indicator, boolean inline) {
        String title = args != null ? args.getString("title") : null;
        String content = args != null ? args.getString("content") : null;
        if (StringUtils.isEmpty(content)) {
            content = ActivityUtils.getSaying();
        }
        int padding = Math.round(24 * context.getResources().getDisplayMetrics().density);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(inline ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        layout.setGravity(inline ? Gravity.CENTER_VERTICAL : Gravity.NO_GRAVITY);
        layout.setPadding(padding, padding / 2, padding, padding / 2);
        TextView message = new TextView(context);
        message.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        message.setText(content);
        if (inline) {
            layout.addView(indicator);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMarginStart(padding / 2);
            layout.addView(message, params);
        } else {
            layout.addView(message);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = padding / 2;
            layout.addView(indicator, params);
        }
        Dialog dialog = new MaterialAlertDialogBuilder(context).setTitle(title).setView(layout).create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public static void startLoginActivity(Context context) {
        Intent intent = new Intent(context, PhoneConfiguration.getInstance().loginActivityClass);
        context.startActivity(intent);
    }

    public static void startFavoriteTopicActivity(Context context) {
        if (UserManagerImpl.getInstance().getActiveUser() == null) {
            startLoginActivity(context);
        } else {
            Intent intent = new Intent(context, PhoneConfiguration.getInstance().topicActivityClass);
            intent.putExtra("favor", 1);
            context.startActivity(intent);
        }
    }

    public static void startRecommendTopicActivity(Context context, Intent intent) {
        if (UserManagerImpl.getInstance().getActiveUser() == null) {
            startLoginActivity(context);
        } else {
            intent.setClass(context, PhoneConfiguration.getInstance().topicActivityClass);
            context.startActivity(intent);
        }
    }

    public static void startTwentyFourActivity(Context context, Intent intent) {
        if (UserManagerImpl.getInstance().getActiveUser() == null) {
            startLoginActivity(context);
        } else {
            intent.setClass(context, PhoneConfiguration.getInstance().topicActivityClass);
            context.startActivity(intent);
        }
    }

    public static void startHistoryTopicActivity(Context context) {
        context.startActivity(new Intent(context, TopicHistoryActivity.class));
    }
}
