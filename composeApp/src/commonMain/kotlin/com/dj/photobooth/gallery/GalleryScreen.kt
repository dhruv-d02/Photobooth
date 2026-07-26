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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dj.photobooth.compose.rememberPathImageLoader
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothSpacing
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.CornerTicks

/**
 * The Gallery ("Past strips") screen (design/handoff/README.md §4). Pure View in the MVVM
 * sense, mirroring CaptureScreen/StripPreviewScreen - all state from [viewModel]'s
 * [GalleryUiState], every tap calls a ViewModel method or an [onOpenEntry] navigation seam
 * (see StripPreviewViewModel's doc comment for why navigation itself lives outside this branch).
 */
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenEntry: (HistoryEntry) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(PhotoboothColors.Ground)) {
        GalleryHeader()

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
                        onOpen = { onOpenEntry(entry) },
                        onShare = { viewModel.onShare(entry) },
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

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PhotoboothColors.HairlineOnLight)
            .background(PhotoboothColors.SurfaceWash)
            .padding(PhotoboothSpacing.xxl),
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

@Composable
private fun GalleryCard(entry: HistoryEntry, onOpen: () -> Unit, onShare: () -> Unit) {
    val loader = rememberPathImageLoader()
    val bitmap by produceState<ImageBitmap?>(initialValue = null, entry.finalImagePath) {
        value = loader.load(entry.finalImagePath)
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
                    modifier = Modifier.clickable(onClick = onShare),
                )
            }
        }
    }
}
