package com.dj.photobooth.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dj.photobooth.compose.rememberPathImageLoader
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothSpacing
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.TapeCorner

/**
 * The Strips (gallery) screen - design/design_handoff_photobooth_rebrand/README.md § "5. Strips
 * (gallery)", verified against `Photobooth Rebrand.dc.html`'s `screenIs.gallery` block (lines
 * ~341-381). Pure View in the MVVM sense, mirroring CaptureScreen/StripPreviewScreen - all state
 * from [viewModel]'s [GalleryUiState], every tap calls a ViewModel method.
 *
 * Tapping a card is navigation, not a ViewModel action - [onOpenEntry] is the seam, same shape
 * as CaptureScreen's onExitToLanding/onSessionComplete - so the nav layer (not this screen)
 * decides where "open" goes, which is what lets it open the in-app Strip Detail viewer instead
 * of the platform's own gallery app.
 *
 * [onStartStrip] backs the empty-state CTA ("start a strip" -> the Shoot tab, dc.html line 351).
 * It defaults to a no-op so this screen keeps compiling for callers that haven't wired it yet
 * (see this file's rebrand doc note) - PhotoboothNavHost.kt is expected to pass
 * `{ navController.navigate(Route.Shoot.route) }`, the same shape LandingScreen's
 * onStartSession already uses.
 *
 * DOMAIN MODEL GAP (see [HistoryEntry]): the design's gallery card shows per-entry frame-color
 * and strip/grid sub-layout, but [HistoryEntry] persists neither - only [HistoryEntry.filmTreatmentId]
 * survives compositing, and the strip/grid layout is already flattened into
 * [HistoryEntry.finalImagePath] by the compositor. Rather than add new Room columns/migrations
 * (out of scope per CLAUDE.md for a pre-release rebrand), each card's mount color is derived
 * deterministically from [HistoryEntry.id] ([frameColorFor]) - decorative variety, not a record
 * of the strip's actual saved frame color - and the mount shows the single already-composited
 * image rather than re-deriving individual strip/grid cells.
 */
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenEntry: (HistoryEntry) -> Unit,
    onStartStrip: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()

    // statusBarsPadding: the NavHost deliberately applies no blanket status-bar inset (it
    // would break Capture's full-bleed dark screen), so each light screen adds its own.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PhotoboothColors.Cream)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = PhotoboothSpacing.xl, start = PhotoboothSpacing.lg, end = PhotoboothSpacing.lg),
        ) {
            GalleryHeader()

            state.message?.let { message ->
                Text(
                    text = message,
                    style = PhotoboothType.bodyCaption(),
                    color = PhotoboothColors.HotPinkPressed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = PhotoboothSpacing.md),
                )
            }

            if (state.isEmpty) {
                EmptyState(onStartStrip = onStartStrip)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = PhotoboothSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.mdLarge),
                    verticalArrangement = Arrangement.spacedBy(PhotoboothSpacing.mdLarge),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        GalleryCard(
                            entry = entry,
                            onOpen = { onOpenEntry(entry) },
                            onShare = { viewModel.onShare(entry) },
                        )
                    }
                }
            }
        }
    }
}

// dc.html line 344: Caveat 700 17px, #8A3FFC, rotate(-2deg) - same tagline treatment as
// LandingScreen's "pocket photobooth" line. Line 345: Fredoka 700 26px headline.
@Composable
private fun GalleryHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = PhotoboothSpacing.lg)) {
        Text(
            text = "on this device",
            style = PhotoboothType.stamp17(),
            color = PhotoboothColors.Purple,
            modifier = Modifier.graphicsLayer { rotationZ = -2f },
        )
        Text(
            text = "your strips",
            style = PhotoboothType.display26(),
            color = PhotoboothColors.Ink,
            modifier = Modifier.padding(top = PhotoboothSpacing.xxs),
        )
    }
}

// dc.html line 348: "border:2px dashed rgba(43,24,48,.2);border-radius:18px;padding:34px 20px;
// text-align:center". Card copy + CTA per lines 349-351.
@Composable
private fun EmptyState(onStartStrip: () -> Unit, modifier: Modifier = Modifier) {
    // dp converted to px, not raw float px: at high density a raw 6f dash renders far shorter
    // than 6dp, which reads as a near-solid hairline rather than the specced dashed box.
    val density = LocalDensity.current
    val dashed = remember(density) {
        with(density) {
            val dashPx = 6.dp.toPx()
            Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx), 0f),
            )
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = PhotoboothColors.Ink.copy(alpha = 0.2f),
                    style = dashed,
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                )
            }
            .padding(vertical = PhotoboothSpacing.xxl, horizontal = PhotoboothSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("no strips yet", style = PhotoboothType.display15(), color = PhotoboothColors.Ink)
        Text(
            "go make one — it takes about a minute.",
            style = PhotoboothType.bodySmall(),
            color = PhotoboothColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = PhotoboothSpacing.xs, bottom = PhotoboothSpacing.lg),
        )
        Button(
            onClick = onStartStrip,
            shape = RoundedCornerShape(50),
            modifier = Modifier.height(44.dp),
            contentPadding = PaddingValues(horizontal = PhotoboothSpacing.lg),
            colors = ButtonDefaults.buttonColors(
                containerColor = PhotoboothColors.HotPink,
                contentColor = PhotoboothColors.Cream,
            ),
            // dc.html's empty-state CTA has no box-shadow (unlike Booth's full-width primary
            // CTA) - flat pill is correct here, so Material's own elevation is suppressed.
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        ) {
            Text("start a strip", style = PhotoboothType.display13().copy(fontSize = 13.5.sp))
        }
    }
}

