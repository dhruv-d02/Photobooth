package com.dj.photobooth.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothSpacing
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.CornerTicks

/**
 * Landing screen, design/handoff/README.md § 1 - variant **1a "Spec sheet"** only. Per
 * architecture.md's nav section, 1a is the only variant shipped; 1b (Steel field) and 1c
 * (Procedure list) are reference-only design alternatives, deliberately not built here.
 *
 * Pure display + one callback ([onStartSession]) - this screen owns no session state itself,
 * matching CaptureScreen's plain-View convention (all state/logic lives one layer up, here in
 * the NavHost/ViewModel, not in the composable).
 */
@Composable
fun LandingScreen(onStartSession: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PhotoboothColors.Ground)
            // "a faint 27.2px grid (#1d1f2008 1px lines both axes)" behind the whole screen.
            .drawBehind { drawFaintGrid() },
    ) {
        TopBlock()
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            StripFigure()
        }
        BottomBlock(onStartSession = onStartSession)
    }
}

private fun DrawScope.drawFaintGrid() {
    val pitch = PhotoboothSpacing.gridPitch.toPx()
    val color = PhotoboothColors.GridLineOnLight
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += pitch
    }
    var y = 0f
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += pitch
    }
}

// design/handoff/README.md line 60: "Top block, padding 27.2px 20.4px 17px, 13.6px gap, 1px
// bottom border" - CSS 3-value shorthand (top | left-right | bottom).
@Composable
private fun TopBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 1px bottom border only - drawn rather than Modifier.border() so it doesn't
            // also draw the top/side edges the spec doesn't call for.
            .drawBehind {
                drawLine(
                    color = PhotoboothColors.HairlineOnLight,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f,
                )
            }
            .padding(
                PaddingValues(
                    top = PhotoboothSpacing.xlLarge,
                    start = PhotoboothSpacing.lgLarge,
                    end = PhotoboothSpacing.lgLarge,
                    bottom = PhotoboothSpacing.lg,
                ),
            ),
        verticalArrangement = Arrangement.spacedBy(PhotoboothSpacing.mdLarge),
    ) {
        // Kicker: monospace 10px uppercase, letter-spacing .16em, #416180 - meta10 is the
        // right family/size/weight but its default letter-spacing (.14em) is a shared value
        // across every 10px metadata label; this kicker is specced at .16em specifically
        // (line 61), so override via .copy() rather than changing meta10 itself.
        Text(
            text = "SPEC 01 — 4 EXPOSURES, ONE STRIP",
            style = PhotoboothType.meta10.copy(letterSpacing = 0.16f.em),
            color = PhotoboothColors.AccentPressed,
        )
        Text(
            text = "FOUR FRAMES.\nONE STRIP.\nNO BOOTH.",
            style = PhotoboothType.display52,
            color = PhotoboothColors.TextPrimary,
        )
        Text(
            text = "Your camera fires four times on a countdown, then lays the exposures " +
                "into a photobooth strip you can tint, filter and save to your phone.",
            style = PhotoboothType.body15,
            color = PhotoboothColors.TextBody,
        )
    }
}

// design/handoff/README.md lines 67-71: centred drawn strip figure, 150px wide, four empty
// 4:3 cells filled with a 45deg hatch, footer row (brand + "2x6 IN"), four "+" marks.
@Composable
private fun StripFigure() {
    CornerTicks {
        Column(
            modifier = Modifier
                .width(150.dp)
                .background(PhotoboothColors.Paper)
                .border(1.dp, PhotoboothColors.Accent)
                .padding(PhotoboothSpacing.md),
            verticalArrangement = Arrangement.spacedBy(PhotoboothSpacing.sm),
        ) {
            repeat(4) {
                HatchedCell(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = PhotoboothColors.HairlineOnLight,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1f,
                        )
                    }
                    .padding(top = PhotoboothSpacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Brand string is still undecided (architecture.md § Core domain model:
                    // "brand ... default placeholder — app name still undecided"); the design
                    // handoff's own default config uses "Fourframe" as its placeholder brand.
                    Text(
                        text = "FOURFRAME",
                        style = PhotoboothType.heading10.copy(letterSpacing = 0.14f.em),
                        color = PhotoboothColors.TextPrimary,
                    )
                    Text(
                        text = "2×6 IN",
                        style = PhotoboothType.meta8,
                        color = PhotoboothColors.TextMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun HatchedCell(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            // #dfe7f0 / #eef6ff are exactly PhotoboothColors.AccentTintFaint / AccentTintSoft.
            .background(PhotoboothColors.AccentTintSoft)
            .drawBehind { drawDiagonalHatch() }
            .border(1.dp, PhotoboothColors.HairlineOnLight),
    )
}

// "45° 4px/4px hatch (#dfe7f0/#eef6ff)": diagonal stripes, 4px stroke, 4px gap (8px period).
private fun DrawScope.drawDiagonalHatch() {
    val strokeWidthPx = 4.dp.toPx()
    val periodPx = 8.dp.toPx()
    val span = size.width + size.height
    var offset = -size.height
    while (offset < span) {
        drawLine(
            color = PhotoboothColors.AccentTintFaint,
            start = Offset(offset, size.height),
            end = Offset(offset + size.height, 0f),
            strokeWidth = strokeWidthPx,
        )
        offset += periodPx
    }
}

// design/handoff/README.md lines 72-75: "Bottom block, 1px top border, padding 17px 20.4px
// 20.4px: full-width primary button START SESSION ... then a two-line monospace 10px caption".
@Composable
private fun BottomBlock(onStartSession: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = PhotoboothColors.HairlineOnLight,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
            }
            .padding(
                PaddingValues(
                    top = PhotoboothSpacing.lg,
                    start = PhotoboothSpacing.lgLarge,
                    end = PhotoboothSpacing.lgLarge,
                    bottom = PhotoboothSpacing.lgLarge,
                ),
            ),
        verticalArrangement = Arrangement.spacedBy(PhotoboothSpacing.md),
    ) {
        CornerTicks {
            // design/handoff/README.md line 73: "min-height 56px" - matches CaptureScreen's
            // own 56.dp shutter/keep buttons.
            Button(
                onClick = onStartSession,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = RoundedCornerShape(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhotoboothColors.Accent,
                    contentColor = PhotoboothColors.Paper,
                ),
            ) {
                Text("START SESSION", style = PhotoboothType.heading18)
            }
        }
        Text(
            text = "CAMERA PERMISSION REQUIRED · NOTHING LEAVES THIS DEVICE",
            style = PhotoboothType.meta10,
            color = PhotoboothColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

