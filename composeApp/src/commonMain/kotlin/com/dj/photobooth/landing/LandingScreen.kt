package com.dj.photobooth.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dj.photobooth.theme.Brand
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.Sparkle
import com.dj.photobooth.ui.TapeCorner

/**
 * Booth (landing) screen - design/handoff/README.md § "1. Booth (landing)", verified against
 * `Photobooth Rebrand.dc.html`'s `screenIs.booth` block (lines ~149-181), the real markup/inline
 * styles rather than the README's prose summary.
 *
 * Pure display + one callback ([onStartSession]) - this screen owns no session state itself.
 * The bottom tab bar (booth/shoot/strips) is NOT built here - `PhotoboothNavHost`'s `Scaffold`
 * already wraps this route in the shared `BottomTabBar`, so this file is only the screen's own
 * scrollable content above it.
 */
@Composable
fun LandingScreen(onStartSession: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PhotoboothColors.Cream)
            // "background-image:radial-gradient(circle, rgba(43,24,48,.07) 1px, transparent
            // 1.5px);background-size:16px 16px" - Compose has no repeating radial-gradient
            // primitive, so this is a drawn dot grid instead (same technique the Industry-era
            // screen used for its faint line grid, drawCircle in place of drawLine).
            .drawBehind { drawDottedGrid() }
            // Edge-to-edge is on and the NavHost doesn't apply a blanket status-bar inset (that
            // would break Capture's full-bleed dark screen), so each light screen adds its own -
            // the cream/dots still paint behind the status bar, only content insets.
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // dc.html: "flex:1;padding:26px 22px 8px;...gap:14px"
                .padding(top = 26.dp, start = 22.dp, end = 22.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Wordmark()
            Text(
                text = "pocket photobooth",
                style = PhotoboothType.stamp17(),
                color = PhotoboothColors.Purple,
                modifier = Modifier.graphicsLayer { rotationZ = -2f },
            )
            Text(
                text = "four shots,\none strip,\nzero booth.",
                style = PhotoboothType.display36(),
                color = PhotoboothColors.Ink,
            )
            Text(
                text = "smile four times, pick your favorites, turn today into a strip " +
                    "worth keeping.",
                style = PhotoboothType.bodyRegular(),
                color = PhotoboothColors.TextBody,
                modifier = Modifier.widthIn(max = 270.dp),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    // dc.html: "margin:10px 0 6px" on the strip-preview wrapper.
                    .padding(top = 10.dp, bottom = 6.dp),
            ) {
                StripPreview()
            }
        }
        StartBlock(onStartSession = onStartSession)
    }
}

private fun DrawScope.drawDottedGrid() {
    val pitch = 16.dp.toPx()
    val radius = 1.2.dp.toPx()
    val color = PhotoboothColors.GridLineOnLight
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            x += pitch
        }
        y += pitch
    }
}

// dc.html: `<span style="...font-family:'Fredoka';font-weight:700;font-size:15px;color:#FF4FA0;">
// {{ appName }}</span>` plus a 13x13 sparkle glyph - the wordmark is the plain app name, NOT
// uppercased (unlike the old Industry-era "FOURFRAME" placeholder this replaces).
@Composable
private fun Wordmark() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = Brand.NAME,
            style = PhotoboothType.display15(),
            color = PhotoboothColors.HotPink,
        )
        Sparkle(size = 13.dp, tint = PhotoboothColors.Gold)
    }
}

// This screen has no live chaos slider (that's a prototype-only tweak, per architecture.md /
// the design handoff's Interactions section) - fixed at the dc.html default so the strip
// preview still reads as "chaotic-good" scrapbook tilt rather than perfectly square.
private const val Chaos = 0.65f

