package sp.phone.ui.fragment;

import androidx.annotation.NonNull;

import gov.anzong.androidnga.coroutines.UiEventBus;
import sp.phone.rxjava.RxEvent;

/**
 * Created by Justwen on 2017/11/27.
 *
 * <p>订阅页面间事件，订阅随页面销毁结束。
 */
public class BaseRxFragment extends BaseFragment {

    protected void registerRxBus() {
        UiEventBus.observe(this, this::accept);
    }

    protected void accept(@NonNull RxEvent rxEvent) {
    }
}
