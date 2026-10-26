package gov.anzong.androidnga.ui.compose

import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun configureModernWindow(activity: ComponentActivity) {
    activity.enableEdgeToEdge()
}

fun applyLegacyWindowInsets(activity: ComponentActivity) {
    val root = activity.findViewById<View>(androidx.appcompat.R.id.action_bar_root)
        ?: activity.findViewById(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
        view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(root)
}
