package gov.anzong.androidnga.account;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * @author Justwen
 * @date 2017/12/26
 */
@Entity(tableName = "users")
public class User {

    @ColumnInfo(name = "cid")
    public String mCid;

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "uid")
    public String mUserId;

    @ColumnInfo(name = "nick_name")
    public String mNickName;

    @ColumnInfo(name = "avatar_url")
    public String mAvatarUrl;

    public User() {
    }

    @Ignore
    public User(@NonNull String userId, String nickName, String cid) {
        mUserId = userId;
        mNickName = nickName;
        mCid = cid;
    }

    @Ignore
    public User(@NonNull String userId, String nickName) {
        mUserId = userId;
        mNickName = nickName;
    }

    public String getAvatarUrl() {
        return mAvatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        mAvatarUrl = avatarUrl;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String userId) {
        mUserId = userId;
    }

    public String getCid() {
        return mCid;
    }

    public void setCid(String cid) {
        mCid = cid;
    }

    public String getNickName() {
        return mNickName;
    }

    public void setNickName(String nickName) {
        mNickName = nickName;
    }

    /** 同一 UID 即为同一账号。 */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && java.util.Objects.equals(mUserId, ((User) obj).getUserId());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(mUserId);
    }
}
