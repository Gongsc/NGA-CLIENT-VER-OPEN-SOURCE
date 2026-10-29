package sp.phone.ui.fragment.dialog

import android.content.Intent
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import gov.anzong.androidnga.gallery.ImageZoomActivity
import gov.anzong.androidnga.ui.compose.NgaDialogLayout
import sp.phone.util.ImageUtils

/** 楼层作者头像；arguments 为 "name" 与 "url"，点击头像查看大图。 */
class AvatarDialogFragment : BaseDialogFragment() {

    @Composable
    override fun Content() {
        val name = arguments?.getString("name").orEmpty()
        val url = arguments?.getString("url")
        NgaDialogLayout("${name}的头像", buttons = { TextButton(onClick = { dismiss() }) { Text("关闭") } }) {
            AndroidView(factory = { ImageView(it).apply { contentDescription = "${name}的头像" } },
                modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally).clickable(enabled = !url.isNullOrEmpty()) {
                    startActivity(Intent(requireContext(), ImageZoomActivity::class.java).putExtra(ImageZoomActivity.KEY_GALLERY_CUR_URL, url))
                }, update = { ImageUtils.loadAvatar(it, url) })
        }
    }
}
