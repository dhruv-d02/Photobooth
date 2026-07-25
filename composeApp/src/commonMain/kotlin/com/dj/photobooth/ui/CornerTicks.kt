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

// Each corner's Alignment and its outward offset direction are two facts about the same
// corner, not two independent choices - pairing them here, once, means a call site can't
// mismatch them (e.g. TopEnd with a leftward-pointing offset) the way passing Alignment and
// signed Dp values separately at each call site could.
private enum class Corner(val alignment: Alignment, val signX: Int, val signY: Int) {
    TopStart(Alignment.TopStart, signX = -1, signY = -1),
    TopEnd(Alignment.TopEnd, signX = 1, signY = -1),
    BottomStart(Alignment.BottomStart, signX = -1, signY = 1),
    BottomEnd(Alignment.BottomEnd, signX = 1, signY = 1),
}

/**
 * Wraps [content] in a Box and overlays four "+" registration marks just outside its
 * corners, per the design's blueprint aesthetic. The marks are plain monospace glyphs
 * (not vector-drawn crosses) at 12sp, offset ~7px horizontally / ~5px vertically outward
 * from each corner so they read as printer's registration marks around a framed object.
 *
 * The [modifier] passed in should describe only the framed object itself (its border/
 * background) - any inner content padding belongs on the content, not on this modifier,
 * since the ticks align to this Box's own bounds and inner padding would pull them back
 * inside the visible frame instead of landing outside it.
 */
@Composable
fun CornerTicks(
    modifier: Modifier = Modifier,
    tickColor: Color = PhotoboothColors.Accent,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()
        Corner.entries.forEach { corner -> CornerTick(corner, tickColor) }
    }
}

@Composable
private fun BoxScope.CornerTick(corner: Corner, color: Color) {
    Text(
        text = "+",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = Modifier
            .align(corner.alignment)
            .offset(x = TickOffsetX * corner.signX, y = TickOffsetY * corner.signY),
    )
}
