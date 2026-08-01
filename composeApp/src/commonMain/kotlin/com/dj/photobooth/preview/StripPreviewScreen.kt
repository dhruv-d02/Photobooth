package com.dj.photobooth.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dj.photobooth.filter.CompositeGeometry
import com.dj.photobooth.filter.FilmTreatment
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.filter.StripLayout
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.TapeCorner

/**
 * The Customize screen (design/handoff/README.md § "3. Customize"), verified against
 * `Photobooth Rebrand.dc.html`'s `screenIs.customize` block (lines ~231-298) rather than just
 * the README prose. Pure View in the MVVM sense, mirroring CaptureScreen.kt - all state comes
 * from [viewModel]'s [StripPreviewUiState], every tap calls a ViewModel method or one of the
 * navigation callbacks.
 *
 * [onContinue] replaces the old "SAVE PNG" action: Customize no longer saves anything itself
 * (see StripPreviewViewModel's class doc) - it hands the already-composed image off to the new
 * Share screen, and [onContinue] is the nav-layer seam that does the hand-off + navigate
 * (PhotoboothNavHost's Route.Preview composable).
 *
 * [onRetakeRequested] is a navigation seam too, same as before - actually re-entering the
 * capture loop for one slot needs CaptureViewModel + navigation, which live one layer up.
 */
@Composable
fun StripPreviewScreen(
    viewModel: StripPreviewViewModel,
    onReshoot: () -> Unit,
    onRetakeRequested: (Int) -> Unit,
    onContinue: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    // statusBarsPadding/navigationBarsPadding: the NavHost applies no blanket inset (that would
    // break Capture/Share's full-bleed dark screens), so each light screen adds its own.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PhotoboothColors.Cream)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // dc.html: "flex:1;padding:22px 20px 6px;...gap:18px" - no dotted-grid background here
        // (unlike Booth/screenIs.booth's div), confirmed against the raw customize markup.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 22.dp, start = 20.dp, end = 20.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CustomizeHeader()
            MountSection(state = state, onRetake = onRetakeRequested)
            FlicketRow(state = state, onSelect = viewModel::onTreatmentChange)
            FrameRow(state = state, onSelect = viewModel::onFrameColorChange)
            LayoutRow(state = state, onChange = viewModel::onLayoutChange)
        }
        ActionBar(state = state, onReshoot = onReshoot, onContinue = onContinue)
    }
}

