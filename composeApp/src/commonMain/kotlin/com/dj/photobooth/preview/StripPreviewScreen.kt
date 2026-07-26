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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dj.photobooth.filter.CompositeGeometry
import com.dj.photobooth.filter.FilmTreatment
import com.dj.photobooth.filter.FrameColorPreset
import com.dj.photobooth.filter.StripLayout
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothSpacing
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.CornerTicks

/**
 * The Strip Preview & Customise screen (design/handoff/README.md §3): fixed header, scrolling
 * body, fixed action bar. Pure View in the MVVM sense, mirroring CaptureScreen.kt - all state
 * comes from [viewModel]'s [StripPreviewUiState], every tap calls a ViewModel method.
 *
 * [onRetakeRequested] and [onReshoot] are navigation seams, not implemented here - see
 * StripPreviewViewModel's class doc for why (this branch owns the screen/ViewModel, a
 * separate branch owns the nav graph that will eventually drive CaptureViewModel from these
 * callbacks).
 */
@Composable
fun StripPreviewScreen(
    viewModel: StripPreviewViewModel,
    onReshoot: () -> Unit,
    onRetakeRequested: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PhotoboothColors.Ground)) {
        PreviewHeader(state)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            StripFigureBand(state = state, onRetake = onRetakeRequested)
            LayoutControl(state = state, onLayoutChange = viewModel::onLayoutChange)
            FilmTreatmentRow(state = state, onTreatmentChange = viewModel::onTreatmentChange)
            FrameColorRow(state = state, onFrameColorChange = viewModel::onFrameColorChange)
        }

        ActionBar(state = state, onReshoot = onReshoot, onSavePng = viewModel::onSavePng)
    }
}

@Composable
private fun PreviewHeader(state: StripPreviewUiState) {
    Column(modifier = Modifier.fillMaxWidth().background(PhotoboothColors.Paper)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PhotoboothSpacing.lg, vertical = PhotoboothSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("PROOF · STRIP 001", style = PhotoboothType.meta10, color = PhotoboothColors.AccentPressed)
            Text(state.layoutFilterCode, style = PhotoboothType.meta10, color = PhotoboothColors.TextMuted)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PhotoboothColors.HairlineOnLight))
    }
}

@Composable
private fun StripFigureBand(state: StripPreviewUiState, onRetake: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PhotoboothColors.SurfaceWashTranslucent)
            .padding(PhotoboothSpacing.lgLarge),
        contentAlignment = Alignment.Center,
    ) {
        val figureWidth = if (state.layout == StripLayout.Strip) 176.dp else 260.dp
        CornerTicks(modifier = Modifier.width(figureWidth)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PhotoboothColors.Accent)
                    .background(state.frameColor.background)
                    .padding(PhotoboothSpacing.smMedium),
            ) {
                val composed = state.composedImage
                if (composed == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(if (state.layout == StripLayout.Strip) 0.7f else 1f),
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

                            RetakeChip(
                                label = "RETAKE ${(index + 1).toString().padStart(2, '0')}",
                                modifier = Modifier
                                    .offset(x = xOffset + cellDisplayWidth, y = yOffset)
                                    .offset(x = -RetakeChipWidth, y = PhotoboothSpacing.xxs),
                                onClick = { onRetake(index) },
                            )
                        }
                    }
                }
            }
        }
    }
    FooterRow(state)
}

private val RetakeChipWidth = 78.dp

@Composable
private fun RetakeChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .width(RetakeChipWidth)
            .background(PhotoboothColors.Paper.copy(alpha = 0xDD / 255f))
            .clickable(onClick = onClick)
            .padding(horizontal = PhotoboothSpacing.xs, vertical = PhotoboothSpacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = PhotoboothType.meta9, color = PhotoboothColors.TextPrimary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FooterRow(state: StripPreviewUiState) {
    Row(
        modifier = Modifier
            .padding(horizontal = PhotoboothSpacing.lgLarge)
            .padding(top = PhotoboothSpacing.sm)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(state.brand.uppercase(), style = PhotoboothType.heading12, color = PhotoboothColors.TextPrimary)
        Text(state.stamp, style = PhotoboothType.meta8, color = PhotoboothColors.TextMuted)
    }
}

@Composable
private fun LayoutControl(state: StripPreviewUiState, onLayoutChange: (StripLayout) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PhotoboothSpacing.lg, vertical = PhotoboothSpacing.md)) {
        Text("LAYOUT", style = PhotoboothType.meta10, color = PhotoboothColors.AccentPressed)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(PhotoboothSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth().border(1.dp, PhotoboothColors.HairlineOnLight),
        ) {
            SegmentCell("VERTICAL STRIP", state.layout == StripLayout.Strip, Modifier.weight(1f)) {
                onLayoutChange(StripLayout.Strip)
            }
            SegmentCell("2 × 2 GRID", state.layout == StripLayout.Grid, Modifier.weight(1f)) {
                onLayoutChange(StripLayout.Grid)
            }
        }
    }
}

