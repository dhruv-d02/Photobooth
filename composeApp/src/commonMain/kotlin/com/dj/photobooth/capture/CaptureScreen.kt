package com.dj.photobooth.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dj.photobooth.camera.CameraController
import com.dj.photobooth.camera.CameraPreviewSurface
import com.dj.photobooth.theme.PhotoboothColors
import com.dj.photobooth.theme.PhotoboothSpacing
import com.dj.photobooth.theme.PhotoboothType
import com.dj.photobooth.ui.CornerTicks

/**
 * The Capture screen (design/handoff/README.md § 2): full-bleed dark steel screen, no
 * bottom tab bar (a session is modal). Pure View in the MVVM sense - all state comes from
 * [viewModel]'s [CaptureUiState]; every tap just calls a ViewModel method.
 */
@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel,
    cameraController: CameraController,
    onExitToLanding: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PhotoboothColors.DarkSurface),
    ) {
        CaptureTopBar(state = state, onExit = { viewModel.onExit(); onExitToLanding() })

        Box(modifier = Modifier.weight(1f)) {
            Viewfinder(state = state, cameraController = cameraController)
        }

        CaptureBottomBar(
            state = state,
            onShutter = viewModel::onShutter,
            onKeep = viewModel::onKeep,
            onShootAgain = viewModel::onShootAgain,
        )
    }
}

@Composable
private fun CaptureTopBar(state: CaptureUiState, onExit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PhotoboothSpacing.lg, vertical = PhotoboothSpacing.mdLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.size(width = 88.dp, height = 44.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PhotoboothColors.HairlineOnDark),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PhotoboothColors.Paper),
        ) {
            // design/handoff/README.md line 98-99: weight 500 (Medium), not the shared
            // heading12 token's 600 (SemiBold) - this button is the one 12px-heading
            // exception, so it overrides via .copy() rather than changing heading12 itself.
            Text("← EXIT", style = PhotoboothType.heading12.copy(fontWeight = FontWeight.Medium))
        }

        val exposureStatus = state.review?.let { "RETAKE · FRAME ${state.frameLabel(it.index)}" }
            ?: "EXPOSURE ${state.frameLabel(state.queue.firstOrNull() ?: (state.acceptedCount).coerceAtMost(state.shotCount - 1))} / ${state.shotCountLabel()}"
        Text(exposureStatus, style = PhotoboothType.meta10, color = PhotoboothColors.OnDarkAccent)

        val recStatus = if (state.shooting) "● REC" else "○ IDLE"
        Text(recStatus, style = PhotoboothType.meta10, color = PhotoboothColors.OnDarkSecondaryText)
    }
}

@Composable
private fun Viewfinder(state: CaptureUiState, cameraController: CameraController) {
    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreviewSurface(
            controller = cameraController,
            modifier = Modifier.fillMaxSize(),
        )

        // Inset frame + corner ticks, per § 2 Viewfinder.
        CornerTicks(
            modifier = Modifier
                .padding(PhotoboothSpacing.mdLarge)
                .fillMaxSize()
                .border(1.dp, PhotoboothColors.HairlineOnDarkSubtle),
            tickColor = PhotoboothColors.OnDarkAccent,
        ) {}

        AnimatedVisibility(
            visible = state.countdown.isNotEmpty(),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = state.countdown,
                style = PhotoboothType.countdownDisplay,
                color = PhotoboothColors.Paper,
            )
        }

        AnimatedVisibility(
            visible = state.flash,
            enter = fadeIn(tween(0)),
            exit = fadeOut(tween(120)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(PhotoboothColors.FlashOverlay))
        }

        Text(
            text = when (state.cameraState) {
                CameraState.Live -> "FRONT CAMERA LIVE"
                CameraState.Denied -> "PLACEHOLDER MODE"
                CameraState.RequestingPermission -> "CONNECTING…"
                CameraState.Idle -> "CONNECTING…"
            },
            style = PhotoboothType.meta10,
            color = PhotoboothColors.CaptionOnDark,
            modifier = Modifier.align(Alignment.BottomStart).padding(PhotoboothSpacing.lg),
        )
        Text(
            text = if (state.cameraState == CameraState.Live) "MIRRORED · 4:3" else "NO SIGNAL",
            style = PhotoboothType.meta10,
            color = PhotoboothColors.CaptionOnDark,
            modifier = Modifier.align(Alignment.BottomEnd).padding(PhotoboothSpacing.lg),
        )

        state.review?.let { review ->
            ProofOverlay(review = review, state = state)
        }
    }
}

