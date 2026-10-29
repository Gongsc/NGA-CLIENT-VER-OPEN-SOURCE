package sp.phone.ui.fragment.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import gov.anzong.androidnga.ui.compose.NgaTheme
import sp.phone.util.NLog

/**
 * 内容由 Compose 绘制的对话框：窗口透明，[DialogContent] 绘制在带圆角的 Surface 中。
 * 使用 DialogFragment 以便旋转屏幕等重建后保留对话框与输入内容。
 */
abstract class BaseDialogFragment : AppCompatDialogFragment() {

    @Composable
    protected abstract fun DialogContent()

    fun show(fm: FragmentManager) = show(fm, javaClass.simpleName)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { NgaTheme { Surface(shape = MaterialTheme.shapes.extraLarge) { DialogContent() } } }
        }

    override fun onStart() {
        try {
            super.onStart()
        } catch (e: IllegalStateException) {
            NLog.d(e.message)
        }
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    companion object {
        @JvmStatic
        fun show(fm: FragmentManager, args: Bundle?, target: Class<out DialogFragment>) {
            try {
                target.getDeclaredConstructor().newInstance().apply { arguments = args }.show(fm, target.simpleName)
            } catch (e: ReflectiveOperationException) {
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }
}