// dc.html line 630's chaos formulas are all `N * chaos` with a live chaosLevel prop; this app
// has no chaos slider (same call LandingScreen's StripPreview already makes), so it's fixed at
// the prototype's default.
private const val ChaosLevel = 0.65f

// dc.html line 592: `cardOuterStyle: { transform: rotate(${(entry.id % 5 - 2) * chaos}deg) }` -
// deterministic per-entry variety (not random), so the same strip always renders at the same tilt.
private fun rotationDegFor(entry: HistoryEntry): Float = ((entry.id % 5).toInt() - 2) * ChaosLevel

// No persisted per-entry frame color (see this file's class doc) - picked deterministically from
// the entry's id so a given strip's mount color is stable across recompositions/relaunches
// without needing a new Room column.
private fun frameColorFor(entry: HistoryEntry): FrameColorPreset {
    val options = FrameColorPreset.entries
    return options[(entry.id % options.size).toInt()]
}

@Composable
private fun GalleryCard(entry: HistoryEntry, onOpen: () -> Unit, onShare: () -> Unit) {
    val loader = rememberPathImageLoader()
    // Two-column grid on a phone, so a card is roughly half the screen width. Passing that as
    // the decode hint keeps each thumbnail near its displayed size instead of decoding the
    // full 2x-scale export - see PathImageLoader.load's doc comment.
    val density = LocalDensity.current
    val targetWidthPx = remember(density) { with(density) { 200.dp.roundToPx() } }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, entry.finalImagePath, targetWidthPx) {
        value = loader.load(entry.finalImagePath, targetWidthPx)
    }
    val frame = remember(entry.id) { frameColorFor(entry) }
    val rotation = remember(entry.id) { rotationDegFor(entry) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = rotation },
    ) {
        // dc.html line 593: cardBoxStyle's `boxShadow: '0 3px 0 rgba(43,24,48,.12), 0 10px 20px
        // rgba(43,24,48,.15)'` - a hard 3dp ledge plus a soft ambient shadow, not a single
        // blurred elevation. matchParentSize sizes the ledge to the mount Box's resolved size
        // (from its one non-matchParentSize child, the TapeCorner-wrapped mount) rather than
        // needing a duplicated, hand-kept-in-sync height.
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(y = 3.dp)
                    .background(PhotoboothColors.Ink.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            )
            TapeCorner(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(12.dp), clip = false)
                    .clip(RoundedCornerShape(12.dp))
                    .background(frame.background)
                    .clickable(onClick = onOpen)
                    .padding(PhotoboothSpacing.smMedium),
            ) {
                // The already-composited strip/grid image (see this file's class doc for why
                // this isn't re-derived into individual cells here).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PhotoboothColors.SurfaceWash),
                ) {
                    bitmap?.let {
                        Image(
                            painter = BitmapPainter(it),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        // dc.html line 364: the stamp + share row sits BELOW the colored mount, on the plain
        // cream background, not inside it - only the mount itself is frame-colored.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = PhotoboothSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(entry.stamp, style = PhotoboothType.stamp13(), color = PhotoboothColors.TextBody)
            ShareIconButton(onClick = onShare)
        }
    }
}

// dc.html line 367: 15x15 share/nodes glyph, stroke `#8A3FFC` width 2.2 - three circles (r=3) +
// two connecting lines. Wrapped in a 32dp tappable area (a11y minimum) around the 15dp glyph,
// same "small glyph, bigger tap target" pattern the old SAVE link used (heightIn(min = 32.dp)).
@Composable
private fun ShareIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(15.dp)) {
            val scale = size.width / 24f
            val strokeWidthPx = 2.2f * scale
            val purple = PhotoboothColors.Purple
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            drawCircle(color = purple, radius = 3f * scale, center = Offset(18f * scale, 5f * scale), style = stroke)
            drawCircle(color = purple, radius = 3f * scale, center = Offset(6f * scale, 12f * scale), style = stroke)
            drawCircle(color = purple, radius = 3f * scale, center = Offset(18f * scale, 19f * scale), style = stroke)
            drawLine(
                color = purple,
                start = Offset(8.6f * scale, 10.5f * scale),
                end = Offset(15.4f * scale, 6.5f * scale),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = purple,
                start = Offset(8.6f * scale, 13.5f * scale),
                end = Offset(15.4f * scale, 17.5f * scale),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }
}