@Composable
private fun ProofOverlay(review: ReviewState, state: CaptureUiState) {
    Box(modifier = Modifier.fillMaxSize().background(PhotoboothColors.DarkSurface)) {
        // TODO(phase-2): draw the actual mirrored frame bitmap here once the shared
        // decode/compositing path exists; placeholder frames render as text-only for now
        // (see CaptureFrame.isPlaceholder), and real frames just show the proof chip on a
        // solid ground until then.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PhotoboothColors.ProofScrimApprox)
                .padding(horizontal = PhotoboothSpacing.lgLarge, vertical = PhotoboothSpacing.mdLarge),
            horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(PhotoboothColors.OnDarkAccent)
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            ) {
                Text(
                    "PROOF ${state.frameLabel(review.index)}",
                    style = PhotoboothType.metaChip11,
                    color = PhotoboothColors.DarkSurface,
                )
            }
            Text(
                "FRAME ${state.frameLabel(review.index)} OF ${state.shotCountLabel()} · YOUR CALL",
                style = PhotoboothType.meta10,
                color = PhotoboothColors.Paper,
            )
        }
    }
}

@Composable
private fun CaptureBottomBar(
    state: CaptureUiState,
    onShutter: () -> Unit,
    onKeep: () -> Unit,
    onShootAgain: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PhotoboothSpacing.lg, vertical = PhotoboothSpacing.mdLarge),
        verticalArrangement = Arrangement.spacedBy(PhotoboothSpacing.mdLarge),
    ) {
        ThumbnailRow(state = state)

        if (state.review != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.mdLarge)) {
                OutlinedButton(
                    onClick = onShootAgain,
                    modifier = Modifier.weight(1f).height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PhotoboothColors.GhostBorderOnDark),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PhotoboothColors.Paper),
                ) { Text("SHOOT AGAIN", style = PhotoboothType.heading18) }

                val isLastFrame = state.queue.size <= 1
                Button(
                    onClick = onKeep,
                    modifier = Modifier.weight(1.4f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PhotoboothColors.OnDarkAccent,
                        contentColor = PhotoboothColors.DarkSurface,
                    ),
                ) { Text(if (isLastFrame) "KEEP" else "KEEP · NEXT", style = PhotoboothType.heading18) }
            }
            // design/handoff/README.md lines 125-126: "Below them, the log line as centred
            // monospace 10px #9ebbd8" - missing entirely before this fix, so log messages
            // like "Frame 02 discarded · shooting again." never reached the user in review.
            Text(
                state.log,
                style = PhotoboothType.meta10,
                color = PhotoboothColors.OnDarkSecondaryText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    state.log,
                    style = PhotoboothType.body11,
                    color = PhotoboothColors.OnDarkSecondaryText,
                    modifier = Modifier.weight(1f),
                )
                val shutterLabel = when {
                    state.shooting -> "EXPOSING…"
                    state.queue.size == 1 && state.acceptedCount > 0 -> "SHOOT ${state.frameLabel(state.queue.first())}"
                    else -> "SHOOT ${state.shotCount}"
                }
                Button(
                    onClick = onShutter,
                    enabled = !state.shooting,
                    modifier = Modifier.size(width = 150.dp, height = 56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PhotoboothColors.Paper,
                        contentColor = PhotoboothColors.DarkSurface,
                        disabledContainerColor = PhotoboothColors.DisabledOnDark,
                    ),
                ) { Text(shutterLabel, style = PhotoboothType.heading18) }
            }
        }
    }
}

@Composable
private fun ThumbnailRow(state: CaptureUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(PhotoboothSpacing.sm), modifier = Modifier.fillMaxWidth()) {
        state.frames.forEachIndexed { index, frame ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(4f / 3f)
                    .background(if (frame != null) Color.Transparent else PhotoboothColors.ThumbnailEmptyFill)
                    .border(1.dp, if (frame != null) PhotoboothColors.OnDarkAccent else PhotoboothColors.HairlineOnDark),
                contentAlignment = Alignment.BottomStart,
            ) {
                // TODO(phase-2): render frame.jpegBytes as the mirrored thumbnail image once
                // the shared decode path exists.
                if (frame != null) {
                    Text(
                        state.frameLabel(index),
                        style = PhotoboothType.meta8,
                        color = PhotoboothColors.Paper,
                        modifier = Modifier.padding(2.dp),
                    )
                }
            }
        }
    }
}
