package sp.phone.param;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class PostParam implements Parcelable {

    private boolean mAnonymous;

    private StringBuilder mAttachments;

    private StringBuilder mAttachmentsCheck;

    private String mAuthCode;

    private String mPostAction;

    private String mPostContent;

    private int mPostFid;

    private String mPostPid;

    private String mPostSubject;

    private String mPostTid;

    private String mStid;

    public static final Creator<PostParam> CREATOR = new Creator<PostParam>() {

        @Override
        public PostParam createFromParcel(Parcel in) {
            return new PostParam(in);
        }

        @Override
        public PostParam[] newArray(int size) {
            return new PostParam[size];
        }
    };

    protected PostParam(Parcel in) {
        mPostPid = in.readString();
        mPostAction = in.readString();
        mPostFid = in.readInt();
        mPostTid = in.readString();
        mPostSubject = in.readString();
        mPostContent = in.readString();
        mAnonymous = in.readByte() != (byte) 0;
        mAuthCode = in.readString();
        mStid = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPostPid);
        dest.writeString(mPostAction);
        dest.writeInt(mPostFid);
        dest.writeString(mPostTid);
        dest.writeString(mPostSubject);
        dest.writeString(mPostContent);
        dest.writeByte((byte) (mAnonymous ? 1 : 0));
        dest.writeString(mAuthCode);
        dest.writeString(mStid);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getPostAction() {
        return mPostAction;
    }

    public synchronized String getAuthCode() {
        return mAuthCode;
    }

    public synchronized void setAuthCode(String authCode) {
        mAuthCode = authCode;
    }

    public PostParam(String tid, String subject, String content) {
        mPostTid = tid;
        mPostSubject = subject;
        mPostContent = content;
    }

    public void setPostAction(String postAction) {
        mPostAction = postAction;
    }

    public void setAnonymous(boolean anonymous) {
        mAnonymous = anonymous;
    }

    public boolean isAnonymous() {
        return mAnonymous;
    }

    /** 已表单编码的附件字段，多个附件以 %09 分隔；没有附件时为 null。 */
    public String getAttachments() {
        return mAttachments != null ? mAttachments.toString() : null;
    }

    public String getAttachmentsCheck() {
        return mAttachmentsCheck != null ? mAttachmentsCheck.toString() : null;
    }

    public String getPostSubject() {
        return mPostSubject;
    }

    public void setPostSubject(String postSubject) {
        mPostSubject = postSubject;
    }

    public String getPostContent() {
        return mPostContent;
    }

    public void setPostContent(String postContent) {
        mPostContent = postContent;
    }

    public String getPostPid() {
        return mPostPid;
    }

    public void setPostPid(String postPid) {
        mPostPid = postPid;
    }

    public int getPostFid() {
        return mPostFid;
    }

    public void setPostFid(int postFid) {
        mPostFid = postFid;
    }

    public String getPostTid() {
        return mPostTid;
    }

    public String getStid() {
        return mStid;
    }

    public void setStid(String stid) {
        mStid = stid;
    }

    public void appendAttachment(String attachment, String attachmentCheck) {
        try {
            if (mAttachments == null) {
                mAttachments = new StringBuilder();
                mAttachmentsCheck = new StringBuilder();
            }
            mAttachments.append(URLEncoder.encode("\t", "GBK")).append(attachment);
            mAttachmentsCheck.append(URLEncoder.encode("\t", "GBK")).append(attachmentCheck);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}