package com.dj.photobooth.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dj.photobooth.theme.PhotoboothColors

// The "+" registration marks are the single most repeated decorative motif in the design -
// they appear on the landing strip figure, the capture viewfinder, the strip preview
// figure, and every gallery card (design/handoff/README.md § Design language and per-screen
// specs). Building it once here as a wrapper, instead of copy-pasting four Text("+") calls
// at every use site, is what keeps that repetition consistent if the offset/size ever needs
// a global tweak.
private val TickOffsetX = 7.dp
private val TickOffsetY = 5.dp

/**
 * Wraps [content] in a Box and overlays four "+" registration marks just outside its
 * corners, per the design's blueprint aesthetic. The marks are plain monospace glyphs
 * (not vector-drawn crosses) at 12sp, offset ~7px horizontally / ~5px vertically outward
 * from each corner so they read as printer's registration marks around a framed object.
 */
@Composable
fun CornerTicks(
    modifier: Modifier = Modifier,
    tickColor: Color = PhotoboothColors.Accent,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()
        CornerTick(Alignment.TopStart, -TickOffsetX, -TickOffsetY, tickColor)
        CornerTick(Alignment.TopEnd, TickOffsetX, -TickOffsetY, tickColor)
        CornerTick(Alignment.BottomStart, -TickOffsetX, TickOffsetY, tickColor)
        CornerTick(Alignment.BottomEnd, TickOffsetX, TickOffsetY, tickColor)
    }
}

@Composable
private fun BoxScope.CornerTick(
    alignment: Alignment,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    color: Color,
) {
    Text(
        text = "+",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY),
    )
}