// Cell fills stand in for real captured photos on this screen (there are none yet - this is
// the landing screen's decorative strip graphic, not an actual session). Approximates the
// dc.html's four 135deg CSS gradients; Brush.linearGradient's default diagonal reads close
// enough for a small decorative swatch (same call already made for PhotoboothColors.DarkGradient).
private val StripCellGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFFFF9AD1), PhotoboothColors.HotPink)),
    Brush.linearGradient(listOf(Color(0xFFC7A6FF), PhotoboothColors.Purple)),
    Brush.linearGradient(listOf(Color(0xFFFFE18A), PhotoboothColors.Gold)),
    Brush.linearGradient(listOf(Color(0xFF8FF3D9), PhotoboothColors.Mint)),
)

// dc.html lines ~163-171 + the chaos formulas at line ~630: `boothStripRot: -4 * chaos,
// boothTapeRot1: -30 * chaos, boothTapeRot2: 24 * chaos, boothStarRot: 12 * chaos`. The card
// itself carries the -4*chaos rotation; the two tape strips and the sparkle accent each carry
// their own independent rotation, not compounded with the card's.
@Composable
private fun StripPreview() {
    val stripRotation = -4f * Chaos
    val tapeRotationTopLeft = -30f * Chaos
    val tapeRotationBottomRight = 24f * Chaos
    val starRotation = 12f * Chaos

    Box {
        TapeCorner(
            // Booth's tape is bigger/brighter than Customize/Share's default (28x13, ink-
            // tinted) - dc.html's booth tape is 32x15 at .9 opacity, gold top-left / mint
            // bottom-right.
            tapeWidth = 32.dp,
            tapeHeight = 15.dp,
            topLeftTapeColor = PhotoboothColors.Gold.copy(alpha = 0.9f),
            bottomRightTapeColor = PhotoboothColors.Mint.copy(alpha = 0.9f),
            topLeftRotationDeg = tapeRotationTopLeft,
            bottomRightRotationDeg = tapeRotationBottomRight,
        ) {
            Column(
                modifier = Modifier
                    .width(124.dp)
                    .graphicsLayer { rotationZ = stripRotation }
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(14.dp), clip = false)
                    .background(PhotoboothColors.Paper, RoundedCornerShape(14.dp))
                    .padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StripCellGradients.forEach { gradient ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(gradient),
                    )
                }
            }
        }
        Sparkle(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 28.dp, y = 14.dp)
                .graphicsLayer { rotationZ = starRotation },
            size = 26.dp,
            tint = PhotoboothColors.Gold,
        )
    }
}

// dc.html: "border-top:2px dashed rgba(43,24,48,.15);padding:12px 22px 20px" wrapping the
// primary CTA + helper caption.
@Composable
private fun StartBlock(onStartSession: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawDashedTopBorder() }
            .padding(top = 12.dp, start = 22.dp, end = 22.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        PrimaryCta(onStartSession = onStartSession)
        Text(
            text = "no accounts · nothing leaves your phone",
            style = PhotoboothType.bodyCaption(),
            color = PhotoboothColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun DrawScope.drawDashedTopBorder() {
    drawLine(
        color = PhotoboothColors.HairlineDashed,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
    )
}

// dc.html: "height:54px;border-radius:999px;background:#FF4FA0;...font-size:16.5px;
// box-shadow:0 5px 0 #C22A79" - a hard, non-blurred pressed-button ledge, not a blurred
// elevation shadow. Built as two stacked pills: a same-shaped HotPinkPressed pill offset 5dp
// down underneath, and the real button unshifted on top, rather than Modifier.shadow().
@Composable
private fun PrimaryCta(onStartSession: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .offset(y = 5.dp)
                .background(PhotoboothColors.HotPinkPressed, RoundedCornerShape(50)),
        )
        Button(
            onClick = onStartSession,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = PhotoboothColors.HotPink,
                contentColor = PhotoboothColors.Paper,
            ),
            // Material's own elevation shadow would double up with the hard ledge drawn
            // above/underneath - suppress it so only our flat drop-shadow reads.
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
            ),
        ) {
            Text(
                text = "start a strip",
                style = PhotoboothType.display17().copy(fontSize = 16.5.sp),
            )
        }
    }
}
