package sp.phone.mvp.presenter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;

import gov.anzong.androidnga.R;
import gov.anzong.androidnga.base.util.ToastUtils;
import gov.anzong.androidnga.common.util.EmoticonUtils;
import gov.anzong.androidnga.http.OnHttpCallBack;

import sp.phone.mvp.contract.TopicPostContract;
import sp.phone.mvp.model.TopicPostModel;
import sp.phone.param.PostParam;
import sp.phone.ui.fragment.TopicPostFragment;
import sp.phone.util.FunctionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TopicPostPresenter extends BasePresenter<TopicPostFragment, TopicPostModel>
        implements TopicPostContract.Presenter {

    private boolean mLoading;

    private PostParam mPostParam;

    @Override
    public void setEmoticon(String emotion) {
        String urlTemp = emotion.replaceAll("\\n", "");
        if (urlTemp.contains("http")) {
            urlTemp = urlTemp.substring(5, urlTemp.length() - 6);
            String sourceFile = EmoticonUtils.getPathByURI(urlTemp);
            try (InputStream is =
                    mBaseView.getContext().getResources().getAssets().open(sourceFile)) {
                if (is != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    Drawable drawable =
                            new BitmapDrawable(mBaseView.getContext().getResources(), bitmap);
                    drawable.setBounds(
                            0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                    SpannableString spanString = new SpannableString(emotion);
                    ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
                    spanString.setSpan(
                            span, 0, emotion.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    mBaseView.insertBodyText(spanString);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            int[] emotions = {
                1, 2, 3, 24, 25, 26, 27, 28, 29, 30, 32, 33, 34, 35, 36, 37, 38, 39, 4, 40, 41, 42,
                43, 5, 6, 7, 8
            };
            for (int i = 0; i < 27; i++) {
                if (emotion.indexOf("[s:" + emotions[i] + "]") == 0) {
                    String sourceFile = "a" + emotions[i] + ".gif";
                    try (InputStream is =
                            mBaseView.getContext().getResources().getAssets().open(sourceFile)) {
                        if (is != null) {
                            Bitmap bitmap = BitmapFactory.decodeStream(is);
                            Drawable drawable = new BitmapDrawable(bitmap);
                            drawable.setBounds(
                                    0,
                                    0,
                                    drawable.getIntrinsicWidth(),
                                    drawable.getIntrinsicHeight());
                            SpannableString spanString = new SpannableString(emotion);
                            ImageSpan span = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
                            spanString.setSpan(
                                    span, 0, emotion.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            mBaseView.insertBodyText(spanString);
                        } else {
                            mBaseView.insertBodyText(emotion);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    } //

    @Override
    public void setPostParam(PostParam postParam) {
        mPostParam = postParam;
        mBaseModel.getPostInfo(
                mPostParam,
                new OnHttpCallBack<PostParam>() {
                    @Override
                    public void onError(String text) {
                        if (mBaseView != null) {
                            mBaseView.setAttachmentReady(false);
                            mBaseView.setEditorError(text);
                        }
                    }

                    @Override
                    public void onSuccess(PostParam data) {
                        mPostParam = data;
                        if (mBaseView != null) mBaseView.setAttachmentReady(true);
                    }
                });
    }

    @Override
    public void onViewCreated() {
        if (!TextUtils.isEmpty(mPostParam.getPostSubject())) {
            mBaseView.insertTitleText(mPostParam.getPostSubject());
        }
        if (!TextUtils.isEmpty(mPostParam.getPostContent())) {
            mBaseView.insertBodyText(mPostParam.getPostContent());
        }
        super.onViewCreated();
    }

    @Override
    public void post(String title, String body, boolean isAnony) {
        if (mLoading) {
            mBaseView.showToast(R.string.avoidWindfury);
            return;
        }
        if (body.trim().isEmpty()
                || ("new".equals(mPostParam.getPostAction()) && title.trim().isEmpty())) {
            mBaseView.setEditorError("请填写标题及正文后提交。");
            return;
        }
        mLoading = true;
        mBaseView.setSubmitting(true);
        mBaseView.setEditorError(null);
        mPostParam.setAnonymous(isAnony);
        mPostParam.setPostSubject(title);
        mPostParam.setPostContent(FunctionUtils.ColorTxtCheck(body));
        mBaseModel.post(
                mPostParam,
                new OnHttpCallBack<String>() {
                    @Override
                    public void onError(String text) {
                        onPostFinished(false, text);
                    }

                    @Override
                    public void onSuccess(String data) {
                        onPostFinished(true, data);
                    }
                });
    }

    @Override
    public void showFilePicker() {
        mBaseView.showFilePicker();
    }

    @Override
    public void startUploadTask(final Uri uri) {
        mBaseView.showUploadFileProgressBar();
        mBaseModel.uploadFile(
                uri,
                mPostParam,
                new OnHttpCallBack<String>() {
                    @Override
                    public void onError(String text) {
                        if (mBaseView != null) {
                            mBaseView.hideUploadFileProgressBar();
                            mBaseView.setEditorError(text);
                        }
                    }

                    @Override
                    public void onSuccess(String data) {
                        if (mBaseView != null) {
                            mBaseView.hideUploadFileProgressBar();
                            ToastUtils.success("上传成功");
                            finishUpload(data, uri);
                        }
                    }
                });
    }

    @Override
    public void insertAtFormat() {
        mBaseView.insertBodyText("[@]", 2);
    }

    @Override
    public void insertQuoteFormat() {
        mBaseView.insertBodyText("[quote][/quote]", "[quote]".length());
    }

    @Override
    public void insertUrlFormat() {
        mBaseView.insertBodyText("[url][/url]", "[url]".length());
    }

    @Override
    public void insertBoldFormat() {
        mBaseView.insertBodyText("[b][/b]", "[b]".length());
    }

    @Override
    public void insertItalicFormat() {
        mBaseView.insertBodyText("[i][/i]", "[i]".length());
    }

    @Override
    public void insertUnderLineFormat() {
        mBaseView.insertBodyText("[u][/u]", "[u]".length());
    }

    @Override
    public void insertDeleteLineFormat() {
        mBaseView.insertBodyText("[del][/del]", "[del]".length());
    }

    @Override
    public void insertCollapseFormat() {
        mBaseView.insertBodyText("[collapse][/collapse]", "[collapse]".length());
    }

    @Override
    public void insertFontColorFormat(String fontColor) {
        mBaseView.insertBodyText(fontColor, fontColor.length() - "[/color]".length());
    }

    @Override
    public void insertFontSizeFormat(String fontSize) {
        mBaseView.insertBodyText(fontSize, "[size=100%]".length());
    }

    @Override
    public void insertTopicCategory(String category) {
        mBaseView.insertTitleText(category);
    }

    @Override
    public void loadTopicCategory(OnHttpCallBack<List<String>> callBack) {
        mBaseModel.loadTopicCategory(mPostParam, callBack);
    }

    private void onPostFinished(boolean isSuccess, String result) {
        if (mBaseView != null) {
            mBaseView.setSubmitting(false);
            if (!isSuccess) mBaseView.setEditorError(result == null ? "提交失败，请稍后重试。" : result);
            if (isSuccess) {
                mBaseView.setResult(Activity.RESULT_OK);
                mBaseView.finish();
            }
        }
        mLoading = false;
    }

    private void finishUpload(String picUrl, Uri uri) {
        mBaseView.insertFile(null, "[img]./" + picUrl + "[/img]");
    }

    @Override
    protected TopicPostModel onCreateModel() {
        return new TopicPostModel();
    }
}
