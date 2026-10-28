package sp.phone.param;

/** 设置头像页填写的头像地址，提交见 {@link sp.phone.http.post.ForumActionClient#setAvatar}。 */
public class AvatarPostAction {
    private String icon_;

    public void seticon_(String icon) {
        icon_ = icon;
    }

    public String geticon_() {
        return icon_;
    }
}
