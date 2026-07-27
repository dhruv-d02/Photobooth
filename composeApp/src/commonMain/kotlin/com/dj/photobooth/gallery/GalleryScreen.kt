package com.dj.photobooth.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dj.photobooth.compose.rememberPathImageLoader
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothSpacing
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.CornerTicks

/**
 * The Gallery ("Past strips") screen (design/handoff/README.md §4). Pure View in the MVVM
 * sense, mirroring CaptureScreen/StripPreviewScreen - all state from [viewModel]'s
 * [GalleryUiState], every tap calls a ViewModel method.
 *
 * Both card actions are terminal (save a copy, hand off to the system gallery app), so unlike
 * the other screens this one needs no navigation callbacks at all.
 */
@Composable
fun GalleryScreen(viewModel: GalleryViewModel) {
    val state by viewModel.uiState.collectAsState()

    // statusBarsPadding: the NavHost deliberately applies no blanket status-bar inset (it
    // would break Capture's full-bleed dark screen), so each light screen adds its own.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PhotoboothColors.Ground)
            .statusBarsPadding(),
    ) {
        GalleryHeader()

        state.message?.let { message ->
            Text(
                text = message.uppercase(),
                style = PhotoboothType.meta10,
                color = PhotoboothColors.AccentPressed,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PhotoboothSpacing.md, start = PhotoboothSpacing.lg, end = PhotoboothSpacing.lg),
            )
        }

        if (state.isEmpty) {
            EmptyState(modifier = Modifier.padding(PhotoboothSpacing.lg))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(PhotoboothSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.mdLarge),
                verticalArrangement = Arrangement.spacedBy(PhotoboothSpacing.mdLarge),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    GalleryCard(
                        entry = entry,
                        onOpen = { viewModel.onOpen(entry) },
                        onSave = { viewModel.onSaveCopy(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = PhotoboothSpacing.lgLarge,
                start = PhotoboothSpacing.lg,
                end = PhotoboothSpacing.lg,
                bottom = PhotoboothSpacing.mdLarge,
            ),
    ) {
        Text("ARCHIVE · ON THIS DEVICE", style = PhotoboothType.meta10, color = PhotoboothColors.AccentPressed)
        Text("PAST STRIPS", style = PhotoboothType.heading36, color = PhotoboothColors.TextPrimary)
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PhotoboothColors.HairlineOnLight))
}

// design/handoff/README.md §4: "1px dashed #1d1f2033 box, 34px/20.4px padding, 45deg 6px/6px
// hatch background (#e9e9ea/#f2f2f3)".
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    val dashed = remember {
        // 6px on / 6px off, matching the hatch's own 6px period so the two read as one texture.
        Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PhotoboothColors.Ground)
            .drawBehind {
                drawEmptyStateHatch()
                drawRect(color = PhotoboothColors.HairlineDashed, style = dashed)
            }
            .padding(vertical = PhotoboothSpacing.xxl, horizontal = PhotoboothSpacing.lgLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("NO STRIPS ON FILE", style = PhotoboothType.meta11, color = PhotoboothColors.TextMuted)
        Text(
            "Strips you save are archived here.",
            style = PhotoboothType.body14,
            color = PhotoboothColors.TextBody,
            modifier = Modifier.padding(top = PhotoboothSpacing.sm),
        )
    }
}

// "45° 6px/6px hatch (#e9e9ea/#f2f2f3)" - the Ground fill is drawn by the caller's
// background(), so this only needs to lay the #e9e9ea stripes over it.
private fun DrawScope.drawEmptyStateHatch() {
    val strokeWidthPx = 6.dp.toPx()
    val periodPx = 12.dp.toPx()
    var offset = -size.height
    while (offset < size.width + size.height) {
        drawLine(
            color = PhotoboothColors.SurfaceWash,
            start = Offset(offset, size.height),
            end = Offset(offset + size.height, 0f),
            strokeWidth = strokeWidthPx,
        )
        offset += periodPx
    }
}

@Composable
private fun GalleryCard(entry: HistoryEntry, onOpen: () -> Unit, onSave: () -> Unit) {
    val loader = rememberPathImageLoader()
    // Two-column grid on a phone, so a card is roughly half the screen width. Passing that as
    // the decode hint keeps each thumbnail near its displayed size instead of decoding the
    // full 2x-scale export - see PathImageLoader.load's doc comment.
    val density = LocalDensity.current
    val targetWidthPx = remember(density) { with(density) { 200.dp.roundToPx() } }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, entry.finalImagePath, targetWidthPx) {
        value = loader.load(entry.finalImagePath, targetWidthPx)
    }

    CornerTicks(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PhotoboothColors.HairlineOnLight)
            .clickable(onClick = onOpen)
            .padding(PhotoboothSpacing.md),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).background(PhotoboothColors.SurfaceWash)) {
                bitmap?.let {
                    Image(
                        painter = BitmapPainter(it),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = PhotoboothSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${entry.stamp} · ${entry.filmTreatmentId}",
                    style = PhotoboothType.meta9,
                    color = PhotoboothColors.TextMuted,
                )
                Text(
                    "SAVE",
                    style = PhotoboothType.heading10,
                    color = PhotoboothColors.AccentPressed,
                    // design/handoff/README.md §4 specs this link at min-height 32px - it's a
                    // small tap target next to a card-sized one, so it needs the explicit
                    // minimum rather than inheriting the text's own line height.
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .clickable(onClick = onSave)
                        .padding(top = PhotoboothSpacing.xs),
                )
            }
        }
    }
}
