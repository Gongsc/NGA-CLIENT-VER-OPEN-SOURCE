package gov.anzong.androidnga.ui.compose

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ActionIcons {
    private fun vector(name: String, draw: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        ImageVector.Builder(name,24.dp,24.dp,24f,24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = draw)
        }.build()
    val Refresh = vector("Refresh") { moveTo(20f,7f);lineTo(20f,12f);lineTo(15f,12f);moveTo(4f,17f);lineTo(4f,12f);lineTo(9f,12f);moveTo(6f,6f);curveTo(11f,1f,18f,4f,20f,11f);moveTo(18f,18f);curveTo(13f,23f,6f,20f,4f,13f) }
    val Restore = vector("Restore") { moveTo(3f,4f);lineTo(3f,10f);lineTo(9f,10f);moveTo(4f,10f);curveTo(5f,3f,16f,1f,20f,8f);curveTo(26f,19f,9f,26f,4f,17f);moveTo(12f,7f);lineTo(12f,12f);lineTo(15f,14f) }
    val Compact = vector("Compact") { for (y in listOf(5f,12f,19f)) { moveTo(3f,y);lineTo(5f,y);moveTo(9f,y);lineTo(21f,y) } }
    val Grid = vector("Grid") { for(x in listOf(4f,14f)) for(y in listOf(4f,14f)) { moveTo(x,y);lineTo(x+6,y);lineTo(x+6,y+6);lineTo(x,y+6);close() } }
    val First = vector("First") { moveTo(5f,5f);lineTo(5f,19f);moveTo(17f,5f);lineTo(10f,12f);lineTo(17f,19f) }
    val Left = vector("Left") { moveTo(15f,5f);lineTo(8f,12f);lineTo(15f,19f) }
    val Right = vector("Right") { moveTo(9f,5f);lineTo(16f,12f);lineTo(9f,19f) }
    val Down = vector("Down") { moveTo(7f,10f);lineTo(12f,15f);lineTo(17f,10f) }
    val Reply = vector("Reply") { moveTo(4f,5f);lineTo(20f,5f);lineTo(20f,17f);lineTo(9f,17f);lineTo(4f,21f);close();moveTo(8f,9f);lineTo(16f,9f);moveTo(8f,13f);lineTo(13f,13f) }
    val ThumbUp = vector("ThumbUp") { moveTo(7f,10f);lineTo(7f,21f);moveTo(4f,10f);lineTo(7f,10f);lineTo(12f,3f);curveTo(13.8f,3f,15.3f,4.6f,14.8f,6.3f);lineTo(14f,10f);lineTo(19.6f,10f);curveTo(20.9f,10f,21.9f,11.3f,21.5f,12.6f);lineTo(19.4f,19.6f);curveTo(19.1f,20.4f,18.4f,21f,17.5f,21f);lineTo(4f,21f);curveTo(3.4f,21f,3f,20.6f,3f,20f);lineTo(3f,11f);curveTo(3f,10.4f,3.4f,10f,4f,10f);close() }
    val Download = vector("Download") { moveTo(12f,4f);lineTo(12f,15f);moveTo(7f,10f);lineTo(12f,15f);lineTo(17f,10f);moveTo(5f,20f);lineTo(19f,20f) }
    val Link = vector("Link") { moveTo(10f,14f);lineTo(14f,10f);moveTo(9f,7f);lineTo(11f,5f);curveTo(13f,3f,17f,3f,19f,5f);curveTo(21f,7f,21f,11f,19f,13f);lineTo(17f,15f);moveTo(15f,17f);lineTo(13f,19f);curveTo(11f,21f,7f,21f,5f,19f);curveTo(3f,17f,3f,13f,5f,11f);lineTo(7f,9f) }
    val Appearance =vector("Appearance") { moveTo(12f,3f);curveTo(4f,3f,0f,12f,5f,18f);curveTo(8f,22f,18f,23f,15f,17f);curveTo(13f,13f,22f,17f,21f,10f);curveTo(20f,5f,16f,3f,12f,3f);moveTo(7f,9f);lineTo(7f,9.1f);moveTo(11f,6f);lineTo(11f,6.1f);moveTo(16f,7f);lineTo(16f,7.1f) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionIcon(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
               enabled: Boolean = true) {
    TooltipBox(positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        IconButton(onClick = onClick, enabled = enabled, modifier = modifier) { Icon(icon, contentDescription = label) }
    }
}
