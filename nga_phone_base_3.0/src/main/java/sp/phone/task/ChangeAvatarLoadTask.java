package sp.phone.task;

import android.graphics.Bitmap;
import gov.anzong.androidnga.coroutines.BackgroundTask;
import android.widget.ImageView;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import sp.phone.util.ImageUtils;
import sp.phone.util.HttpUtil;
import sp.phone.util.NLog;

public class ChangeAvatarLoadTask {

    public interface ChangeAvatarLoadCompleteCallBack {
        void OnAvatarLoadStart(String url);

        void OnAvatarLoadComplete(String url, Bitmap result);
    }

    static final String TAG = ChangeAvatarLoadTask.class.getSimpleName();
    final ImageView view;
    final boolean downImg;
    final int floor;
    final ChangeAvatarLoadCompleteCallBack callBack;
    String uri = null;

    public ChangeAvatarLoadTask(ImageView view, int floor, ChangeAvatarLoadCompleteCallBack callBack) {
        this.view = view;
        this.downImg = true;
        this.floor = floor;
        this.callBack = callBack;
    }


    /** 原 AsyncTask.execute：在后台下载头像，结果在主线程显示。 */
    public void execute(String... params) {
        BackgroundTask.run(() -> doInBackground(params), this::onPostExecute);
    }

    @SuppressWarnings("resource")
    private Bitmap doInBackground(String... params) {

        final String avatarUrl = params[0];
        final String avatarLocalPath = params[1];
        uri = avatarUrl;
        callBack.OnAvatarLoadStart(uri);

        Bitmap bitmap = null;
        InputStream is = null;
        HttpUtil.downImage(avatarUrl, avatarLocalPath);
        try {
            is = new FileInputStream(avatarLocalPath);
            //NLog.d(TAG,
            //		"download avatar from " + avatarUrl);

        } catch (FileNotFoundException e) {
            NLog.d(TAG,
                    "avatar " + avatarUrl + " is failed to download");
        }

        if (is != null) {
            //NLog.d(TAG,"load avatar from file: " + avatarLocalPath);
            bitmap = ImageUtils.loadAvatarFromSdcard(avatarLocalPath);
        }


        return bitmap;
    }


    private void onPostExecute(Bitmap result) {
        if (result != null) {
            view.setImageBitmap(result);
        }
        callBack.OnAvatarLoadComplete(uri, result);
    }


}
