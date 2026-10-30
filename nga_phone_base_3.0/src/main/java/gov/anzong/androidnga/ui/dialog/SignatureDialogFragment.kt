package gov.anzong.androidnga.ui.dialog

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gov.anzong.androidnga.ui.compose.ArticleHtml
import gov.anzong.androidnga.ui.compose.NgaDialogLayout

/** 楼层作者签名；arguments 为 "name" 与已转换的签名网页 "html"。 */
class SignatureDialogFragment : BaseDialogFragment() {

    @Composable
    override fun DialogContent() {
        NgaDialogLayout("${arguments?.getString("name").orEmpty()}的签名", buttons = { TextButton(onClick = { dismiss() }) { Text("关闭") } }) {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                ArticleHtml(arguments?.getString("html").orEmpty(), emptyList())
            }
        }
    }
}
