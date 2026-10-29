package gov.anzong.androidnga.ui.fragment

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

open class BaseFragment : Fragment {

    private val mActivityViewModelProvider: ViewModelProvider by lazy { ViewModelProvider(activity as FragmentActivity) }

    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    constructor() : super()

    protected fun setTitle(title: String) {
        activity?.title = title
    }

    protected fun getActivityViewModelProvider(): ViewModelProvider {
        return mActivityViewModelProvider;
    }

    open fun onBackPressed() : Boolean {
        return false
    }
}
