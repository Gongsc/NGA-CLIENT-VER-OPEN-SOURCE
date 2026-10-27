package sp.phone.mvp.model;

import androidx.lifecycle.LifecycleOwner;

import sp.phone.util.ForumUtils;

/**
 * Created by Justwen on 2017/11/25.
 */

public abstract class BaseModel  {

    /** 请求随该页面销毁取消（见 gov.anzong.androidnga.coroutines.scopeOf）；未绑定页面时为 null。 */
    private volatile LifecycleOwner mLifecycleOwner;

    private String mDomain;

    public BaseModel() {
        mDomain = ForumUtils.getAvailableDomain();
    }

    public void detach() {
        mLifecycleOwner = null;
    }

    public void setLifecycleOwner(LifecycleOwner owner) {
        mLifecycleOwner = owner;
    }

    protected LifecycleOwner getLifecycleOwner() {
        return mLifecycleOwner;
    }


    protected String getAvailableDomain() {
        return mDomain;
    }
}
