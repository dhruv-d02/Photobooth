package com.dj.photobooth.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dj.photobooth.theme.PhotoboothColors

// The four-pointed sparkle glyph - design/handoff/README.md's Motifs section: "only for
// celebration/confirmation moments (saved, strip ready)". Path verified against the dc.html's
// inline SVG (`M12 0 L14.5 9 L24 12 L14.5 15 L12 24 L9.5 15 L0 12 L9.5 9 Z`, viewBox 24x24).
private val SparklePoints = listOf(
    12f to 0f, 14.5f to 9f, 24f to 12f, 14.5f to 15f,
    12f to 24f, 9.5f to 15f, 0f to 12f, 9.5f to 9f,
)

@Composable
fun Sparkle(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = PhotoboothColors.Gold,
) {
    Canvas(modifier = modifier.size(size)) {
        val scale = this.size.width / 24f
        val path = Path().apply {
            SparklePoints.forEachIndexed { index, (x, y) ->
                val point = Offset(x * scale, y * scale)
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
        drawPath(path, color = tint)
    }
}
