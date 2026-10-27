package sp.phone.mvp.model;

import androidx.lifecycle.LifecycleOwner;

import com.trello.rxlifecycle2.LifecycleProvider;
import com.trello.rxlifecycle2.android.FragmentEvent;

import sp.phone.util.ForumUtils;

/**
 * Created by Justwen on 2017/11/25.
 */

public abstract class BaseModel  {

    private volatile LifecycleProvider<FragmentEvent> mProvider;

    /** 协程请求随该页面销毁取消；迁移完成后取代 {@link #mProvider}。 */
    private volatile LifecycleOwner mLifecycleOwner;

    private String mDomain;

    public BaseModel() {
        mDomain = ForumUtils.getAvailableDomain();
    }

    public void detach() {
        mProvider = null;
        mLifecycleOwner = null;
    }

    public void setLifecycleOwner(LifecycleOwner owner) {
        mLifecycleOwner = owner;
    }

    protected LifecycleOwner getLifecycleOwner() {
        return mLifecycleOwner;
    }

    public void setLifecycleProvider(LifecycleProvider<FragmentEvent> provider) {
        mProvider = provider;
    }

    protected LifecycleProvider<FragmentEvent> getLifecycleProvider() {
        return mProvider;
    }

    protected String getAvailableDomain() {
        return mDomain;
    }
}