@Composable
private fun SegmentCell(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(if (selected) PhotoboothColors.Accent else PhotoboothColors.Paper)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = PhotoboothType.heading13,
            color = if (selected) PhotoboothColors.Paper else PhotoboothColors.TextPrimary,
        )
    }
}

@Composable
private fun FilmTreatmentRow(state: StripPreviewUiState, onTreatmentChange: (FilmTreatment) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PhotoboothSpacing.lg, vertical = PhotoboothSpacing.md)) {
        Text("FILM TREATMENT", style = PhotoboothType.meta10, color = PhotoboothColors.AccentPressed)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(PhotoboothSpacing.xs))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.sm),
        ) {
            FilmTreatment.entries.forEach { treatment ->
                val selected = state.treatment == treatment
                Chip(selected = selected, onClick = { onTreatmentChange(treatment) }) {
                    Text(treatment.displayName.uppercase(), style = PhotoboothType.heading12, color = if (selected) PhotoboothColors.Paper else PhotoboothColors.TextPrimary)
                    Text(treatment.code, style = PhotoboothType.meta9, color = if (selected) PhotoboothColors.Paper.copy(alpha = 0.6f) else PhotoboothColors.TextMuted)
                }
            }
        }
    }
}

@Composable
private fun FrameColorRow(state: StripPreviewUiState, onFrameColorChange: (FrameColorPreset) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = PhotoboothSpacing.lg, vertical = PhotoboothSpacing.md)) {
        Text("FRAME COLOUR", style = PhotoboothType.meta10, color = PhotoboothColors.AccentPressed)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(PhotoboothSpacing.xs))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.sm),
        ) {
            FrameColorPreset.entries.forEach { preset ->
                val selected = state.frameColor == preset
                Chip(
                    selected = selected,
                    selectedBackground = PhotoboothColors.AccentTintSoft,
                    selectedBorder = PhotoboothColors.Accent,
                    onClick = { onFrameColorChange(preset) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.xs)) {
                        Box(modifier = Modifier.size(14.dp).background(preset.background).border(1.dp, PhotoboothColors.TextPrimary.copy(alpha = 0.2f)))
                        Text(preset.name.uppercase(), style = PhotoboothType.heading12, color = PhotoboothColors.TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(
    selected: Boolean,
    selectedBackground: androidx.compose.ui.graphics.Color = PhotoboothColors.Accent,
    selectedBorder: androidx.compose.ui.graphics.Color = PhotoboothColors.Accent,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .height(44.dp)
            .border(1.dp, if (selected) selectedBorder else PhotoboothColors.HairlineOnLight)
            .background(if (selected) selectedBackground else PhotoboothColors.Paper)
            .clickable(onClick = onClick)
            .padding(horizontal = PhotoboothSpacing.mdLarge, vertical = PhotoboothSpacing.md),
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

@Composable
private fun ActionBar(state: StripPreviewUiState, onReshoot: () -> Unit, onSavePng: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(PhotoboothColors.Paper)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PhotoboothColors.HairlineOnLight))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PhotoboothSpacing.lg, vertical = PhotoboothSpacing.mdLarge),
        ) {
        ActionBarContent(state, onReshoot, onSavePng)
        }
    }
}

@Composable
private fun ActionBarContent(state: StripPreviewUiState, onReshoot: () -> Unit, onSavePng: () -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.mdLarge)) {
            OutlinedButton(
                onClick = onReshoot,
                modifier = Modifier.size(width = 56.dp, height = 56.dp),
                border = BorderStroke(1.dp, PhotoboothColors.HairlineOnLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PhotoboothColors.TextPrimary),
            ) { Text("↺", style = PhotoboothType.heading18) }

            Button(
                onClick = onSavePng,
                enabled = state.composedImage != null && !state.isSaving,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhotoboothColors.Accent,
                    contentColor = PhotoboothColors.Paper,
                    disabledContainerColor = PhotoboothColors.Accent.copy(alpha = 0.45f),
                ),
            ) {
                // design/handoff/README.md line 150: 17px - closest shared token is
                // heading18 (18px); not pixel-exact, but no dedicated 17px token exists yet.
                Text(if (state.isSaving) "SAVING…" else "SAVE PNG", style = PhotoboothType.heading18)
            }
        }
        Text(
            state.saveError ?: state.saveCaption,
            style = PhotoboothType.meta10,
            color = PhotoboothColors.TextMuted,
            modifier = Modifier.fillMaxWidth().padding(top = PhotoboothSpacing.sm),
            textAlign = TextAlign.Center,
        )
    }
}
