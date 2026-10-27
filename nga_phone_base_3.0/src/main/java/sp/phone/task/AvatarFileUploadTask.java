package sp.phone.task;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import gov.anzong.androidnga.base.json.NgaJson;
import gov.anzong.androidnga.coroutines.BackgroundTask;

import gov.anzong.androidnga.common.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.Utils;
import gov.anzong.androidnga.base.util.ToastUtils;
import sp.phone.util.ImageUtils;
import sp.phone.util.ActivityUtils;
import sp.phone.util.ImageUtils;
import sp.phone.util.NLog;
import sp.phone.util.StringUtils;

public class AvatarFileUploadTask {
    @SuppressWarnings("unused")
    private static final String TAG = AvatarFileUploadTask.class
            .getSimpleName();
    private static final String BOUNDARY = "-----------------------------7db1c5232222b";
    private static final String ATTACHMENT_SERVER = "http://app.myauth.us/api/attach.php?";
    private static final String LOG_TAG = AvatarFileUploadTask.class
            .getSimpleName();
    final private Uri uri;
    /* private InputStream is; */
    private long filesize;
    private Context context;
    private onFileUploaded notifier;
    private String filename;
    @SuppressWarnings("unused")
    private String utfFilename;
    private String contentType;
    private String errorStr = "";

	/*
     * public FileUploadTask(InputStream is, long filesize, Context context,
	 * onFileUploaded notifier, String contentType) { super(); this.is = is;
	 * this.filesize = filesize; this.context = context; this.notifier =
	 * notifier; this.contentType = contentType; this.filename =
	 * contentType.replace('/', '.'); this.utfFilename = filename.substring(1);
	 * }
	 */

    public AvatarFileUploadTask(Context context, onFileUploaded notifier,
                                Uri uri) {
        this.context = context;
        this.notifier = notifier;
        this.uri = uri;
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /** 原 AsyncTask.execute：先显示进度条，在后台上传，结果在主线程处理。 */
    public void execute(String... params) {
        onPreExecute();
        BackgroundTask.run(() -> doInBackground(params), this::onPostExecute);
    }

    private void onPreExecute() {
        ActivityUtils.getInstance().noticeSayingWithProgressBar(context);
    }

    /** 原 AsyncTask.publishProgress：进度在主线程更新。 */
    private void publishProgress(int progress) {
        mainHandler.post(() -> onProgressUpdate(progress));
    }

    private void onProgressUpdate(Integer... values) {
        if (values[0] == -101) {
            ToastUtils.info(R.string.image_to_big2);
        } else {
            if (values[0] < 0 || values[0] > 100) {
                values[0] = 99;
            }
            ActivityUtils.getInstance().noticebarsetprogress(values[0]);
        }
    }

    private void onPostExecute(String result) {
        if (StringUtils.isEmpty(result)) {
            ToastUtils.error(errorStr);
        } else {
            NonameUploadResponse response = NgaJson.parseObject(result, NonameUploadResponse.class);
            if (response.error) {
                ToastUtils.error(response.errorinfo);
            } else {
                notifier.finishUpload(response.data, uri);
            }
        }
        ActivityUtils.getInstance().dismiss();
    }

    @SuppressWarnings("unused")
    private String doInBackground(String... params) {

        ContentResolver cr = context.getContentResolver();

        InputStream is = null;
        try {
            ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
            contentType = cr.getType(uri);
            if (StringUtils.isEmpty(contentType)) {
                errorStr = context.getResources().getString(
                        R.string.invalid_img_selected);
                return null;
            }
            filesize = pfd.getStatSize();
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri), null, opts);
            int width = opts.outWidth;
            int height = opts.outHeight;
            if (width > 255 || height > 180) {
                publishProgress(-101);
                byte[] img = ImageUtils.fitNGAImageToUpload(
                        cr.openInputStream(uri), opts);
                contentType = "image/png";
                filesize = img.length;
                is = new ByteArrayInputStream(img);
            }

            NLog.d(LOG_TAG, "file size =" + filesize);
            pfd.close();
            if (is == null)
                is = cr.openInputStream(uri);
        } catch (Exception e1) {
            e1.printStackTrace();
            errorStr = context.getResources().getString(
                    R.string.img_cannot_read);
            return null;
        }

        this.filename = contentType.replace('/', '.');
        this.utfFilename = filename.substring(1);

        final byte header[] = buildHeader().getBytes();
        final byte tail[] = buildTail().getBytes();

        String html = null;
        URL url;
        try {
            url = new URL(ATTACHMENT_SERVER);
            // NLog.d(LOG_TAG, "cookie:" + cookie);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + BOUNDARY);
            conn.setRequestProperty("Content-Length",
                    String.valueOf(header.length + filesize + tail.length));
            conn.setRequestProperty("Accept-Charset", "GBK");
            conn.setDoOutput(true);

            OutputStream out = conn.getOutputStream();

            byte[] buf = new byte[1024];
            int len;
            out.write(header);
            int ilen = 0, progress = 0;
            while ((len = is.read(buf)) != -1) {
                ilen += len;
                progress = (int) ((ilen / (float) filesize) * 100);
                if (progress == 100) {
                    progress = 99;
                }
                publishProgress(progress);
                out.write(buf, 0, len);
            }
            out.write(tail);

            is.close();
            InputStream httpInputStream = conn.getInputStream();
            html = StreamUtils.toString(httpInputStream, "gbk");
            out.close();
            publishProgress(100);

        } catch (Exception e) {
            errorStr = context.getResources().getString(
                    R.string.net_work_error);
            NLog.e(LOG_TAG, NLog.getStackTraceString(e));
            return null;
        }

        return html;
    }

    private String buildHeader() {
        StringBuilder sb = new StringBuilder();
        final String keys[] = {"v2", "attachment_file1_watermark",
                "attachment_file1_dscp", "attachment_file1_url_utf8_name",
                "fid", "func", "attachment_file1_img", "origin_domain", "lite"};
        final String values[] = {"1", "", "", filename, "-7", "upload", "1",
                Utils.getNGADomain(), "js"};

        for (int i = 0; i < keys.length; ++i) {
            sb = sb.append("--");
            sb = sb.append(BOUNDARY);
            sb = sb.append("\r\n");
            sb = sb.append("Content-Disposition: form-data; name=\"" + keys[i]
                    + "\"\r\n\r\n");
            sb = sb.append(values[i]);
            sb = sb.append("\r\n");
        }

        sb.append("--" + BOUNDARY + "\r\n");
        // sb.append("Content-Disposition: form-data; name=\"attachment_file1\"");
        sb.append("Content-Disposition: form-data; name=\"attachment_file1\"; ");
        sb.append("filename=\"");
        sb.append(filename);
        sb.append("\"");
        sb.append("\r\n");

        sb.append("Content-Type: ");
        sb.append(contentType);
        sb.append("\r\n\r\n");

        return sb.toString();

    }

    private String buildTail() {
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n--" + BOUNDARY + "--\r\n");

        return sb.toString();
    }

    public interface onFileUploaded {
        int finishUpload(String picUrl, Uri uri);
    }

    private static class NonameUploadResponse {
        public boolean error;
        public String errorinfo;
        public String data;
    }

}