// dc.html lines 234-237: Caveat 700 17px purple tagline (rotate -2deg) + Fredoka 700 24px
// headline, no kicker/proof-code header (that was the old Industry-era PreviewHeader - gone).
@Composable
private fun CustomizeHeader() {
    Column {
        Text(
            text = "your strip",
            style = PhotoboothType.stamp17(),
            color = PhotoboothColors.Purple,
            modifier = Modifier.graphicsLayer { rotationZ = -2f },
        )
        Text(
            text = "make it yours",
            style = PhotoboothType.display24(),
            color = PhotoboothColors.Ink,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

// dc.html lines 239-255 / 559-564: the strip/grid mount - width 140dp (strip) or 220dp (grid),
// frame-color background, 16dp radius, 12dp padding, a hard 5px ledge + soft blur shadow, and
// (per this screen's spec, unlike Share's) NO rotation on the mount itself - only its two tape
// corners rotate, and even those are fixed at -25/20deg (TapeCorner's own defaults), not
// chaos-scaled.
@Composable
private fun MountSection(state: StripPreviewUiState, onRetake: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val mountWidth = if (state.layout == StripLayout.Strip) 140.dp else 220.dp

        TapeCorner(modifier = Modifier.width(mountWidth)) {
            Box(modifier = Modifier.width(mountWidth)) {
                // Hard drop-shadow ledge - "0 5px 0 rgba(43,24,48,.15)" approximated as a
                // same-shaped Ink-15%-alpha copy of the mount, offset 5dp down and drawn first
                // (LandingScreen.PrimaryCta uses this same two-layer technique for its pill
                // button's hard shadow). matchParentSize lets this track the mount's real
                // height, which varies with the composed image's aspect ratio.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(y = 5.dp)
                        .background(PhotoboothColors.Ink.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                )
                Box(
                    modifier = Modifier
                        // The second, soft half of "0 16px 30px rgba(43,24,48,.18)" - an
                        // approximation via Modifier.shadow rather than a second offset-rect
                        // copy, since a blurred shadow can't be faked with a flat rectangle.
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = false,
                            ambientColor = PhotoboothColors.Ink.copy(alpha = 0.18f),
                            spotColor = PhotoboothColors.Ink.copy(alpha = 0.18f),
                        )
                        .background(state.frameColor.background, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                ) {
                    val composed = state.composedImage
                    if (composed == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (state.layout == StripLayout.Strip) 0.7f else 1f),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = state.frameColor.text) }
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val geometry = CompositeGeometry.forLayout(state.layout, state.decodedFrames.size)
                            val aspect = geometry.outputWidth.toFloat() / geometry.outputHeight.toFloat()
                            val displayHeight = maxWidth / aspect

                            Image(
                                painter = BitmapPainter(composed),
                                contentDescription = null,
                                modifier = Modifier.width(maxWidth).height(displayHeight),
                            )

                            repeat(state.decodedFrames.size) { index ->
                                val (cellX, cellY) = geometry.cellOrigin(index)
                                val xFrac = cellX.toFloat() / geometry.outputWidth
                                val yFrac = cellY.toFloat() / geometry.outputHeight
                                val cellWidthFrac = geometry.photoWidth.toFloat() / geometry.outputWidth
                                val xOffset = maxWidth * xFrac
                                val yOffset = displayHeight * yFrac
                                val cellDisplayWidth = maxWidth * cellWidthFrac

                                RetakeButton(
                                    modifier = Modifier.offset(
                                        x = xOffset + cellDisplayWidth - RetakeButtonSize - 5.dp,
                                        y = yOffset + 5.dp,
                                    ),
                                    onClick = { onRetake(index) },
                                )
                            }

                            // Footer row, overlaid on the mount's bottom edge rather than
                            // rendered as a separate section below it (see MountFooter's doc
                            // comment for why this is an overlay, not a flow child).
                            MountFooter(
                                brand = state.brand,
                                stamp = state.stamp,
                                frameColor = state.frameColor,
                                modifier = Modifier.align(Alignment.BottomCenter).width(maxWidth),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val RetakeButtonSize = 24.dp

// dc.html lines 245-247: a small circular retake button, top-right of each photo cell -
// restyled from the old rectangular RETAKE-0N chip into a 24dp translucent-white circle with a
// simple "undo" glyph, per the design spec.
@Composable
private fun RetakeButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(RetakeButtonSize)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("↺", color = PhotoboothColors.Ink, fontSize = 13.sp)
    }
}

// dc.html lines 250-253: "grid-column:1/-1;...padding-top:8px;border-top:1px dashed
// {{currentFrameRule}}" - brand name (left) + date stamp (right), both colored to the mount's
// own frame-contrast text color. This is drawn as an overlay pinned to the bottom of the
// composed image (not a flow child below it): the composed PNG (StripCompositor.compose)
// already bakes in the frame-color background and a footer-height reservation with its own
// rule line for the *whole* mount, so the live text just needs to land inside that already-
// reserved band, not add a second one. ShareScreen.kt keeps its own private copy of this same
// shape - not extracted into a shared component since each screen already owns its own file
// per this task's file-ownership split, and it's ~15 lines.
@Composable
private fun MountFooter(
    brand: String,
    stamp: String,
    frameColor: FrameColorPreset,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .drawBehind {
                drawLine(
                    color = frameColor.dim,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f),
                )
            }
            .padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(brand, style = PhotoboothType.display11(), color = frameColor.text)
        Text(stamp, style = PhotoboothType.stamp13(), color = frameColor.text)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = PhotoboothType.sectionLabel(),
        color = PhotoboothColors.TextMuted,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

// dc.html lines 257-266: "flicket" row - horizontal scroll of pill chips, one per
// FilmTreatment, active = filled hot pink + white text.
@Composable
private fun FlicketRow(state: StripPreviewUiState, onSelect: (FilmTreatment) -> Unit) {
    Column {
        SectionLabel("flicket")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilmTreatment.entries.forEach { treatment ->
                val selected = state.treatment == treatment
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) PhotoboothColors.HotPink else Color.White)
                        .border(
                            width = 2.dp,
                            color = if (selected) PhotoboothColors.HotPink else PhotoboothColors.Ink.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50),
                        )
                        .clickable { onSelect(treatment) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    // FilmTreatment.displayName is already lowercase (e.g. "disposable") -
                    // don't re-case it, per this task's spec.
                    Text(
                        text = treatment.displayName,
                        style = PhotoboothType.bodyBold12(),
                        color = if (selected) Color.White else PhotoboothColors.Ink,
                    )
                }
            }
        }
    }
}

