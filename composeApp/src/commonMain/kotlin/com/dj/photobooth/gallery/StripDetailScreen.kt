package com.dj.photobooth.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dj.photobooth.compose.rememberPathImageLoader
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothSpacing
import com.dj.photobooth.theme.PhotoboothType

/**
 * The Strip Detail screen - a lightweight in-app viewer opened by tapping a card in the
 * Gallery grid, replacing the previous hand-off to the platform's own photo viewer (see the
 * retired MediaViewer interface). No design handoff spec covers this screen - Gallery cards
 * were originally terminal - so it borrows the visual language CaptureScreen/its ProofOverlay
 * already established for showing a photo full-bleed (dark stage, scrim headers, monospace
 * captions) rather than the light Gallery grid's look, matching how system photo viewers use a
 * dark backdrop regardless of the image's own colours.
 *
 * Pure View in the MVVM sense - all state comes from [state], every tap calls back out.
 */
@Composable
fun StripDetailScreen(
    state: StripDetailUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteCancelled: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(PhotoboothColors.DarkSurface)) {
        val entry = state.entry
        when {
            entry != null -> {
                DetailImage(entry = entry, modifier = Modifier.fillMaxSize())
                DetailTopBar(
                    entry = entry,
                    onBack = onBack,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                DetailActionBar(
                    isDeleting = state.isDeleting,
                    onShare = onShare,
                    onDelete = onDeleteRequested,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            state.notFound -> NotFoundState(onBack = onBack, modifier = Modifier.align(Alignment.Center))
            else -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PhotoboothColors.OnDarkAccent,
            )
        }
    }

    if (state.confirmingDelete) {
        DeleteConfirmDialog(
            isDeleting = state.isDeleting,
            onConfirm = onDeleteConfirmed,
            onDismiss = onDeleteCancelled,
        )
    }
}

@Composable
private fun DetailImage(entry: HistoryEntry, modifier: Modifier = Modifier) {
    val loader = rememberPathImageLoader()
    BoxWithConstraints(modifier = modifier) {
        // Full container width, not the Gallery grid's fixed 200dp hint - this is the one
        // screen where the user asked to see the strip closely, so it's worth decoding near
        // the actual display size rather than a thumbnail (PathImageLoader still downsamples
        // to this hint, so it stays well short of the raw 2x-scale export's full resolution).
        val density = LocalDensity.current
        val maxWidthPx = remember(density, maxWidth) { with(density) { maxWidth.roundToPx() } }
        val bitmap by produceState<ImageBitmap?>(initialValue = null, entry.finalImagePath, maxWidthPx) {
            value = loader.load(entry.finalImagePath, maxWidthPx)
        }
        if (bitmap != null) {
            Image(
                painter = BitmapPainter(bitmap!!),
                contentDescription = null,
                // Fit, not the grid's Crop: this screen exists so the user can see the whole
                // strip, not a cell of it - letterboxing against the dark backdrop is the
                // correct trade here, not cropping content away.
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = PhotoboothColors.OnDarkAccent,
            )
        }
    }
}

@Composable
private fun DetailTopBar(entry: HistoryEntry, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Same scrim treatment as ProofOverlay's header (design/handoff/README.md line
            // 113's linear-gradient), so the back button stays legible over a bright frame
            // colour without needing an opaque bar that would cover more of the image.
            .background(
                Brush.verticalGradient(listOf(PhotoboothColors.ProofScrimApprox, PhotoboothColors.DarkSurface.copy(alpha = 0f))),
            )
            .statusBarsPadding()
            .padding(horizontal = PhotoboothSpacing.lg, vertical = PhotoboothSpacing.mdLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Mirrors CaptureTopBar's "← EXIT" button exactly (same size/border/weight override) -
        // this screen and Capture are the only two full-bleed dark viewers in the app.
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.size(width = 88.dp, height = 44.dp),
            border = BorderStroke(1.dp, PhotoboothColors.HairlineOnDark),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PhotoboothColors.Paper),
        ) {
            Text("← BACK", style = PhotoboothType.heading12.copy(fontWeight = FontWeight.Medium))
        }
        Text(
            "${entry.stamp} · ${entry.filmTreatmentId}",
            style = PhotoboothType.meta10,
            color = PhotoboothColors.OnDarkAccent,
        )
    }
}

@Composable
private fun DetailActionBar(
    isDeleting: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(PhotoboothColors.DarkSurface.copy(alpha = 0f), PhotoboothColors.ProofScrimApprox)),
            )
            .navigationBarsPadding()
            .padding(horizontal = PhotoboothSpacing.lg, vertical = PhotoboothSpacing.mdLarge),
        horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.mdLarge),
    ) {
        // Ghost, not a red/danger button - the design's palette has no destructive colour at
        // all (one steel-blue accent, full stop), so DELETE reads as intentional through the
        // confirmation dialog's wording, the same way SHOOT AGAIN discards a frame without a
        // dedicated warning colour anywhere else in the app.
        OutlinedButton(
            onClick = onDelete,
            enabled = !isDeleting,
            modifier = Modifier.weight(1f).height(56.dp),
            border = BorderStroke(1.dp, PhotoboothColors.GhostBorderOnDark),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PhotoboothColors.Paper,
                disabledContentColor = PhotoboothColors.DisabledOnDark,
            ),
        ) { Text(if (isDeleting) "DELETING…" else "DELETE", style = PhotoboothType.heading18) }

        Button(
            onClick = onShare,
            modifier = Modifier.weight(1.4f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PhotoboothColors.OnDarkAccent,
                contentColor = PhotoboothColors.DarkSurface,
            ),
        ) { Text("SHARE", style = PhotoboothType.heading18) }
    }
}

@Composable
private fun NotFoundState(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("STRIP NOT FOUND", style = PhotoboothType.meta11, color = PhotoboothColors.OnDarkSecondaryText)
        Text(
            "It may have already been deleted.",
            style = PhotoboothType.body14,
            color = PhotoboothColors.Paper,
            modifier = Modifier.padding(top = PhotoboothSpacing.sm, bottom = PhotoboothSpacing.lgLarge),
        )
        OutlinedButton(
            onClick = onBack,
            border = BorderStroke(1.dp, PhotoboothColors.HairlineOnDark),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PhotoboothColors.Paper),
        ) { Text("← BACK", style = PhotoboothType.heading12.copy(fontWeight = FontWeight.Medium)) }
    }
}

@Composable
private fun DeleteConfirmDialog(isDeleting: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        // Only dismissable by tapping outside/back while not mid-delete - once a delete is in
        // flight there's no state to usefully back out of.
        onDismissRequest = { if (!isDeleting) onDismiss() },
        containerColor = PhotoboothColors.Paper,
        title = { Text("DELETE THIS STRIP?", style = PhotoboothType.heading18, color = PhotoboothColors.TextPrimary) },
        text = {
            Text(
                "This removes it from your gallery and device storage. This can't be undone.",
                style = PhotoboothType.body14,
                color = PhotoboothColors.TextBody,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(
                    if (isDeleting) "DELETING…" else "DELETE",
                    style = PhotoboothType.heading12,
                    color = PhotoboothColors.AccentPressed,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("CANCEL", style = PhotoboothType.heading12, color = PhotoboothColors.TextMuted)
            }
        },
    )
}
