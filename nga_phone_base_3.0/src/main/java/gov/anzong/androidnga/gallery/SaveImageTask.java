package gov.anzong.androidnga.gallery;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import gov.anzong.androidnga.base.util.ContextUtils;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import org.reactivestreams.Subscription;

import sp.phone.http.OnSimpleHttpCallBack;
import sp.phone.rxjava.BaseSubscriber;
import sp.phone.util.ActivityUtils;

import java.io.File;

public class SaveImageTask {

    private Context mContext;

    private int mDownloadCount;

    private Subscription mSubscription;

    public SaveImageTask() {
        mContext = ContextUtils.getContext();
    }

    public static class DownloadResult {

        File file;

        String url;

        Uri uri;

        public DownloadResult(File file, String url) {
            this.file = file;
            this.url = url;
        }
    }

    public void execute(OnSimpleHttpCallBack<DownloadResult> callBack, String... urls) {

        if (isRunning()) {
            ActivityUtils.showToast("图片正在下载，防止风怒！！");
            return;
        }

        mDownloadCount = 0;
        Observable.fromArray(urls)
                .observeOn(Schedulers.io())
                .map(
                        url -> {
                            File file =
                                    Glide.with(mContext)
                            .load(url)
                                            .downloadOnly(
                                                    Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .get();
                    return new DownloadResult(file, url);
                })
                .map(
                        result -> {
                            String extension =
                                    android.webkit.MimeTypeMap.getFileExtensionFromUrl(result.url);
                            if (extension.isEmpty()) extension = "jpg";
                            String mimeType =
                                    android.webkit.MimeTypeMap.getSingleton()
                                            .getMimeTypeFromExtension(
                                                    extension.toLowerCase(java.util.Locale.ROOT));
                            if (mimeType == null) mimeType = "image/jpeg";
                            String name = "nga_" + java.util.UUID.randomUUID() + "." + extension;
                            result.uri =
                                    gov.anzong.androidnga.util.SharedStorage.publish(
                                            mContext,
                                            result.file,
                                            android.provider.MediaStore.Images.Media
                                                    .EXTERNAL_CONTENT_URI,
                                            Environment.DIRECTORY_PICTURES + "/NGA",
                                            name,
                                            mimeType);
                            return result;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new BaseSubscriber<DownloadResult>() {
                    @Override
                    public void onNext(DownloadResult result) {
                        mDownloadCount++;
                        if (mDownloadCount == urls.length) {
                            if (urls.length > 1) {
                                ActivityUtils.showToast("所有图片已保存");
                            } else {
                                        ActivityUtils.showToast("图片已保存至相册 NGA 文件夹");
                            }
                        }
                        callBack.onResult(result);
                    }

                    @Override
                    public void onComplete() {
                        mSubscription = null;
                    }

                    @Override
                    public void onSubscribe(Subscription subscription) {
                        super.onSubscribe(subscription);
                        mSubscription = subscription;
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        mSubscription = null;
                        ActivityUtils.showToast("下载失败");
                    }
                });
    }

    private boolean isRunning() {
        return mSubscription != null;
    }
}
