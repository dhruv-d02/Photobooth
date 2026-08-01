package com.dj.photobooth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dj.photobooth.theme.PhotoboothColors

/**
 * Two low-opacity rotated "tape" rectangles at opposing corners - the design's one motif
 * reserved for photo/strip mount objects only (design/handoff/README.md's Motifs section;
 * exact size/rotation/opacity confirmed against `Photobooth Rebrand.dc.html`'s Customize/Share
 * mount tape corners: 28x13dp, top-left at -25deg, bottom-right at 20deg, ~25% opacity ink).
 * Booth's strip-preview graphic uses bigger, brighter, chaos-driven tape and should NOT reach
 * for this default - pass its own [tapeColor]/[tapeWidth]/[tapeHeight]/rotations instead.
 *
 * Like the old CornerTicks, this wraps [content] in a Box and overlays the tape outside its
 * corners - the [modifier] passed in should describe only the framed object itself.
 */
@Composable
fun TapeCorner(
    modifier: Modifier = Modifier,
    tapeColor: Color = PhotoboothColors.Ink.copy(alpha = 0.25f),
    tapeWidth: Dp = 28.dp,
    tapeHeight: Dp = 13.dp,
    topLeftRotationDeg: Float = -25f,
    bottomRightRotationDeg: Float = 20f,
    // Per-corner overrides, both defaulting to [tapeColor] - every existing call site keeps
    // its current single-color look unchanged. Added for Booth's strip-preview graphic, whose
    // tape is bright and two-toned (gold top-left, mint bottom-right - dc.html's booth block)
    // rather than the shared ink-tinted default.
    topLeftTapeColor: Color = tapeColor,
    bottomRightTapeColor: Color = tapeColor,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        content()
        TapeStrip(
            alignment = Alignment.TopStart,
            offsetX = (-tapeWidth) * 0.4f,
            offsetY = (-tapeHeight) * 0.6f,
            width = tapeWidth,
            height = tapeHeight,
            rotationDeg = topLeftRotationDeg,
            color = topLeftTapeColor,
        )
        TapeStrip(
            alignment = Alignment.BottomEnd,
            offsetX = tapeWidth * 0.4f,
            offsetY = tapeHeight * 0.6f,
            width = tapeWidth,
            height = tapeHeight,
            rotationDeg = bottomRightRotationDeg,
            color = bottomRightTapeColor,
        )
    }
}

@Composable
private fun BoxScope.TapeStrip(
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp,
    width: Dp,
    height: Dp,
    rotationDeg: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(width = width, height = height)
            .graphicsLayer { rotationZ = rotationDeg }
            .background(color, RoundedCornerShape(2.dp)),
    )
}