// dc.html lines 268-278: "frame" row - horizontal scroll of circular swatches, one per
// FrameColorPreset, active swatch gets a dark ring.
@Composable
private fun FrameRow(state: StripPreviewUiState, onSelect: (FrameColorPreset) -> Unit) {
    Column {
        SectionLabel("frame")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FrameColorPreset.entries.forEach { preset ->
                val selected = state.frameColor == preset
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.clickable { onSelect(preset) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(preset.background)
                            .border(
                                width = 3.dp,
                                color = if (selected) PhotoboothColors.Ink else Color.Transparent,
                                shape = CircleShape,
                            ),
                    )
                    Text(
                        text = preset.name.lowercase(),
                        style = PhotoboothType.bodyBold11().copy(fontSize = 10.5.sp),
                        color = PhotoboothColors.TextBody,
                    )
                }
            }
        }
    }
}

// dc.html lines 280-286: "layout" row - a segmented pill control, strip vs grid.
@Composable
private fun LayoutRow(state: StripPreviewUiState, onChange: (StripLayout) -> Unit) {
    Column {
        SectionLabel("layout")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, PhotoboothColors.Ink.copy(alpha = 0.12f), RoundedCornerShape(50))
                .padding(3.dp),
        ) {
            SegmentCell("strip", state.layout == StripLayout.Strip, Modifier.weight(1f)) {
                onChange(StripLayout.Strip)
            }
            SegmentCell("grid", state.layout == StripLayout.Grid, Modifier.weight(1f)) {
                onChange(StripLayout.Grid)
            }
        }
    }
}

@Composable
private fun SegmentCell(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) PhotoboothColors.Ink else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PhotoboothType.display13(), color = if (selected) Color.White else PhotoboothColors.Ink)
    }
}

// dc.html lines 288-291: "border-top:2px dashed rgba(43,24,48,.15)" wrapping "reshoot"
// (outline, flex 1) + "continue" (primary, flex 1.4, hard drop-shadow).
@Composable
private fun ActionBar(state: StripPreviewUiState, onReshoot: () -> Unit, onContinue: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PhotoboothColors.Cream)
            .drawBehind { drawDashedTopBorder() }
            .padding(top = 14.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedButton(
            onClick = onReshoot,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(50),
            border = BorderStroke(2.dp, PhotoboothColors.Ink.copy(alpha = 0.18f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PhotoboothColors.Ink),
        ) {
            Text("reshoot", style = PhotoboothType.display13().copy(fontSize = 14.sp))
        }

        Box(modifier = Modifier.weight(1.4f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .offset(y = 5.dp)
                    .background(PhotoboothColors.HotPinkPressed, RoundedCornerShape(50)),
            )
            Button(
                onClick = onContinue,
                enabled = state.composedImage != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhotoboothColors.HotPink,
                    contentColor = Color.White,
                    disabledContainerColor = PhotoboothColors.HotPink.copy(alpha = 0.5f),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            ) {
                Text("continue", style = PhotoboothType.display13().copy(fontSize = 14.5.sp))
            }
        }
    }
}

private fun DrawScope.drawDashedTopBorder() {
    drawLine(
        color = PhotoboothColors.Ink.copy(alpha = 0.15f),
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
    )
}
