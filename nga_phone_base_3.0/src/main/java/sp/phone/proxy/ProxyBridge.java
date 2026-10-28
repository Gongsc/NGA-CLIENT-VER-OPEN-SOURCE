package sp.phone.proxy;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import gov.anzong.androidnga.coroutines.BackgroundTask;
import sp.phone.http.post.ForumActionClient;
import sp.phone.http.post.NgaWriteClient;
import sp.phone.util.ActivityUtils;
import sp.phone.util.StringUtils;

public final class ProxyBridge {

    Context context;
    Toast toast;

    public ProxyBridge(Context ccontext, Toast mtoast) {
        // TODO Auto-generated constructor stub
        context = ccontext;
        toast = mtoast;
    }

    /** 正文中投票脚本的提交入口，只接受投票，见 {@link ForumActionClient#vote}。 */
    @JavascriptInterface
    public void postURL(String query) {
        ActivityUtils.getInstance().noticeSaying("正在提交...", context);
        BackgroundTask.run(() -> ForumActionClient.create().vote(query), this::onPostExecute);
    }

    private void onPostExecute(NgaWriteClient.ActionResult result) {
        ActivityUtils.getInstance().dismiss();
        String message = result.message;
        if (StringUtils.isEmpty(message))
            message = result.success ? "操作成功" : "未知错误,请重试";
        if (message.startsWith("操作成功"))
            message = "操作成功";
        if (toast != null) {
            toast.setText(message);
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
