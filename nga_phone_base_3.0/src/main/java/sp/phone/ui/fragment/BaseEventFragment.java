package sp.phone.ui.fragment;

import androidx.annotation.NonNull;

import gov.anzong.androidnga.coroutines.UiEventBus;
import gov.anzong.androidnga.coroutines.UiEvent;

/**
 * Created by Justwen on 2017/11/27.
 *
 * <p>订阅页面间事件，订阅随页面销毁结束。
 */
public class BaseEventFragment extends BaseFragment {

    protected void registerUiEvents() {
        UiEventBus.observe(this, this::onUiEvent);
    }

    protected void onUiEvent(@NonNull UiEvent event) {
    }
}
